package com.ondo.journal;

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
import com.ondo.common.exception.AiAnalysisException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.common.exception.BusinessException;
import com.ondo.memo.MemoRepository;
import com.ondo.memo.domain.CurriculumArea;
import com.ondo.memo.domain.Memo;
import com.ondo.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/v1/journals/analyze 통합 테스트(Story 3.5). 외부 AI 는 StubAiClient 로 대체.
 * REQUIRES_NEW 저장은 테스트 트랜잭션 롤백과 무관하게 커밋되므로 @Transactional 미사용 — 테스트마다 새 teacher/classroom(고유 id)으로 격리.
 */
@AutoConfigureMockMvc
@Import(JournalIntegrationTest.StubAiConfig.class)
class JournalIntegrationTest extends IntegrationTestSupport {

    private static final String HAPPY_JSON = """
            {"summary":"오늘은 놀이와 또래 상호작용이 활발했어요",
             "areas":{"PHYSICAL_HEALTH":"대근육 활동","COMMUNICATION":"친구와 대화","SOCIAL":"협력 놀이","ART":"그림 표현","NATURE":"관찰 탐구"},
             "memoClassifications":[{"index":1,"area":"SOCIAL"},{"index":2,"area":"ART"}]}
            """;
    // 파싱은 되지만 OutputValidator 실패(메모 인덱스 미커버) → AI_OUTPUT_INVALID
    private static final String INVALID_JSON = """
            {"summary":"부족","areas":{"PHYSICAL_HEALTH":"a","COMMUNICATION":"b","SOCIAL":"c","ART":"d","NATURE":"e"},
             "memoClassifications":[]}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private MemoRepository memoRepository;
    @Autowired private DailyJournalRepository dailyJournalRepository;
    @Autowired private JournalPersistService journalPersistService;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private StubAiClient stubAiClient;

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);
    private String tokenA;
    private Long teacherAId;
    private Long classroomAId;
    private Long classroomBId; // 교사 B 소유
    private Long memo1Id;
    private Long memo2Id;

    @BeforeEach
    void setUp() {
        stubAiClient.reset();
        Teacher a = teacherRepository.save(Teacher.create("a+" + System.nanoTime() + "@ondo.dev", "h", "교사A"));
        Teacher b = teacherRepository.save(Teacher.create("b+" + System.nanoTime() + "@ondo.dev", "h", "교사B"));
        classroomAId = classroomRepository.save(Classroom.create(a.getId(), "햇살반", 2026, LocalDate.of(2026, 3, 2))).getId();
        classroomBId = classroomRepository.save(Classroom.create(b.getId(), "달님반", 2026, LocalDate.of(2026, 3, 2))).getId();
        Long childA1 = childRepository.save(Child.create(classroomAId, "김민준", LocalDate.of(2021, 1, 1), Gender.MALE, "아이A")).getId();
        Long childA2 = childRepository.save(Child.create(classroomAId, "박서윤", LocalDate.of(2021, 2, 1), Gender.FEMALE, "아이B")).getId();
        memo1Id = memoRepository.save(Memo.create(childA1, a.getId(), "김민준이가 블록놀이에 집중", "블록 쌓기", "친구와 협력", "적극적")).getId();
        memo2Id = memoRepository.save(Memo.create(childA2, a.getId(), "박서윤이 그림 그리기", "그림", null, null)).getId();
        teacherAId = a.getId();
        tokenA = jwtProvider.createToken(a.getId(), a.getEmail());
    }

    private org.springframework.test.web.servlet.ResultActions analyze(String token, Long classroomId) throws Exception {
        return mockMvc.perform(post("/api/v1/journals/analyze").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"classroomId\":" + classroomId + ",\"date\":\"" + today + "\"}"));
    }

    @Test
    void 하루치_메모로_일지_초안을_생성한다_201() throws Exception {
        stubAiClient.enqueue(() -> HAPPY_JSON);

        analyze(tokenA, classroomAId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.content.summary").isNotEmpty())
                .andExpect(jsonPath("$.content.SOCIAL").value("협력 놀이"))
                .andExpect(jsonPath("$.linkedMemoIds.length()").value(2));

        // 메모 영역 자동 분류(FR-2)
        assertThat(memoRepository.findById(memo1Id).orElseThrow().getCurriculumArea()).isEqualTo(CurriculumArea.SOCIAL);
        assertThat(memoRepository.findById(memo2Id).orElseThrow().getCurriculumArea()).isEqualTo(CurriculumArea.ART);
    }

    @Test
    void 실명은_비식별화되어_프롬프트로_나간다() throws Exception {
        stubAiClient.enqueue(() -> HAPPY_JSON);

        analyze(tokenA, classroomAId).andExpect(status().isCreated());

        String userPrompt = stubAiClient.lastRequest.userPrompt();
        assertThat(userPrompt).doesNotContain("김민준", "박서윤");
        assertThat(userPrompt).contains("[[CHILD_");
    }

    @Test
    void 같은_날짜_재생성은_409_JOURNAL_ALREADY_EXISTS() throws Exception {
        stubAiClient.enqueue(() -> HAPPY_JSON);
        analyze(tokenA, classroomAId).andExpect(status().isCreated());

        analyze(tokenA, classroomAId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOURNAL_ALREADY_EXISTS"));
    }

    @Test
    void 타_교사_반은_404_CLASSROOM_NOT_FOUND() throws Exception {
        analyze(tokenA, classroomBId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLASSROOM_NOT_FOUND"));
    }

    @Test
    void 그_날짜_메모가_없으면_400_VALIDATION_FAILED() throws Exception {
        // today 엔 메모가 있으므로 메모가 없는 다른 날짜로 요청
        mockMvc.perform(post("/api/v1/journals/analyze").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classroomId\":" + classroomAId + ",\"date\":\"2020-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void AI_실패시_502이고_메모는_보존된다() throws Exception {
        stubAiClient.enqueue(() -> { throw new AiAnalysisException(ErrorCode.AI_ANALYSIS_FAILED); });

        analyze(tokenA, classroomAId)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_ANALYSIS_FAILED"));

        // 메모 보존(영역 미갱신) + 일지 미생성
        assertThat(memoRepository.findById(memo1Id).orElseThrow().getCurriculumArea()).isNull();
        assertThat(dailyJournalRepository.existsByTeacherIdAndClassroomIdAndJournalDate(
                jwtProvider.parseTeacherId(tokenA), classroomAId, today)).isFalse();
    }

    @Test
    void 출력검증_1차실패_2차성공이면_재요청후_생성된다() throws Exception {
        stubAiClient.enqueue(() -> INVALID_JSON); // 1차: 인덱스 미커버
        stubAiClient.enqueue(() -> HAPPY_JSON);   // 2차: 정상

        analyze(tokenA, classroomAId).andExpect(status().isCreated());
    }

    @Test
    void 출력검증_2회_연속실패면_502_AI_OUTPUT_INVALID() throws Exception {
        stubAiClient.enqueue(() -> INVALID_JSON);
        stubAiClient.enqueue(() -> INVALID_JSON);

        analyze(tokenA, classroomAId)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_OUTPUT_INVALID"));
    }

    @Test
    void persist_경합으로_UNIQUE위반시_JOURNAL_ALREADY_EXISTS로_변환된다() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        journalPersistService.save(teacherAId, classroomAId, today, "{}", List.of(memo1Id), Map.of(), now);

        // 선검사를 건너뛴 두 번째 insert(경합 모사) → DB UNIQUE 위반 → 계약 코드로 변환
        assertThatThrownBy(() -> journalPersistService.save(teacherAId, classroomAId, today, "{}", List.of(memo2Id), Map.of(), now))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.JOURNAL_ALREADY_EXISTS);
    }

    @Test
    void 저장시점_softDelete된_메모는_링크와_영역갱신에서_제외된다() {
        Memo m2 = memoRepository.findById(memo2Id).orElseThrow();
        m2.softDelete(); // 분석 후·저장 전 삭제 모사
        memoRepository.save(m2);

        JournalPersistService.PersistResult result = journalPersistService.save(
                teacherAId, classroomAId, today, "{}", List.of(memo1Id, memo2Id),
                Map.of(memo1Id, CurriculumArea.SOCIAL, memo2Id, CurriculumArea.ART),
                LocalDateTime.now(ZoneOffset.UTC));

        assertThat(result.linkedMemoIds()).containsExactly(memo1Id); // 삭제된 memo2 제외
        assertThat(memoRepository.findById(memo1Id).orElseThrow().getCurriculumArea()).isEqualTo(CurriculumArea.SOCIAL);
    }

    @TestConfiguration
    static class StubAiConfig {
        @Bean
        @Primary
        StubAiClient stubAiClient() {
            return new StubAiClient();
        }
    }

    /** 스크립트된 응답을 순서대로 반환하는 가짜 AiClient. 각 step 은 JSON 을 반환하거나 예외를 던질 수 있다. */
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
