package com.ondo.report;

import com.ondo.ai.AiClient;
import com.ondo.ai.deid.Deidentifier;
import com.ondo.ai.deid.RestorationContext;
import com.ondo.ai.dto.AiRequest;
import com.ondo.ai.dto.MemoPromptInput;
import com.ondo.ai.dto.ReportAnalysisResult;
import com.ondo.ai.prompt.PromptTemplateLoader;
import com.ondo.ai.prompt.ReportAnalysisSchema;
import com.ondo.ai.prompt.ReportResultParser;
import com.ondo.ai.prompt.OutputValidator;
import com.ondo.child.ChildRepository;
import com.ondo.child.domain.Child;
import com.ondo.classroom.ClassroomRepository;
import com.ondo.classroom.domain.Classroom;
import com.ondo.common.concurrency.AnalysisGuard;
import com.ondo.common.time.AppTime;
import com.ondo.common.exception.AiAnalysisException;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.memo.MemoRepository;
import com.ondo.memo.domain.Memo;
import com.ondo.report.ReportPersistService.PersistResult;
import com.ondo.report.domain.ChildReport;
import com.ondo.report.domain.ReportType;
import com.ondo.report.dto.ReportResponse;
import com.ondo.report.dto.ReportSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 수동 개인평가 생성 오케스트레이션(Story 4.1). **no-TX** — AiClient 호출 동안 DB 커넥션을 점유하지 않는다.
 * AnalysisGuard.runExclusively 로 사용자당 동시 1건 보장, 저장은 ReportPersistService(REQUIRES_NEW)에 위임.
 * JournalService(3.5)와 동형. 정본: ai-integration-spec §4·§7, api-spec [16][17][18].
 */
@Service
public class ReportService {

    private static final int REPORT_MAX_TOKENS = 2048;

    private final ChildRepository childRepository;
    private final ClassroomRepository classroomRepository;
    private final ChildReportRepository childReportRepository;
    private final MemoRepository memoRepository;
    private final Deidentifier deidentifier;
    private final PromptTemplateLoader promptTemplateLoader;
    private final ReportResultParser reportResultParser;
    private final OutputValidator outputValidator;
    private final AiClient aiClient;
    private final AnalysisGuard analysisGuard;
    private final ReportContentSerializer contentSerializer;
    private final ReportPersistService reportPersistService;

    public ReportService(ChildRepository childRepository,
                         ClassroomRepository classroomRepository,
                         ChildReportRepository childReportRepository,
                         MemoRepository memoRepository,
                         Deidentifier deidentifier,
                         PromptTemplateLoader promptTemplateLoader,
                         ReportResultParser reportResultParser,
                         OutputValidator outputValidator,
                         AiClient aiClient,
                         AnalysisGuard analysisGuard,
                         ReportContentSerializer contentSerializer,
                         ReportPersistService reportPersistService) {
        this.childRepository = childRepository;
        this.classroomRepository = classroomRepository;
        this.childReportRepository = childReportRepository;
        this.memoRepository = memoRepository;
        this.deidentifier = deidentifier;
        this.promptTemplateLoader = promptTemplateLoader;
        this.reportResultParser = reportResultParser;
        this.outputValidator = outputValidator;
        this.aiClient = aiClient;
        this.analysisGuard = analysisGuard;
        this.contentSerializer = contentSerializer;
        this.reportPersistService = reportPersistService;
    }

    /** [16] 수동 평가 생성 — 사용자당 동시 1건 가드로 감싼 진입점. */
    public ReportResponse createManual(Long teacherId, Long childId) {
        return analysisGuard.runExclusively(teacherId, () -> doCreate(teacherId, childId));
    }

