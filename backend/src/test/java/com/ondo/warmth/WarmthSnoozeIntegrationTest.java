package com.ondo.warmth;

import com.ondo.auth.TeacherRepository;
import com.ondo.auth.domain.Teacher;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.child.ChildRepository;
import com.ondo.child.domain.Child;
import com.ondo.child.domain.Gender;
import com.ondo.classroom.ClassroomRepository;
import com.ondo.classroom.domain.Classroom;
import com.ondo.common.time.AppTime;
import com.ondo.memo.MemoRepository;
import com.ondo.memo.domain.Memo;
import com.ondo.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관찰 온도 '잠시 접어두기'(spec-child-warmth-snooze).
 * 만료 경계가 이 기능의 안전 속성이라 오늘/오늘+1 양쪽을 직접 고정해 검증한다.
 */
@AutoConfigureMockMvc
@Transactional
class WarmthSnoozeIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate BIRTH = LocalDate.of(2021, 4, 10);
    private static final int OLD_ENOUGH_DAYS = 30;

    @Autowired private MockMvc mockMvc;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private MemoRepository memoRepository;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @PersistenceContext private EntityManager em;

    private Long teacherId;
    private String token;
    private Long classroomId;
    private Long otherChildId;
    private int aliasSeq;

    @BeforeEach
    void setUp() {
        aliasSeq = 0;
        Teacher me = teacherRepository.save(Teacher.create("snz+" + System.nanoTime() + "@ondo.dev", "h", "교사A"));
        Teacher other = teacherRepository.save(Teacher.create("snz+" + System.nanoTime() + "@ondo.dev", "h", "교사B"));
        teacherId = me.getId();
        token = jwtProvider.createToken(me.getId(), me.getEmail());
        classroomId = classroomRepository.save(
                Classroom.create(me.getId(), "햇살반", 2026, LocalDate.of(2026, 3, 2))).getId();
        Long otherClassroom = classroomRepository.save(
                Classroom.create(other.getId(), "달님반", 2026, LocalDate.of(2026, 3, 2))).getId();
        otherChildId = childRepository.save(
                Child.create(otherClassroom, "남의아이", BIRTH, Gender.MALE, "남A")).getId();
    }

    // ---------- 헬퍼 ----------

    private Long oldChild(String name) {
        Long id = childRepository.save(
                Child.create(classroomId, name, BIRTH, Gender.MALE, "아이" + (++aliasSeq))).getId();
        jdbcTemplate.update("UPDATE child SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(AppTime.startOfDayUtc(AppTime.today().minusDays(OLD_ENOUGH_DAYS))), id);
        return id;
    }

    private void memos(Long childId, int daysAgo, int count) {
        LocalDateTime at = AppTime.startOfDayUtc(AppTime.today().minusDays(daysAgo)).plusHours(2);
        for (int i = 0; i < count; i++) {
            Long memoId = memoRepository.save(
                    Memo.create(childId, teacherId, "블록놀이를 했어요", null, null, null)).getId();
            jdbcTemplate.update("UPDATE memo SET created_at = ? WHERE id = ?", Timestamp.valueOf(at), memoId);
        }
    }

    /** 만료일을 직접 고정한다(경계 검증용). */
    private void snoozeUntil(Long childId, LocalDate until) {
        jdbcTemplate.update("UPDATE child SET warmth_snoozed_until = ? WHERE id = ?", Date.valueOf(until), childId);
    }

    private void fillers(int count) {
        for (int i = 1; i <= count; i++) {
            memos(oldChild(String.format("배경%02d", i)), 1, 5);
        }
    }

    private Map<String, Object> fetchBody() throws Exception {
        em.flush();
        em.clear();
        String json = mockMvc.perform(get("/api/v1/classrooms/{id}/warmth", classroomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, Map.class);
    }

    private Map<Long, String> fetch() throws Exception {
        Map<Long, String> levels = new LinkedHashMap<>();
        for (Object item : (List<?>) fetchBody().get("items")) {
            Map<?, ?> m = (Map<?, ?>) item;
            levels.put(((Number) m.get("childId")).longValue(), (String) m.get("level"));
        }
        return levels;
    }

    private void snoozeVia(Long childId, int expectedStatus) throws Exception {
        em.flush();
        em.clear();
        mockMvc.perform(post("/api/v1/children/{id}/warmth-snooze", childId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }

    // ---------- 접기 · 해제 ----------

    @Test
    void 접어두면_SNOOZED가_되고_만료일이_오늘부터_14일_뒤다() throws Exception {
        fillers(8);
        Long absent = oldChild("결석한아이");

        snoozeVia(absent, 204);

        Map<String, Object> body = fetchBody();
        Map<?, ?> item = (Map<?, ?>) ((List<?>) body.get("items")).stream()
                .filter(o -> ((Number) ((Map<?, ?>) o).get("childId")).longValue() == absent)
                .findFirst().orElseThrow();
        assertThat(item.get("level")).isEqualTo("SNOOZED");
        assertThat(item.get("snoozedUntil")).isEqualTo(AppTime.today().plusDays(14).toString());
    }

    @Test
    void 접힌_아이는_LOW가_되지_않는다() throws Exception {
        fillers(8);
        Long absent = oldChild("결석한아이"); // 메모 0건 → 접지 않으면 LOW
        assertThat(fetch().get(absent)).isEqualTo("LOW");

        snoozeVia(absent, 204);

        assertThat(fetch().get(absent)).isEqualTo("SNOOZED");
    }

    @Test
    void 해제하면_즉시_판정_대상으로_돌아온다() throws Exception {
        fillers(8);
        Long absent = oldChild("돌아온아이");
        snoozeVia(absent, 204);
        assertThat(fetch().get(absent)).isEqualTo("SNOOZED");

        em.flush();
        em.clear();
        mockMvc.perform(delete("/api/v1/children/{id}/warmth-snooze", absent)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(fetch().get(absent)).isEqualTo("LOW");
    }

    @Test
    void 접혀있지_않은_아이를_해제해도_성공한다() throws Exception {
        Long child = oldChild("멀쩡한아이");

        em.flush();
        em.clear();
        mockMvc.perform(delete("/api/v1/children/{id}/warmth-snooze", child)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void 이미_접힌_아이를_다시_접으면_오늘_기준으로_갱신된다() throws Exception {
        fillers(8);
        Long absent = oldChild("계속결석");
        snoozeUntil(absent, AppTime.today().plusDays(2)); // 곧 만료될 예정이었다

        snoozeVia(absent, 204);

        Map<?, ?> item = (Map<?, ?>) ((List<?>) fetchBody().get("items")).stream()
                .filter(o -> ((Number) ((Map<?, ?>) o).get("childId")).longValue() == absent)
                .findFirst().orElseThrow();
        // 누적(2+14)이 아니라 오늘+14 로 갱신
        assertThat(item.get("snoozedUntil")).isEqualTo(AppTime.today().plusDays(14).toString());
    }

    // ---------- 만료 경계 (이 기능의 안전 속성) ----------

    @Test
    void 만료일_당일에는_이미_만료다() throws Exception {
        fillers(8);
        Long child = oldChild("오늘만료");
        snoozeUntil(child, AppTime.today());

        assertThat(fetch().get(child)).isEqualTo("LOW"); // 판정 대상으로 복귀
    }

    @Test
    void 만료_하루_전에는_아직_접혀있다() throws Exception {
        fillers(8);
        Long child = oldChild("내일만료");
        snoozeUntil(child, AppTime.today().plusDays(1));

        assertThat(fetch().get(child)).isEqualTo("SNOOZED");
    }

    // ---------- 판정 통합 ----------

    @Test
    void 접힌_아이는_평균_계산에서도_빠진다() throws Exception {
        // 활발한 아이 4명 + 메모 0건 5명. 0건 아이들이 평균을 끌어내리는 구조.
        for (int i = 1; i <= 4; i++) {
            memos(oldChild(String.format("활발%02d", i)), 1, 5);
        }
        Long borderline = oldChild("경계아이");
        memos(borderline, 10, 2); // 오래된 기록 소량 — 접기 전엔 LOW 를 면한다
        for (int i = 1; i <= 4; i++) {
            oldChild(String.format("무기록%02d", i));
        }
        assertThat(fetch().get(borderline)).isEqualTo("WARM");

        // 0건 아이 4명을 모두 접으면 평균이 올라가고, 경계 아이가 임계 아래로 내려간다
        for (Child c : childRepository.findByClassroomIdOrderByNameAscIdAsc(classroomId)) {
            if (c.getName().startsWith("무기록")) snoozeVia(c.getId(), 204);
        }

        assertThat(fetch().get(borderline)).isEqualTo("LOW");
    }

    @Test
    void 반_전원이_접히면_판정하지_않는다() throws Exception {
        for (int i = 1; i <= 5; i++) {
            memos(oldChild(String.format("아이%02d", i)), 1, 5);
        }
        for (Child c : childRepository.findByClassroomIdOrderByNameAscIdAsc(classroomId)) {
            snoozeVia(c.getId(), 204);
        }

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/v1/classrooms/{id}/warmth", classroomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void 접힌_아이의_메모는_콜드스타트_행수에도_들어가지_않는다() throws Exception {
        Long absent = oldChild("결석한아이");
        memos(absent, 1, 50); // 세어지면 콜드 스타트를 넘겨버린다
        for (int i = 1; i <= 5; i++) {
            oldChild(String.format("아이%02d", i)); // 메모 0건, 대상 5명
        }
        snoozeVia(absent, 204);

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/v1/classrooms/{id}/warmth", classroomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void 접힌_아이는_snoozedUntil만_실리고_점수는_실리지_않는다() throws Exception {
        fillers(8);
        Long absent = oldChild("결석한아이");
        snoozeVia(absent, 204);

        em.flush();
        em.clear();
        String json = mockMvc.perform(get("/api/v1/classrooms/{id}/warmth", classroomId)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();

        assertThat(json).doesNotContain("score").doesNotContain("count");
    }

    // ---------- 인가 ----------

    @Test
    void 남의_아이는_접을_수_없다() throws Exception {
        em.flush();
        em.clear();
        mockMvc.perform(post("/api/v1/children/{id}/warmth-snooze", otherChildId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHILD_NOT_FOUND"));
    }

    @Test
    void 인증_없이는_접을_수_없다() throws Exception {
        Long child = oldChild("아무개");
        em.flush();
        em.clear();
        mockMvc.perform(post("/api/v1/children/{id}/warmth-snooze", child))
                .andExpect(status().isUnauthorized());
    }
}
