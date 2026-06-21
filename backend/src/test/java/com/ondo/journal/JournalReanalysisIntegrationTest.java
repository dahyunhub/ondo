package com.ondo.journal;

import com.ondo.auth.TeacherRepository;
import com.ondo.auth.domain.Teacher;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.child.ChildRepository;
import com.ondo.child.domain.Child;
import com.ondo.child.domain.Gender;
import com.ondo.classroom.ClassroomRepository;
import com.ondo.classroom.domain.Classroom;
import com.ondo.journal.domain.DailyJournal;
import com.ondo.memo.MemoRepository;
import com.ondo.memo.domain.CurriculumArea;
import com.ondo.memo.domain.Memo;
import com.ondo.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [12] GET /journals(재분석 안내) · [15] POST /journals/{id}/analyze(덮어쓰기) 통합 테스트(Story 3.7).
 * AiClient 스텁은 3.5(JournalIntegrationTest)의 것을 재사용. REQUIRES_NEW 커밋 때문에 @Transactional 미사용 — 테스트별 고유 teacher.
 */
@AutoConfigureMockMvc
@Import(JournalIntegrationTest.StubAiConfig.class)
class JournalReanalysisIntegrationTest extends IntegrationTestSupport {

    private static final String HAPPY_JSON = """
            {"summary":"재분석된 요약",
             "areas":{"PHYSICAL_HEALTH":"신체","COMMUNICATION":"의사소통","SOCIAL":"협력","ART":"표현","NATURE":"탐구"},
             "memoClassifications":[{"index":1,"area":"SOCIAL"},{"index":2,"area":"ART"}]}
            """;
    private static final String SEED_CONTENT = """
            {"summary":"초안 요약","PHYSICAL_HEALTH":"a","COMMUNICATION":"b","SOCIAL":"c","ART":"d","NATURE":"e"}""";

    @Autowired private MockMvc mockMvc;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private MemoRepository memoRepository;
    @Autowired private DailyJournalRepository dailyJournalRepository;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private JournalIntegrationTest.StubAiClient stubAiClient;

    private final LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
    private Long teacherAId;
    private String tokenA;
    private String tokenB;
    private Long classroomAId;
    private Long childA1Id;
    private Long memo1Id;
    private Long memo2Id;
    private LocalDateTime baseTime; // 메모 생성 시각(재분석 안내 판정 기준)

    @BeforeEach
    void setUp() {
        stubAiClient.reset();
        Teacher a = teacherRepository.save(Teacher.create("a+" + System.nanoTime() + "@ondo.dev", "h", "교사A"));
        Teacher b = teacherRepository.save(Teacher.create("b+" + System.nanoTime() + "@ondo.dev", "h", "교사B"));
        teacherAId = a.getId();
        classroomAId = classroomRepository.save(Classroom.create(a.getId(), "햇살반", 2026, LocalDate.of(2026, 3, 2))).getId();
        childA1Id = childRepository.save(Child.create(classroomAId, "김민준", LocalDate.of(2021, 1, 1), Gender.MALE, "아이A")).getId();
        Long c2 = childRepository.save(Child.create(classroomAId, "박서윤", LocalDate.of(2021, 2, 1), Gender.FEMALE, "아이B")).getId();
        memo1Id = memoRepository.save(Memo.create(childA1Id, a.getId(), "김민준 블록놀이", "블록", "협력", "적극")).getId();
        memo2Id = memoRepository.save(Memo.create(c2, a.getId(), "박서윤 그림", "그림", null, null)).getId();
        baseTime = memoRepository.findById(memo1Id).orElseThrow().getCreatedAt();
        tokenA = jwtProvider.createToken(a.getId(), a.getEmail());
        tokenB = jwtProvider.createToken(b.getId(), b.getEmail());
    }

    private Long seedJournal(LocalDateTime analyzedAt) {
        return dailyJournalRepository.save(
                DailyJournal.createDraft(teacherAId, classroomAId, today, SEED_CONTENT, analyzedAt)).getId();
    }

    // ---------- [12] GET /journals ----------

