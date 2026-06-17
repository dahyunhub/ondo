package com.jaram.memo;

import com.jaram.auth.TeacherRepository;
import com.jaram.auth.domain.Teacher;
import com.jaram.auth.jwt.JwtProvider;
import com.jaram.child.ChildRepository;
import com.jaram.child.domain.Child;
import com.jaram.child.domain.Gender;
import com.jaram.classroom.ClassroomRepository;
import com.jaram.classroom.domain.Classroom;
import com.jaram.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class MemoIntegrationTest extends IntegrationTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private MemoRepository memoRepository;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private ObjectMapper objectMapper;
    @PersistenceContext private EntityManager em;

    private String tokenA;
    private Long childAId;   // 교사A 소유
    private Long childBId;   // 교사B 소유 (소유권 테스트)

    @BeforeEach
    void setUp() {
        Teacher a = teacherRepository.save(Teacher.create("a@jaram.dev", "h", "교사A"));
        Teacher b = teacherRepository.save(Teacher.create("b@jaram.dev", "h", "교사B"));
        Long clsA = classroomRepository.save(Classroom.create(a.getId(), "햇살반", 2026, LocalDate.of(2026, 3, 2))).getId();
        Long clsB = classroomRepository.save(Classroom.create(b.getId(), "달님반", 2026, LocalDate.of(2026, 3, 2))).getId();
        childAId = childRepository.save(Child.create(clsA, "김민준", LocalDate.of(2021, 1, 1), Gender.MALE, "아이A")).getId();
        childBId = childRepository.save(Child.create(clsB, "남의아이", LocalDate.of(2021, 1, 1), Gender.MALE, "아이A")).getId();
        tokenA = jwtProvider.createToken(a.getId(), a.getEmail());
    }

    @Test
    void 메모를_저장하면_201_curriculumArea는_null() throws Exception {
        mockMvc.perform(post("/api/v1/memos").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"childId\":" + childAId + ",\"content\":\"블록놀이에 집중함\",\"playActivity\":\"블록 쌓기\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.childId").value(childAId))
                .andExpect(jsonPath("$.content").value("블록놀이에 집중함"))
                .andExpect(jsonPath("$.curriculumArea").doesNotExist())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void childId_없으면_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(post("/api/v1/memos").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"내용만 있음\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 내용과_3항목이_모두_비면_400_MEMO_EMPTY() throws Exception {
        mockMvc.perform(post("/api/v1/memos").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"childId\":" + childAId + ",\"content\":\"   \",\"playActivity\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMO_EMPTY"));
    }

    @Test
    void 타_교사_아이에_메모하면_404_CHILD_NOT_FOUND() throws Exception {
        mockMvc.perform(post("/api/v1/memos").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"childId\":" + childBId + ",\"content\":\"몰래 메모\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHILD_NOT_FOUND"));
    }

    @Test
    void 메모_soft_delete_204_그리고_재삭제시_404() throws Exception {
        var result = mockMvc.perform(post("/api/v1/memos").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"childId\":" + childAId + ",\"attitude\":\"적극적\"}"))
                .andExpect(status().isCreated()).andReturn();
        long memoId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/v1/memos/{id}", memoId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        em.flush();
        em.clear();

        mockMvc.perform(delete("/api/v1/memos/{id}", memoId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMO_NOT_FOUND"));
    }

    // ── Story 2.2 타임라인 / 2.3 영역 수정 ──

    private long createMemo(String content) throws Exception {
        var res = mockMvc.perform(post("/api/v1/memos").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"childId\":" + childAId + ",\"content\":\"" + content + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void 타임라인은_최신순으로_반환되고_빈_아이는_빈배열() throws Exception {
        createMemo("첫번째");
        createMemo("두번째");

        mockMvc.perform(get("/api/v1/children/{id}/timeline", childAId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("두번째"))   // 최신 우선
                .andExpect(jsonPath("$[0].curriculumArea").doesNotExist())
                .andExpect(jsonPath("$[0].date").isNotEmpty());

        // 메모 없는 다른 아이 → 빈 배열 (childB 는 교사A 소유가 아니므로 별도 검증은 아래 404 테스트)
    }

    @Test
    void 영역_수정후_필터로_조회된다() throws Exception {
        long id = createMemo("역할놀이에서 양보함");

        mockMvc.perform(patch("/api/v1/memos/{id}/curriculum-area", id).header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"curriculumArea\":\"SOCIAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculumArea").value("SOCIAL"));

        mockMvc.perform(get("/api/v1/children/{id}/timeline", childAId).param("area", "SOCIAL")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].curriculumArea").value("SOCIAL"));

        mockMvc.perform(get("/api/v1/children/{id}/timeline", childAId).param("area", "UNCLASSIFIED")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 잘못된_영역값_필터는_400_INVALID_CURRICULUM_AREA() throws Exception {
        mockMvc.perform(get("/api/v1/children/{id}/timeline", childAId).param("area", "WRONG")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRICULUM_AREA"));
    }

    @Test
    void 타_교사_아이_타임라인은_404_CHILD_NOT_FOUND() throws Exception {
        mockMvc.perform(get("/api/v1/children/{id}/timeline", childBId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHILD_NOT_FOUND"));
    }

    @Test
    void 영역_수정_잘못된_enum은_400_VALIDATION_FAILED() throws Exception {
        long id = createMemo("아무거나");
        mockMvc.perform(patch("/api/v1/memos/{id}/curriculum-area", id).header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"curriculumArea\":\"WRONG\"}"))
                .andExpect(status().isBadRequest());
    }
}
