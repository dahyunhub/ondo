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
import com.ondo.common.time.AppTime;
import com.ondo.common.exception.AiAnalysisException;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.classroom.ClassroomRepository;
import com.ondo.journal.JournalPersistService.PersistResult;
import com.ondo.journal.domain.DailyJournal;
import com.ondo.journal.domain.JournalStatus;
import com.ondo.journal.dto.JournalAnalyzeRequest;
import com.ondo.journal.dto.JournalDetailResponse;
import com.ondo.journal.dto.JournalListResponse;
import com.ondo.journal.dto.JournalResponse;
import com.ondo.journal.dto.JournalUpdateRequest;
import com.ondo.memo.MemoRepository;
import com.ondo.memo.domain.CurriculumArea;
import com.ondo.memo.domain.Memo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

        AnalysisOutcome outcome = runAnalysisPipeline(classroomId, date);
        PersistResult persisted = journalPersistService.save(
                teacherId, classroomId, date, outcome.contentJson(), outcome.memoIds(), outcome.memoIdToArea(),
                outcome.analyzedAt());

        return new JournalResponse(persisted.journalId(), classroomId, date, JournalStatus.DRAFT.name(),
                outcome.content(), persisted.analyzedAt(), persisted.linkedMemoIds());
    }

    /**
     * analyze(생성)·reanalyze(덮어쓰기) 공통 AI 파이프라인:
     * 묶음 로드(0건 → VALIDATION_FAILED) → 비식별화 → 프롬프트 → AiClient(TX 밖) → 복원·검증·1회 재요청 →
     * (평탄화 content, contentJson, memoIds, index→영역 매핑).
     */
    private AnalysisOutcome runAnalysisPipeline(Long classroomId, LocalDate date) {
        // 묶음 스냅샷 기준 시각 — AI 호출 '전'에 찍는다. 이래야 분석 도중 추가된 메모가
        // (createdAt > analyzedAt) 판정에 잡혀 재분석 안내에서 누락되지 않는다(false negative 방지).
        LocalDateTime analyzedAt = LocalDateTime.now(ZoneOffset.UTC);
        List<Memo> memos = memoRepository.findClassroomBundle(
                classroomId, AppTime.startOfDayUtc(date), AppTime.startOfNextDayUtc(date));
        if (memos.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "그 날짜에 작성된 메모가 없어요.");
        }

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

        AiRequest aiRequest = new AiRequest(
                promptTemplateLoader.journalSystemPrompt(),
                promptTemplateLoader.renderMemos(inputs),
                JournalAnalysisSchema.schema(),
                JOURNAL_MAX_TOKENS);
        JournalAnalysisResult result = analyzeWithRetry(aiRequest, ctx, indexToMemoId.keySet());

        Map<Long, CurriculumArea> memoIdToArea = new HashMap<>();
        for (JournalAnalysisResult.MemoArea ma : result.memoClassifications()) {
            Long memoId = indexToMemoId.get(ma.index());
            if (memoId != null) {
                memoIdToArea.put(memoId, ma.area());
            }
        }
        Map<String, String> content = contentSerializer.toFlatContent(result);
        return new AnalysisOutcome(content, contentSerializer.toJson(content),
                new ArrayList<>(indexToMemoId.values()), memoIdToArea, analyzedAt);
    }

    private record AnalysisOutcome(Map<String, String> content, String contentJson,
                                   List<Long> memoIds, Map<Long, CurriculumArea> memoIdToArea,
                                   LocalDateTime analyzedAt) {
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

    /** [12] 반·날짜 일지 조회 + 재분석 안내(FR-6). 없으면 JOURNAL_NOT_FOUND. AI 미사용. */
    @Transactional(readOnly = true)
    public JournalListResponse getByClassroomAndDate(Long teacherId, Long classroomId, LocalDate date) {
        DailyJournal journal = dailyJournalRepository
                .findByTeacherIdAndClassroomIdAndJournalDate(teacherId, classroomId, date)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOURNAL_NOT_FOUND));

        // 분석 이후 추가된 메모 식별(memo.createdAt > journal.analyzedAt)
        List<Long> newMemoIds = memoRepository.findClassroomBundle(
                        classroomId, AppTime.startOfDayUtc(date), AppTime.startOfNextDayUtc(date))
                .stream()
                .filter(memo -> journal.getAnalyzedAt() != null && memo.getCreatedAt().isAfter(journal.getAnalyzedAt()))
                .map(Memo::getId)
                .toList();

        return new JournalListResponse(journal.getId(), journal.getClassroomId(), journal.getJournalDate(),
                journal.getStatus().name(), contentSerializer.toMap(journal.getContent()), journal.getAnalyzedAt(),
                !newMemoIds.isEmpty(), newMemoIds);
    }

    /** [15] 재분석·덮어쓰기(FR-6) — 같은 행 UPDATE + 링크 재구성. 사용자당 동시 1건 가드. */
    public JournalResponse reanalyze(Long teacherId, Long journalId) {
        return analysisGuard.runExclusively(teacherId, () -> doReanalyze(teacherId, journalId));
    }

    private JournalResponse doReanalyze(Long teacherId, Long journalId) {
        DailyJournal journal = findOwned(teacherId, journalId);
        AnalysisOutcome outcome = runAnalysisPipeline(journal.getClassroomId(), journal.getJournalDate());
        PersistResult persisted = journalPersistService.overwrite(
                journal.getId(), outcome.contentJson(), outcome.memoIds(), outcome.memoIdToArea(),
                outcome.analyzedAt());
        return new JournalResponse(persisted.journalId(), journal.getClassroomId(), journal.getJournalDate(),
                JournalStatus.DRAFT.name(), outcome.content(), persisted.analyzedAt(), persisted.linkedMemoIds());
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
