package com.ondo.report;

import com.ondo.ai.AiClient;
import com.ondo.ai.dto.AiRequest;
import com.ondo.auth.TeacherRepository;
import com.ondo.auth.domain.Teacher;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.child.ChildRepository;
import com.ondo.child.domain.Child;
import com.ondo.child.domain.Gender;
import com.ondo.classroom.ClassroomRepository;
import com.ondo.classroom.domain.Classroom;
import com.ondo.memo.MemoRepository;
import com.ondo.memo.domain.Memo;
import com.ondo.report.domain.ChildReport;
import com.ondo.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 수동 개인평가 API 통합 테스트(Story 4.1). 외부 AI 는 JournalIntegrationTest 의 StubAiClient 재사용.
 * REQUIRES_NEW 저장은 테스트 롤백과 무관하게 커밋되므로 @Transactional 미사용 — 테스트마다 고유 teacher/child 로 격리.
 */
@AutoConfigureMockMvc
@Import(ReportIntegrationTest.StubAiConfig.class)
class ReportIntegrationTest extends IntegrationTestSupport {

    private static final String HAPPY_JSON = """
            {"summary":"기간 동안 또래와 협력하며 표현이 풍부해졌어요",
             "areas":[{"area":"SOCIAL","text":"친구와 협력 놀이"},{"area":"ART","text":"그림으로 표현"}]}
            """;
    // 파싱은 되지만 OutputValidator 실패(areas 비어있음) → AI_OUTPUT_INVALID
    private static final String INVALID_JSON = """
            {"summary":"부족","areas":[]}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private MemoRepository memoRepository;
    @Autowired private ChildReportRepository childReportRepository;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private StubAiClient stubAiClient;

    private String tokenA;
    private Long teacherAId;
    private Long classroomAId;
    private Long childA1Id;
    private Long childBId; // 교사 B 소유

    @BeforeEach
    void setUp() {
        stubAiClient.reset();
        Teacher a = teacherRepository.save(Teacher.create("ra+" + System.nanoTime() + "@ondo.dev", "h", "교사A"));
        Teacher b = teacherRepository.save(Teacher.create("rb+" + System.nanoTime() + "@ondo.dev", "h", "교사B"));
        teacherAId = a.getId();
        classroomAId = classroomRepository.save(Classroom.create(a.getId(), "햇살반", 2026, LocalDate.of(2026, 3, 2))).getId();
        Long classroomBId = classroomRepository.save(Classroom.create(b.getId(), "달님반", 2026, LocalDate.of(2026, 3, 2))).getId();
        childA1Id = childRepository.save(Child.create(classroomAId, "김민준", LocalDate.of(2021, 1, 1), Gender.MALE, "아이A")).getId();
        childBId = childRepository.save(Child.create(classroomBId, "이서연", LocalDate.of(2021, 2, 1), Gender.FEMALE, "아이A")).getId();
        memoRepository.save(Memo.create(childA1Id, a.getId(), "김민준이가 블록놀이에 집중", "블록 쌓기", "친구와 협력", "적극적"));
        tokenA = jwtProvider.createToken(a.getId(), a.getEmail());
    }

    private org.springframework.test.web.servlet.ResultActions createReport(String token, Long childId) throws Exception {
        return mockMvc.perform(post("/api/v1/children/" + childId + "/reports")
                .header("Authorization", "Bearer " + token));
    }

    @Test
    void 메모가_있는_아이의_평가를_생성한다_201_MANUAL() throws Exception {
        stubAiClient.enqueue(() -> HAPPY_JSON);

        createReport(tokenA, childA1Id)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.childId").value(childA1Id))
                .andExpect(jsonPath("$.reportType").value("MANUAL"))
                .andExpect(jsonPath("$.reportMonth").doesNotExist())
                .andExpect(jsonPath("$.periodStart").value("2026-03-02")) // 직전 평가 없음 → 반 start_date
                .andExpect(jsonPath("$.content.summary").isNotEmpty())
                .andExpect(jsonPath("$.content.areas[0].area").value("SOCIAL"));
    }

    @Test
    void 실명은_비식별화되어_프롬프트로_나간다() throws Exception {
        stubAiClient.enqueue(() -> HAPPY_JSON);

        createReport(tokenA, childA1Id).andExpect(status().isCreated());

        String userPrompt = stubAiClient.lastRequest.userPrompt();
        assertThat(userPrompt).doesNotContain("김민준");
        assertThat(userPrompt).contains("[[CHILD_");
    }

    @Test
    void 대상아동_토큰을_명시하고_다른아이_축약형까지_비식별화한다() throws Exception {
        // 회귀 방지: 대상 아이 메모가 다른 아이를 축약형("서윤")으로 언급해도, 실명·축약형이 모두 마스킹되고
        // 프롬프트 첫머리에 '대상 아동' 토큰이 고정된다 — 엉뚱한 아이 이름이 요약 주어로 새는 버그를 막는다.
        childRepository.save(Child.create(classroomAId, "박서윤", LocalDate.of(2021, 3, 1), Gender.FEMALE, "아이C"));
        memoRepository.save(Memo.create(childA1Id, teacherAId, "서윤이와 블록을 번갈아 쌓음", null, "차례 지키기", null));
        stubAiClient.enqueue(() -> HAPPY_JSON);

        createReport(tokenA, childA1Id).andExpect(status().isCreated());

        String userPrompt = stubAiClient.lastRequest.userPrompt();
        assertThat(userPrompt).doesNotContain("김민준", "박서윤", "서윤"); // 대상·타아동 실명·축약형 모두 마스킹
        assertThat(userPrompt).contains("대상 아동은 [[CHILD_");        // 대상 토큰 고정
    }

    @Test
    void 직전_평가가_있으면_그_다음날부터_기간을_잡는다() throws Exception {
        childReportRepository.save(
                ChildReport.createManual(childA1Id, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 20), "{}"));
        stubAiClient.enqueue(() -> HAPPY_JSON);

        createReport(tokenA, childA1Id)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodStart").value("2026-05-21")); // 직전 period_end + 1일
    }

    @Test
    void 기간내_메모가_없으면_422_REPORT_NO_MEMO() throws Exception {
        // 직전 평가 period_end 를 오늘로 두면 다음 기간은 [내일, 오늘] → 비어 있어 메모가 없다(셋업 메모는 오늘 생성).
        childReportRepository.save(
                ChildReport.createManual(childA1Id, LocalDate.of(2026, 5, 1), LocalDate.now(), "{}"));

        createReport(tokenA, childA1Id)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REPORT_NO_MEMO"));
    }

    @Test
    void 타_교사의_아이면_404_CHILD_NOT_FOUND() throws Exception {
        createReport(tokenA, childBId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHILD_NOT_FOUND"));
    }

    @Test
    void 같은_아이_평가는_덮어쓰지_않고_누적된다() throws Exception {
        // 과거 기간 평가 1건이 이미 있고(report_month=null), 새 평가를 만들면 2건으로 누적된다.
        // (직전 period_end=3/31 → 새 기간 4/1~오늘 → 오늘 메모 포함). report_month=null 이라 UNIQUE 충돌 없음.
        childReportRepository.save(
                ChildReport.createManual(childA1Id, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 31), "{}"));
        stubAiClient.enqueue(() -> HAPPY_JSON);
        createReport(tokenA, childA1Id).andExpect(status().isCreated());

        assertThat(childReportRepository.findByChildIdOrderByCreatedAtAsc(childA1Id)).hasSize(2);
    }

    @Test
    void AI_출력이_부실하면_1회_재요청후_성공() throws Exception {
        stubAiClient.enqueue(() -> INVALID_JSON); // 1차: areas 비어있음 → AI_OUTPUT_INVALID
        stubAiClient.enqueue(() -> HAPPY_JSON);    // 2차 재요청: 성공

        createReport(tokenA, childA1Id).andExpect(status().isCreated());
    }

    @Test
    void 목록과_단건을_조회한다() throws Exception {
        stubAiClient.enqueue(() -> HAPPY_JSON);
        String location = createReport(tokenA, childA1Id).andReturn().getResponse().getContentAsString();
        Long reportId = childReportRepository.findByChildIdOrderByCreatedAtAsc(childA1Id).get(0).getId();

        // [17] 목록: content 생략
        mockMvc.perform(get("/api/v1/children/" + childA1Id + "/reports").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reportType").value("MANUAL"))
                .andExpect(jsonPath("$[0].content").doesNotExist());

        // [18] 단건: content 포함
        mockMvc.perform(get("/api/v1/reports/" + reportId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.summary").isNotEmpty());
        assertThat(location).contains("MANUAL");
    }

    @TestConfiguration
    static class StubAiConfig {
        @Bean
        @Primary
        StubAiClient stubAiClient() {
            return new StubAiClient();
        }
    }

    /** 스크립트된 응답을 순서대로 반환하는 가짜 AiClient(평가 경로용). */
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
