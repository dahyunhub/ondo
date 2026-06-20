package com.ondo.journal;

import com.ondo.ai.AiClient;
import com.ondo.ai.deid.Deidentifier;
import com.ondo.ai.deid.RestorationContext;
import com.ondo.ai.dto.AiRequest;
import com.ondo.ai.dto.JournalAnalysisResult;
import com.ondo.ai.dto.MemoPromptInput;
import com.ondo.ai.prompt.JournalAnalysisSchema;
import com.ondo.ai.prompt.JournalResultParser;
import com.ondo.ai.prompt.OutputValidator;
import com.ondo.ai.prompt.PromptTemplateLoader;
import com.ondo.child.ChildRepository;
import com.ondo.child.domain.Child;
import com.ondo.common.concurrency.AnalysisGuard;
import com.ondo.common.exception.AiAnalysisException;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.classroom.ClassroomRepository;
import com.ondo.journal.JournalPersistService.PersistResult;
import com.ondo.journal.domain.DailyJournal;
import com.ondo.journal.domain.JournalStatus;
import com.ondo.journal.dto.JournalAnalyzeRequest;
import com.ondo.journal.dto.JournalDetailResponse;
import com.ondo.journal.dto.JournalResponse;
import com.ondo.journal.dto.JournalUpdateRequest;
import com.ondo.memo.MemoRepository;
import com.ondo.memo.domain.CurriculumArea;
import com.ondo.memo.domain.Memo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 하루치 일지 초안 생성 오케스트레이션(Story 3.5). **no-TX** — AiClient 호출 동안 DB 커넥션을 점유하지 않는다.
 * AnalysisGuard.runExclusively 로 사용자당 동시 1건을 보장하고, 저장은 주입받은 JournalPersistService(REQUIRES_NEW)에 위임한다.
 * 부품 조립: Deidentifier(3.2) · PromptTemplateLoader/OutputValidator(3.3) · AnalysisGuard(3.4) · AiClient(3.1).
 * 정본: ai-integration-spec §7, api-spec [11].
 */
@Service
public class JournalService {

    private static final int JOURNAL_MAX_TOKENS = 4096;

    private final ClassroomRepository classroomRepository;
    private final DailyJournalRepository dailyJournalRepository;
    private final MemoRepository memoRepository;
    private final ChildRepository childRepository;
    private final Deidentifier deidentifier;
    private final PromptTemplateLoader promptTemplateLoader;
    private final JournalResultParser journalResultParser;
    private final OutputValidator outputValidator;
    private final AiClient aiClient;
    private final AnalysisGuard analysisGuard;
    private final JournalContentSerializer contentSerializer;
    private final JournalPersistService journalPersistService;

    public JournalService(ClassroomRepository classroomRepository,
                          DailyJournalRepository dailyJournalRepository,
                          MemoRepository memoRepository,
                          ChildRepository childRepository,
                          Deidentifier deidentifier,
                          PromptTemplateLoader promptTemplateLoader,
                          JournalResultParser journalResultParser,
                          OutputValidator outputValidator,
                          AiClient aiClient,
                          AnalysisGuard analysisGuard,
                          JournalContentSerializer contentSerializer,
                          JournalPersistService journalPersistService) {
        this.classroomRepository = classroomRepository;
        this.dailyJournalRepository = dailyJournalRepository;
        this.memoRepository = memoRepository;
        this.childRepository = childRepository;
        this.deidentifier = deidentifier;
        this.promptTemplateLoader = promptTemplateLoader;
        this.journalResultParser = journalResultParser;
        this.outputValidator = outputValidator;
        this.aiClient = aiClient;
        this.analysisGuard = analysisGuard;
        this.contentSerializer = contentSerializer;
        this.journalPersistService = journalPersistService;
    }

    /** 사용자당 동시 1건 가드로 감싼 진입점. */
    public JournalResponse analyze(Long teacherId, JournalAnalyzeRequest request) {
        return analysisGuard.runExclusively(teacherId, () -> doAnalyze(teacherId, request));
    }

    private JournalResponse doAnalyze(Long teacherId, JournalAnalyzeRequest request) {
        Long classroomId = request.classroomId();
        LocalDate date = request.date();

        // 소유권 검증(타 교사 반은 비노출)
        classroomRepository.findByIdAndTeacherId(classroomId, teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASSROOM_NOT_FOUND));

        // 일지 1건 유일성
        if (dailyJournalRepository.existsByTeacherIdAndClassroomIdAndJournalDate(teacherId, classroomId, date)) {
            throw new BusinessException(ErrorCode.JOURNAL_ALREADY_EXISTS);
        }

        // 그 날짜 반 전체 메모(부분 선택 없음)
        List<Memo> memos = memoRepository.findClassroomBundle(
                classroomId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        if (memos.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "그 날짜에 작성된 메모가 없어요.");
        }