    @Test
    void 분석_이후_추가메모가_있으면_reanalysisNeeded_true() throws Exception {
        seedJournal(baseTime.minusSeconds(10)); // 메모보다 먼저 분석된 상태

        mockMvc.perform(get("/api/v1/journals").header("Authorization", "Bearer " + tokenA)
                        .param("classroomId", classroomAId.toString()).param("date", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reanalysisNeeded").value(true))
                .andExpect(jsonPath("$.newMemoIds.length()").value(2));
    }

    @Test
    void 추가메모가_없으면_reanalysisNeeded_false() throws Exception {
        seedJournal(baseTime.plusSeconds(10)); // 메모보다 나중에 분석된 상태

        mockMvc.perform(get("/api/v1/journals").header("Authorization", "Bearer " + tokenA)
                        .param("classroomId", classroomAId.toString()).param("date", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reanalysisNeeded").value(false))
                .andExpect(jsonPath("$.newMemoIds.length()").value(0));
    }

    @Test
    void 분석_도중_추가된_메모도_재분석_안내에_잡힌다() throws Exception {
        // 스텁이 AI 호출 시점(묶음을 읽은 뒤)에 새 메모를 만든다 = '분석 도중 추가'.
        // analyzedAt 이 묶음 스냅샷(AI 호출 전) 시각이어야 이 메모가 createdAt > analyzedAt 으로 잡힌다.
        stubAiClient.enqueue(() -> {
            memoRepository.save(Memo.create(childA1Id, teacherAId, "분석 도중 추가", "x", null, null));
            return HAPPY_JSON;
        });

        mockMvc.perform(post("/api/v1/journals/analyze").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classroomId\":" + classroomAId + ",\"date\":\"" + today + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/journals").header("Authorization", "Bearer " + tokenA)
                        .param("classroomId", classroomAId.toString()).param("date", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reanalysisNeeded").value(true));
    }

    @Test
    void 필수_쿼리파라미터_누락은_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(get("/api/v1/journals").header("Authorization", "Bearer " + tokenA)
                        .param("date", today.toString())) // classroomId 누락
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 잘못된_date_형식은_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(get("/api/v1/journals").header("Authorization", "Bearer " + tokenA)
                        .param("classroomId", classroomAId.toString()).param("date", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 일지가_없으면_404() throws Exception {
        mockMvc.perform(get("/api/v1/journals").header("Authorization", "Bearer " + tokenA)
                        .param("classroomId", classroomAId.toString()).param("date", today.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOURNAL_NOT_FOUND"));
    }

    // ---------- [15] POST /journals/{id}/analyze ----------

    @Test
    void 재분석하면_덮어쓰기되고_안내가_해소된다() throws Exception {
        Long journalId = seedJournal(baseTime.minusSeconds(10)); // 재분석 필요 상태
        stubAiClient.enqueue(() -> HAPPY_JSON);

        mockMvc.perform(post("/api/v1/journals/" + journalId + "/analyze").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.content.summary").value("재분석된 요약"))
                .andExpect(jsonPath("$.linkedMemoIds.length()").value(2));

        // 메모 영역 재분류
        assertThat(memoRepository.findById(memo1Id).orElseThrow().getCurriculumArea()).isEqualTo(CurriculumArea.SOCIAL);
        assertThat(memoRepository.findById(memo2Id).orElseThrow().getCurriculumArea()).isEqualTo(CurriculumArea.ART);

        // analyzedAt 갱신 → 재분석 안내 자동 해소
        mockMvc.perform(get("/api/v1/journals").header("Authorization", "Bearer " + tokenA)
                        .param("classroomId", classroomAId.toString()).param("date", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reanalysisNeeded").value(false));
    }

    @Test
    void 타_교사_일지_재분석은_404() throws Exception {
        Long journalId = seedJournal(baseTime.minusSeconds(10));

        mockMvc.perform(post("/api/v1/journals/" + journalId + "/analyze").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOURNAL_NOT_FOUND"));
    }

    @Test
    void 없는_일지_재분석은_404() throws Exception {
        mockMvc.perform(post("/api/v1/journals/99999999/analyze").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOURNAL_NOT_FOUND"));
    }
}