    private ReportResponse doCreate(Long teacherId, Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHILD_NOT_FOUND));
        Classroom classroom = ownedClassroom(child.getClassroomId(), teacherId, ErrorCode.CHILD_NOT_FOUND);

        // 기간: 직전 평가 period_end+1일 ~ 당일(KST), 없으면 반 start_date ~ 당일.
        LocalDate periodEnd = AppTime.today();
        LocalDate periodStart = childReportRepository.findTopByChildIdOrderByPeriodEndDesc(childId)
                .map(r -> r.getPeriodEnd().plusDays(1))
                .orElse(classroom.getStartDate());

        List<Memo> memos = memoRepository.findChildBundle(
                childId, AppTime.startOfDayUtc(periodStart), AppTime.startOfNextDayUtc(periodEnd));
        if (memos.isEmpty()) {
            throw new BusinessException(ErrorCode.REPORT_NO_MEMO);
        }
        ReportAnalysisResult result = runPipelineForMemos(child.getClassroomId(), memos);
        String json = contentSerializer.toJson(result);
        PersistResult persisted = reportPersistService.saveManual(childId, periodStart, periodEnd, json);

        return new ReportResponse(persisted.reportId(), childId, ReportType.MANUAL.name(),
                periodStart, periodEnd, null, contentSerializer.toContent(json), persisted.createdAt());
    }

    /** [FR-9] 월말 자동 평가 1건 생성. 시스템 잡이라 AnalysisGuard 없음(순차성은 스케줄러가 보장). */
    public MonthlyOutcome createMonthly(Long childId, java.time.YearMonth month) {
        String reportMonth = month.toString(); // "yyyy-MM"
        if (childReportRepository.existsByChildIdAndReportMonth(childId, reportMonth)) {
            return MonthlyOutcome.SKIPPED_EXISTS; // 멱등(재실행 안전) — AI 호출 전 차단
        }
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHILD_NOT_FOUND));

        LocalDate periodStart = month.atDay(1);
        LocalDate periodEnd = month.atEndOfMonth();
        List<Memo> memos = memoRepository.findChildBundle(
                childId, AppTime.startOfDayUtc(periodStart), AppTime.startOfNextDayUtc(periodEnd));
        if (memos.isEmpty()) {
            return MonthlyOutcome.SKIPPED_NO_MEMO; // 그달 메모 없는 아이 skip(throw 아님)
        }

        ReportAnalysisResult result = runPipelineForMemos(child.getClassroomId(), memos);
        reportPersistService.saveMonthly(childId, periodStart, periodEnd, reportMonth,
                contentSerializer.toJson(result));
        return MonthlyOutcome.CREATED;
    }

    /** 비식별화 → 프롬프트 → AiClient(TX 밖) → 복원·검증·1회 재요청. 호출 전 비어있지 않은 메모 묶음 전제(manual은 throw, monthly는 skip). */
    private ReportAnalysisResult runPipelineForMemos(Long classroomId, List<Memo> memos) {
        RestorationContext ctx = deidentifier.newContext(rosterNames(classroomId));
        List<MemoPromptInput> inputs = new ArrayList<>();
        int index = 1;
        for (Memo memo : memos) {
            inputs.add(new MemoPromptInput(index++,
                    deidentifier.deidentify(memo.getContent(), ctx),
                    deidentifier.deidentify(memo.getPlayActivity(), ctx),
                    deidentifier.deidentify(memo.getInteraction(), ctx),
                    deidentifier.deidentify(memo.getAttitude(), ctx)));
        }

        AiRequest request = new AiRequest(
                promptTemplateLoader.reportSystemPrompt(),
                promptTemplateLoader.renderReportMemos(inputs),
                ReportAnalysisSchema.schema(),
                REPORT_MAX_TOKENS);
        return analyzeWithRetry(request, ctx);
    }

    /** AI_OUTPUT_INVALID(파싱/검증 실패)면 1회 재요청. 연결·타임아웃 실패는 전파. */
    private ReportAnalysisResult analyzeWithRetry(AiRequest request, RestorationContext ctx) {
        try {
            return attempt(request, ctx);
        } catch (AiAnalysisException e) {
            if (e.getErrorCode() == ErrorCode.AI_OUTPUT_INVALID) {
                return attempt(request, ctx);
            }
            throw e;
        }
    }

    private ReportAnalysisResult attempt(AiRequest request, RestorationContext ctx) {
        String raw = aiClient.complete(request);
        ReportAnalysisResult result = reportResultParser.parse(ctx.restore(raw));
        outputValidator.validateReport(result);
        return result;
    }

    /** [17] 평가 목록 — 본인 아이만. 시간순, content 생략. */
    @Transactional(readOnly = true)
    public List<ReportSummaryResponse> list(Long teacherId, Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHILD_NOT_FOUND));
        ownedClassroom(child.getClassroomId(), teacherId, ErrorCode.CHILD_NOT_FOUND);
        return childReportRepository.findByChildIdOrderByCreatedAtAsc(childId).stream()
                .map(r -> new ReportSummaryResponse(r.getId(), r.getReportType().name(),
                        r.getPeriodStart(), r.getPeriodEnd(), r.getReportMonth(), r.getCreatedAt()))
                .toList();
    }

    /** [18] 평가 단건 — 본인 아이 평가만(없거나 타인 → REPORT_NOT_FOUND, 존재 비노출). */
    @Transactional(readOnly = true)
    public ReportResponse get(Long teacherId, Long reportId) {
        ChildReport report = childReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        Child child = childRepository.findById(report.getChildId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        ownedClassroom(child.getClassroomId(), teacherId, ErrorCode.REPORT_NOT_FOUND);
        return new ReportResponse(report.getId(), report.getChildId(), report.getReportType().name(),
                report.getPeriodStart(), report.getPeriodEnd(), report.getReportMonth(),
                contentSerializer.toContent(report.getContent()), report.getCreatedAt());
    }

    private Classroom ownedClassroom(Long classroomId, Long teacherId, ErrorCode notFound) {
        return classroomRepository.findByIdAndTeacherId(classroomId, teacherId)
                .orElseThrow(() -> new BusinessException(notFound));
    }

    private List<String> rosterNames(Long classroomId) {
        return childRepository.findByClassroomIdOrderByNameAscIdAsc(classroomId)
                .stream().map(Child::getName).toList();
    }
}
