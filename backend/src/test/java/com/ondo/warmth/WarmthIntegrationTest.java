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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관찰 온도 판정(spec-child-warmth). 날짜 의존 로직이라 child/memo 의 created_at 을 native update 로
 * 백데이트한 뒤 영속성 컨텍스트를 비우고(em.clear) 요청한다 — 안 비우면 1차 캐시의 옛 created_at 이 읽힌다.
 */
@AutoConfigureMockMvc
@Transactional
class WarmthIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate BIRTH = LocalDate.of(2021, 4, 10);
    /** 이 날짜 이전에 등록된 아이는 '신규'가 아니다(NEW_CHILD_DAYS 여유 있게). */
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
    private Long otherClassroomId;
    private int aliasSeq;

    @BeforeEach
    void setUp() {
        aliasSeq = 0;
        Teacher me = teacherRepository.save(Teacher.create("warmth+" + System.nanoTime() + "@ondo.dev", "h", "교사A"));
        Teacher other = teacherRepository.save(Teacher.create("warmth+" + System.nanoTime() + "@ondo.dev", "h", "교사B"));
        teacherId = me.getId();
        token = jwtProvider.createToken(me.getId(), me.getEmail());
        classroomId = classroomRepository.save(
                Classroom.create(me.getId(), "햇살반", 2026, LocalDate.of(2026, 3, 2))).getId();
        otherClassroomId = classroomRepository.save(
                Classroom.create(other.getId(), "달님반", 2026, LocalDate.of(2026, 3, 2))).getId();
    }

    // ---------- 픽스처 헬퍼 ----------

    /** 아이 등록 + created_at 을 {@code registeredDaysAgo} 일 전으로 백데이트. */
    private Long child(String name, int registeredDaysAgo) {
        Long id = childRepository.save(
                Child.create(classroomId, name, BIRTH, Gender.MALE, "아이" + (++aliasSeq))).getId();
        jdbcTemplate.update("UPDATE child SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(kstDay(registeredDaysAgo)), id);
        return id;
    }

    /** 등록한 지 충분히 지난(=판정 대상) 아이. */
    private Long oldChild(String name) {
        return child(name, OLD_ENOUGH_DAYS);
    }

    /** {@code daysAgo}일 전(KST) 새벽에 작성된 메모 {@code count}건. */
    private void memos(Long childId, int daysAgo, int count) {
        memosAt(childId, kstDay(daysAgo).plusHours(2), count);
    }

    /** 정확한 UTC 시각으로 메모 {@code count}건. 창 경계 검증용. */
    private void memosAt(Long childId, LocalDateTime createdAtUtc, int count) {
        for (int i = 0; i < count; i++) {
            Long memoId = memoRepository.save(
                    Memo.create(childId, teacherId, "블록놀이를 했어요", null, null, null)).getId();
            jdbcTemplate.update("UPDATE memo SET created_at = ? WHERE id = ?",
                    Timestamp.valueOf(createdAtUtc), memoId);
        }
    }

    /** KST 기준 {@code daysAgo}일 전 자정의 UTC LocalDateTime. */
    private static LocalDateTime kstDay(int daysAgo) {
        return AppTime.startOfDayUtc(AppTime.today().minusDays(daysAgo));
    }

    /** 판정에 영향만 주는 배경 아이들(전원 어제 메모 5건). */
    private void fillers(int count) {
        for (int i = 1; i <= count; i++) {
            memos(oldChild(String.format("배경%02d", i)), 1, 5);
        }
    }

    /** childId -> level. 요청 전 영속성 컨텍스트를 비운다. */
    private Map<Long, String> fetch() throws Exception {
        em.flush();
        em.clear();
        String json = mockMvc.perform(get("/api/v1/classrooms/{id}/warmth", classroomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> body = objectMapper.readValue(json, Map.class);
        Map<Long, String> levels = new LinkedHashMap<>();
        for (Object item : (List<?>) body.get("items")) {
            Map<?, ?> m = (Map<?, ?>) item;
            levels.put(((Number) m.get("childId")).longValue(), (String) m.get("level"));
        }
        return levels;
    }

    private static List<Long> lowIds(Map<Long, String> levels) {
        return levels.entrySet().stream().filter(e -> "LOW".equals(e.getValue())).map(Map.Entry::getKey).toList();
    }

    // ---------- 판정 ----------

    @Test
    void 기록이_편중되면_희박한_아이만_LOW가_된다() throws Exception {
        fillers(8);
        Long quietA = oldChild("조용한A");
        Long quietB = oldChild("조용한B");

        Map<Long, String> levels = fetch();

        assertThat(levels).hasSize(10);
        assertThat(lowIds(levels)).containsExactlyInAnyOrder(quietA, quietB);
    }

    @Test
    void 고르게_기록하면_LOW가_한_명도_없다() throws Exception {
        for (int i = 1; i <= 10; i++) {
            memos(oldChild(String.format("아이%02d", i)), 1, 4);
        }

        Map<Long, String> levels = fetch();

        assertThat(levels).hasSize(10);
        assertThat(lowIds(levels)).isEmpty();
    }

    @Test
    void LOW_는_후보가_많아도_가장_옅은_세_명까지만() throws Exception {
        for (int i = 1; i <= 4; i++) {
            memos(oldChild(String.format("활발%02d", i)), 1, 10);
        }
        for (int i = 1; i <= 6; i++) {
            oldChild(String.format("조용%02d", i)); // 메모 0건 → 후보 6명
        }

        Map<Long, String> levels = fetch();

        assertThat(levels).hasSize(10);
        assertThat(lowIds(levels)).hasSize(3);
    }

    /**
     * 리뷰 회귀: 예전엔 "후보가 대상의 1/3을 넘을 때만" 상한을 걸어서, 후보가 정확히 1/3 이하면
     * 상한이 통째로 빠졌다(15명 반 → LOW 5명). 상한은 조건 없이 항상 걸려야 한다.
     */
    @Test
    void 후보가_대상의_정확히_삼분의일이어도_상한이_걸린다() throws Exception {
        for (int i = 1; i <= 10; i++) {
            memos(oldChild(String.format("활발%02d", i)), 1, 5);
        }
        for (int i = 1; i <= 5; i++) {
            oldChild(String.format("조용%02d", i)); // 후보 5명 == 15/3
        }

        Map<Long, String> levels = fetch();

        assertThat(levels).hasSize(15);
        assertThat(lowIds(levels)).hasSize(3);
    }

    /**
     * 리뷰 회귀: 상한이 조건부였을 땐 규칙이 비단조적이었다 — 방치된 아이가 5명이면 LOW 5명,
     * 6명이면 오히려 LOW 3명. 방치가 늘었는데 목록이 줄어드는 일은 없어야 한다.
     */
    @Test
    void 방치된_아이가_늘어도_LOW_목록이_줄지_않는다() throws Exception {
        for (int i = 1; i <= 10; i++) {
            memos(oldChild(String.format("활발%02d", i)), 1, 5);
        }
        for (int i = 1; i <= 5; i++) {
            oldChild(String.format("조용%02d", i));
        }
        int before = lowIds(fetch()).size();

        oldChild("조용06"); // 방치 아이 1명 추가
        int after = lowIds(fetch()).size();

        assertThat(after).isGreaterThanOrEqualTo(before);
    }

    @Test
    void 삭제된_메모는_점수에_반영되지_않는다() throws Exception {
        fillers(8);
        Long kept = oldChild("살아있는기록");
        Long erased = oldChild("지운기록");
        memos(kept, 1, 5);
        memos(erased, 1, 5);
        // erased 의 메모만 soft delete → 기록이 없는 것과 같아져야 한다
        jdbcTemplate.update("UPDATE memo SET deleted_at = ? WHERE child_id = ?",
                Timestamp.valueOf(LocalDateTime.now()), erased);

        Map<Long, String> levels = fetch();

        assertThat(levels.get(kept)).isEqualTo("WARM");
        assertThat(levels.get(erased)).isEqualTo("LOW");
    }

    @Test
    void 아이_전원이_신규면_판정하지_않는다() throws Exception {
        for (int i = 1; i <= 6; i++) {
            memos(child(String.format("오늘등록%02d", i), 0), 1, 5);
        }

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/v1/classrooms/{id}/warmth", classroomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void 미래_시각_메모는_창_밖이라_집계되지_않는다() throws Exception {
        fillers(8);
        Long normal = oldChild("정상기록");
        Long skewed = oldChild("시계오차");
        memos(normal, 1, 5);
        // 시계 오차/백필로 들어온 미래 행 — 예전엔 경과일 클램프 때문에 최대 가중치로 잡혔다
        memosAt(skewed, kstDay(-3), 20);

        Map<Long, String> levels = fetch();

        assertThat(levels.get(normal)).isEqualTo("WARM");
        assertThat(levels.get(skewed)).isEqualTo("LOW");
    }

    @Test
    void 최근_기록이_옛날_기록보다_무겁다() throws Exception {
        fillers(8);
        Long many13DaysAgo = oldChild("옛날에많이");  // 13일 전 5건
        Long fewYesterday = oldChild("어제조금");     // 어제 2건
        memos(many13DaysAgo, 13, 5);
        memos(fewYesterday, 1, 2);

        Map<Long, String> levels = fetch();

        // 건수는 많지만 오래된 쪽이 LOW — 단순 카운트였다면 반대로 나온다.
        assertThat(levels.get(many13DaysAgo)).isEqualTo("LOW");
        assertThat(levels.get(fewYesterday)).isEqualTo("WARM");
    }

    @Test
    void 판정_창_밖의_메모는_집계되지_않는다() throws Exception {
        fillers(8);
        LocalDateTime windowStart = kstDay(13); // 창 하한(포함)
        Long onEdge = oldChild("경계안쪽");
        Long justOutside = oldChild("경계바깥");
        memosAt(onEdge, windowStart, 15);
        memosAt(justOutside, windowStart.minusSeconds(1), 15);

        Map<Long, String> levels = fetch();

        assertThat(levels.get(onEdge)).isEqualTo("WARM");
        assertThat(levels.get(justOutside)).isEqualTo("LOW");
    }

    // ---------- 가드 ----------

    @Test
    void 반_전체_기록이_아이_수보다_적으면_판정하지_않는다() throws Exception {
        for (int i = 1; i <= 10; i++) {
            oldChild(String.format("아이%02d", i));
        }
        memos(childRepository.findByClassroomIdOrderByNameAscIdAsc(classroomId).get(0).getId(), 1, 5);

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/v1/classrooms/{id}/warmth", classroomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void 아이가_없으면_판정하지_않는다() throws Exception {
        em.flush();
        em.clear();
        mockMvc.perform(get("/api/v1/classrooms/{id}/warmth", classroomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.windowDays").value(14));
    }

    @Test
    void 갓_등록한_아이는_WARM이고_평균을_끌어내리지_않는다() throws Exception {
        for (int i = 1; i <= 5; i++) {
            memos(oldChild(String.format("아이%02d", i)), 1, 3);
        }
        Long newcomer = child("오늘등록", 0);

        Map<Long, String> levels = fetch();

        // 신규가 평균에 포함됐다면 자신의 0점이 임계를 넘겨 스스로 LOW 가 됐을 것.
        assertThat(levels.get(newcomer)).isEqualTo("WARM");
        assertThat(lowIds(levels)).isEmpty();
    }

    @Test
    void 숨긴_아이는_응답에서_빠진다() throws Exception {
        for (int i = 1; i <= 6; i++) {
            memos(oldChild(String.format("아이%02d", i)), 1, 4);
        }
        Child hidden = childRepository.findByClassroomIdOrderByNameAscIdAsc(classroomId).get(0);
        Long hiddenId = hidden.getId();
        hidden.softDelete();
        childRepository.save(hidden);

        Map<Long, String> levels = fetch();

        assertThat(levels).hasSize(5);
        assertThat(levels).doesNotContainKey(hiddenId);
    }

    /**
     * 리뷰 지적: 기존 '숨긴 아이' 테스트는 명단에서 빠지는 것만 봐서, 숨긴 아이의 <b>메모</b>가
     * 집계에서 빠지는지는 검증하지 못했다(포함이든 제외든 단언이 통과). 여기선 숨긴 아이의 메모가
     * 세어지면 콜드 스타트 가드를 넘고, 제외되면 못 넘도록 수치를 잡아 판별 가능하게 만든다.
     */
    @Test
    void 숨긴_아이의_메모는_콜드스타트_집계에도_들어가지_않는다() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            ids.add(oldChild(String.format("아이%02d", i)));
        }
        Long hiddenId = ids.get(0);
        memos(hiddenId, 1, 50); // 세어지면 rows=54 >= 5 → enabled:true 가 되어버린다
        Child hidden = childRepository.findById(hiddenId).orElseThrow();
        hidden.softDelete();
        childRepository.save(hidden);
        // 보이는 5명 중 4명에게만 1건씩 → 집계 대상 메모 4건 < 대상 5명 → 콜드 스타트여야 정상
        for (int i = 1; i <= 4; i++) {
            memos(ids.get(i), 1, 1);
        }

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/v1/classrooms/{id}/warmth", classroomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    // ---------- 인가 · 응답 형태 ----------

    @Test
    void 남의_반은_조회할_수_없다() throws Exception {
        em.flush();
        em.clear();
        mockMvc.perform(get("/api/v1/classrooms/{id}/warmth", otherClassroomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLASSROOM_NOT_FOUND"));
    }

    @Test
    void 인증_없이는_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/classrooms/{id}/warmth", classroomId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 응답에는_점수나_건수가_실리지_않는다() throws Exception {
        for (int i = 1; i <= 5; i++) {
            memos(oldChild(String.format("아이%02d", i)), 1, 3);
        }

        em.flush();
        em.clear();
        String json = mockMvc.perform(get("/api/v1/classrooms/{id}/warmth", classroomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn().getResponse().getContentAsString();

        // 허용된 키만 실린다 — 점수·건수가 새어 나가면 화면에서 등수가 된다.
        // 개수가 아니라 이름을 고정해야 새 필드가 조용히 끼어드는 걸 잡는다.
        Map<String, Object> body = objectMapper.readValue(json, Map.class);
        for (Object item : (List<?>) body.get("items")) {
            assertThat(((Map<?, ?>) item).keySet().stream().map(String::valueOf).toList())
                    .containsExactlyInAnyOrder("childId", "level", "snoozedUntil");
        }
        assertThat(json).doesNotContain("score").doesNotContain("count");
    }
}
