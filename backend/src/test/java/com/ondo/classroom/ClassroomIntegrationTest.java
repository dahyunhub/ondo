package com.ondo.classroom;

import com.ondo.auth.TeacherRepository;
import com.ondo.auth.domain.Teacher;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.classroom.domain.Classroom;
import com.ondo.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ClassroomIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private ClassroomRepository classroomRepository;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenA;

    @BeforeEach
    void setUp() {
        Teacher teacherA = teacherRepository.save(Teacher.create("a@ondo.dev", "hash", "교사A"));
        Teacher teacherB = teacherRepository.save(Teacher.create("b@ondo.dev", "hash", "교사B"));

        // 교사A: 동명 반 2개(학년도 구분)
        Classroom sunny2026 = classroomRepository.save(
                Classroom.create(teacherA.getId(), "햇살반", 2026, LocalDate.of(2026, 3, 2)));
        Classroom sunny2025 = classroomRepository.save(
                Classroom.create(teacherA.getId(), "햇살반", 2025, LocalDate.of(2025, 3, 2)));
        // 교사B: 다른 반(소유권 테스트용)
        classroomRepository.save(
                Classroom.create(teacherB.getId(), "달님반", 2026, LocalDate.of(2026, 3, 2)));

        // 2026 반: 활성 2 + soft delete 1 → childCount 2
        insertChild(sunny2026.getId(), "아이1", "s26-1", false);
        insertChild(sunny2026.getId(), "아이2", "s26-2", false);
        insertChild(sunny2026.getId(), "아이3", "s26-3", true);
        // 2025 반: 활성 1 → childCount 1
        insertChild(sunny2025.getId(), "아이4", "s25-1", false);

        tokenA = jwtProvider.createToken(teacherA.getId(), teacherA.getEmail());
    }

    @Test
    void 본인_소유_반만_아이수와_함께_학년도_내림차순으로_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // year DESC → 2026 먼저
                .andExpect(jsonPath("$[0].name").value("햇살반"))
                .andExpect(jsonPath("$[0].year").value(2026))
                .andExpect(jsonPath("$[0].startDate").value("2026-03-02"))
                .andExpect(jsonPath("$[0].childCount").value(2))
                .andExpect(jsonPath("$[1].year").value(2025))
                .andExpect(jsonPath("$[1].childCount").value(1));
    }

    @Test
    void 새_반을_생성하면_201과_반정보를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"별빛반\",\"year\":2026}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("별빛반"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.startDate").value("2026-03-02")) // 학년도 3월 2일 자동
                .andExpect(jsonPath("$.childCount").value(0));

        // 목록에도 추가됨(교사A: 기존 2 + 1 = 3)
        mockMvc.perform(get("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void 같은_학년도_같은_이름_반은_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(post("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"햇살반\",\"year\":2026}")) // 이미 존재
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    // ---------- 반 연령(spec-classroom-age-birthdate) ----------

    @Test
    void 만_나이를_함께_등록하면_응답과_목록에_반영된다() throws Exception {
        mockMvc.perform(post("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"별빛반\",\"year\":2026,\"ageClass\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ageClass").value(4));

        mockMvc.perform(get("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$[?(@.name=='별빛반')].ageClass").value(org.hamcrest.Matchers.hasItem(4)));
    }

    @Test
    void 만_나이는_선택_항목이라_없어도_생성된다() throws Exception {
        // 혼합연령반이거나 아직 모르는 경우 — 필수로 만들면 등록 자체가 막힌다
        mockMvc.perform(post("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"구름반\",\"year\":2026}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ageClass").doesNotExist());
    }

    @Test
    void 만_나이_경계값_0과_5는_허용된다() throws Exception {
        for (int age : new int[]{0, 5}) {
            mockMvc.perform(post("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"경계" + age + "반\",\"year\":2026,\"ageClass\":" + age + "}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ageClass").value(age));
        }
    }

    @Test
    void 만_나이_범위_밖은_400이다() throws Exception {
        for (String age : new String[]{"-1", "6"}) {
            mockMvc.perform(post("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"범위밖" + age.replace("-", "m") + "반\",\"year\":2026,\"ageClass\":" + age + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Test
    void 기존_반은_만_나이가_null_로_조회된다() throws Exception {
        // 마이그레이션으로 값을 추측해 채우지 않는다 — 틀린 연도를 기본값으로 밀어주는 게 더 나쁘다
        mockMvc.perform(get("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$[?(@.name=='햇살반')].ageClass")
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())));
    }

    @Test
    void 토큰_없이_접근하면_401() throws Exception {
        mockMvc.perform(get("/api/v1/classrooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHENTICATED"));
    }

    private void insertChild(Long classroomId, String name, String alias, boolean deleted) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp deletedAt = deleted ? now : null;
        jdbcTemplate.update("""
                        INSERT INTO child (classroom_id, name, birth_date, gender, token_alias, deleted_at, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                classroomId, name, Date.valueOf(LocalDate.of(2021, 4, 10)), "MALE", alias, deletedAt, now, now);
    }
}
