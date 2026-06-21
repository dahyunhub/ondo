package com.ondo.report;

import com.ondo.ai.AiClient;
import com.ondo.ai.dto.AiRequest;
import com.ondo.auth.TeacherRepository;
import com.ondo.auth.domain.Teacher;
import com.ondo.child.ChildRepository;
import com.ondo.child.domain.Child;
import com.ondo.child.domain.Gender;
import com.ondo.classroom.ClassroomRepository;
import com.ondo.classroom.domain.Classroom;
import com.ondo.memo.MemoRepository;
import com.ondo.memo.domain.Memo;
import com.ondo.report.MonthlyReportScheduler.MonthlySummary;
import com.ondo.report.domain.ChildReport;
import com.ondo.report.domain.ReportType;
import com.ondo.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 월말 자동 개인평가 스케줄러 통합 테스트(Story 4.2). 외부 AI 는 자체 StubAiClient.
 * 격리: runForMonth(findAll 전역 순회)는 타 테스트가 REQUIRES_NEW 로 커밋한 아이도 보므로,
 * 충돌 없는 과거 월(2000-01)을 대상으로 호출한다 → 타 데이터는 전부 SKIPPED_NO_MEMO(stub 미소비).
 * 이 테스트가 만든 아이에만 그달 메모를 넣되 created_at 을 native update 로 2000-01 로 백데이트한다.
 * REQUIRES_NEW 커밋 때문에 @Transactional 미사용 — 고유 teacher/child 로 격리.
 */
@Import(MonthlyReportSchedulerTest.StubAiConfig.class)
class MonthlyReportSchedulerTest extends IntegrationTestSupport {

    private static final YearMonth TARGET = YearMonth.of(2000, 1);
    private static final String HAPPY_JSON = """
            {"summary":"1월 한 달 관찰 요약",
             "areas":[{"area":"SOCIAL","text":"또래와 협력"},{"area":"ART","text":"표현 활동"}]}
            """;

    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private MemoRepository memoRepository;
    @Autowired private ChildReportRepository childReportRepository;
    @Autowired private ReportService reportService;
    @Autowired private MonthlyReportScheduler scheduler;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private StubAiClient stubAiClient;

    private Long teacherId;
    private Long classroomId;

    @BeforeEach
    void setUp() {
        stubAiClient.reset();
        Teacher t = teacherRepository.save(Teacher.create("sch+" + System.nanoTime() + "@ondo.dev", "h", "교사"));
        teacherId = t.getId();
        classroomId = classroomRepository.save(
                Classroom.create(t.getId(), "햇살반", 2000, LocalDate.of(2000, 1, 1))).getId();
    }

    /** 아이 생성 + 2000-01 로 백데이트된 메모 1건. */
    private Long childWithJanMemo(String name) {
        Long childId = childRepository.save(
                Child.create(classroomId, name, LocalDate.of(2021, 1, 1), Gender.MALE, name + System.nanoTime())).getId();
        Long memoId = memoRepository.save(Memo.create(childId, teacherId, name + "이가 블록놀이", "블록", "협력", "적극")).getId();
        jdbcTemplate.update("UPDATE memo SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.of(2000, 1, 15, 10, 0)), memoId);
        return childId;
    }

    @Test
    void 메모가_있는_아이는_MONTHLY로_생성된다() {
        Long childId = childWithJanMemo("김민준");
        stubAiClient.enqueue(() -> HAPPY_JSON);

        assertThat(reportService.createMonthly(childId, TARGET)).isEqualTo(MonthlyOutcome.CREATED);

        List<ChildReport> reports = childReportRepository.findByChildIdOrderByCreatedAtAsc(childId);
        assertThat(reports).hasSize(1);
        ChildReport r = reports.get(0);
        assertThat(r.getReportType()).isEqualTo(ReportType.MONTHLY);
        assertThat(r.getReportMonth()).isEqualTo("2000-01");
        assertThat(r.getPeriodStart()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(r.getPeriodEnd()).isEqualTo(LocalDate.of(2000, 1, 31));
    }

    @Test
    void 그달_메모가_없으면_SKIPPED_NO_MEMO() {
        Long childId = childRepository.save(
                Child.create(classroomId, "박서윤", LocalDate.of(2021, 2, 1), Gender.FEMALE, "nomemo" + System.nanoTime())).getId();

        assertThat(reportService.createMonthly(childId, TARGET)).isEqualTo(MonthlyOutcome.SKIPPED_NO_MEMO);
        assertThat(childReportRepository.findByChildIdOrderByCreatedAtAsc(childId)).isEmpty();
    }

    @Test
    void 이미_그달_평가가_있으면_SKIPPED_EXISTS_멱등() {
        Long childId = childWithJanMemo("이도윤");
        childReportRepository.save(
                ChildReport.createMonthly(childId, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 31), "2000-01", "{}"));

        // 메모가 있어도 이미 그달 평가가 있으므로 AI 호출 없이 skip(멱등). stub 미소비.
        assertThat(reportService.createMonthly(childId, TARGET)).isEqualTo(MonthlyOutcome.SKIPPED_EXISTS);
        assertThat(childReportRepository.findByChildIdOrderByCreatedAtAsc(childId)).hasSize(1);
    }

    @Test
    void 한_아이_실패해도_다른_아이는_생성된다_부분성공() {
        childWithJanMemo("최아인");
        childWithJanMemo("정시우");
        stubAiClient.enqueue(() -> { throw new IllegalStateException("AI 일시 오류"); }); // 첫 아이 실패
        stubAiClient.enqueue(() -> HAPPY_JSON);                                          // 둘째 아이 성공

        MonthlySummary summary = scheduler.runForMonth(TARGET);

        // 이 테스트의 두 아이만 2000-01 묶음에 잡힘(타 데이터는 SKIPPED_NO_MEMO). 한 건 실패, 한 건 생성.
        assertThat(summary.failed()).isEqualTo(1);
        assertThat(summary.created()).isEqualTo(1);
    }

    @TestConfiguration
    static class StubAiConfig {
        @Bean
        @Primary
        StubAiClient stubAiClient() {
            return new StubAiClient();
        }
    }

    static class StubAiClient implements AiClient {
        private final Queue<Supplier<String>> script = new ConcurrentLinkedQueue<>();
        volatile AiRequest lastRequest;

        @Override
        public String complete(AiRequest request) {
            this.lastRequest = request;
            Supplier<String> step = script.poll();
            if (step == null) {
                throw new IllegalStateException("StubAiClient: 스크립트된 응답이 없습니다");
            }
            return step.get();
        }

        void enqueue(Supplier<String> step) {
            script.add(step);
        }

        void reset() {
            script.clear();
            lastRequest = null;
        }
    }
}