        // 비식별화 컨텍스트(반 전체 명단) + 메모 인덱스 입력 구성
        RestorationContext ctx = deidentifier.newContext(rosterNames(classroomId));
        List<MemoPromptInput> inputs = new ArrayList<>();
        Map<Integer, Long> indexToMemoId = new LinkedHashMap<>();
        int index = 1;
        for (Memo memo : memos) {
            inputs.add(new MemoPromptInput(index,
                    deidentifier.deidentify(memo.getContent(), ctx),
                    deidentifier.deidentify(memo.getPlayActivity(), ctx),
                    deidentifier.deidentify(memo.getInteraction(), ctx),
                    deidentifier.deidentify(memo.getAttitude(), ctx)));
            indexToMemoId.put(index, memo.getId());
            index++;
        }

        // 프롬프트 → AiClient(TX 밖) → 복원 → 파싱 → 검증(실패 시 1회 재요청)
        AiRequest aiRequest = new AiRequest(
                promptTemplateLoader.journalSystemPrompt(),
                promptTemplateLoader.renderMemos(inputs),
                JournalAnalysisSchema.schema(),
                JOURNAL_MAX_TOKENS);
        JournalAnalysisResult result = analyzeWithRetry(aiRequest, ctx, indexToMemoId.keySet());

        // 메모 영역 매핑(FR-2) + 평탄화 content
        Map<Long, CurriculumArea> memoIdToArea = new HashMap<>();
        for (JournalAnalysisResult.MemoArea ma : result.memoClassifications()) {
            Long memoId = indexToMemoId.get(ma.index());
            if (memoId != null) {
                memoIdToArea.put(memoId, ma.area());
            }
        }
        Map<String, String> content = contentSerializer.toFlatContent(result);
        List<Long> memoIds = new ArrayList<>(indexToMemoId.values());

        // 저장(REQUIRES_NEW)
        PersistResult persisted = journalPersistService.save(
                teacherId, classroomId, date, contentSerializer.toJson(content), memoIds, memoIdToArea);

        return new JournalResponse(persisted.journalId(), classroomId, date, JournalStatus.DRAFT.name(),
                content, persisted.analyzedAt(), persisted.linkedMemoIds());
    }

    private List<String> rosterNames(Long classroomId) {
        return childRepository.findByClassroomIdOrderByNameAscIdAsc(classroomId)
                .stream().map(Child::getName).toList();
    }

    /** AI_OUTPUT_INVALID(파싱/검증 실패)면 1회 재요청. 연결·타임아웃 실패는 재요청하지 않고 전파. */
    private JournalAnalysisResult analyzeWithRetry(AiRequest request, RestorationContext ctx, Set<Integer> expectedIndices) {
        try {
            return attempt(request, ctx, expectedIndices);
        } catch (AiAnalysisException e) {
            if (e.getErrorCode() == ErrorCode.AI_OUTPUT_INVALID) {
                return attempt(request, ctx, expectedIndices); // 1회 재요청 — 또 실패하면 그대로 전파
            }
            throw e;
        }
    }

    private JournalAnalysisResult attempt(AiRequest request, RestorationContext ctx, Set<Integer> expectedIndices) {
        String raw = aiClient.complete(request);                 // TX 밖 외부 호출
        JournalAnalysisResult result = journalResultParser.parse(ctx.restore(raw)); // 복원 후 파싱
        outputValidator.validate(result, expectedIndices);
        return result;
    }

    /** [13] 단건 조회 — 본인 일지만(없으면 JOURNAL_NOT_FOUND). AI 미사용. */
    @Transactional(readOnly = true)
    public JournalDetailResponse getJournal(Long teacherId, Long journalId) {
        return toDetail(findOwned(teacherId, journalId));
    }

    /** [14] 수정·확정(FR-5) — content·status 저장(AI 미사용). */
    @Transactional
    public JournalDetailResponse updateJournal(Long teacherId, Long journalId, JournalUpdateRequest request) {
        contentSerializer.validateFlatContent(request.content()); // 저장형 계약(요약+5영역 non-blank) 강제
        DailyJournal journal = findOwned(teacherId, journalId);
        journal.update(contentSerializer.toJson(request.content()), request.status()); // dirty checking
        return toDetail(journal);
    }

    private DailyJournal findOwned(Long teacherId, Long journalId) {
        return dailyJournalRepository.findByIdAndTeacherId(journalId, teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOURNAL_NOT_FOUND));
    }

    private JournalDetailResponse toDetail(DailyJournal journal) {
        return new JournalDetailResponse(journal.getId(), journal.getClassroomId(), journal.getJournalDate(),
                journal.getStatus().name(), contentSerializer.toMap(journal.getContent()), journal.getAnalyzedAt());
    }
}
