package com.ondo.photo;

import com.ondo.auth.TeacherRepository;
import com.ondo.auth.domain.Teacher;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.child.ChildRepository;
import com.ondo.child.domain.Child;
import com.ondo.child.domain.Gender;
import com.ondo.classroom.ClassroomRepository;
import com.ondo.classroom.domain.Classroom;
import com.ondo.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 프로필 이미지 API 통합 테스트(아이·교사). 클라이언트가 크롭한 작은 이미지 바이트 업로드/조회/삭제.
 */
@AutoConfigureMockMvc
@Transactional
class PhotoIntegrationTest extends IntegrationTestSupport {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4};

    @Autowired private MockMvc mockMvc;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private JwtProvider jwtProvider;

    private String tokenA;
    private Long childAId;
    private Long childBId; // 교사 B 소유

    @BeforeEach
    void setUp() {
        Teacher a = teacherRepository.save(Teacher.create("pa@ondo.dev", "h", "교사A"));
        Teacher b = teacherRepository.save(Teacher.create("pb@ondo.dev", "h", "교사B"));
        Long classA = classroomRepository.save(Classroom.create(a.getId(), "햇살반", 2026, LocalDate.of(2026, 3, 2))).getId();
        Long classB = classroomRepository.save(Classroom.create(b.getId(), "달님반", 2026, LocalDate.of(2026, 3, 2))).getId();
        childAId = childRepository.save(Child.create(classA, "강하준", LocalDate.of(2021, 1, 1), Gender.MALE, "아이A")).getId();
        childBId = childRepository.save(Child.create(classB, "이서연", LocalDate.of(2021, 2, 1), Gender.FEMALE, "아이A")).getId();
        tokenA = jwtProvider.createToken(a.getId(), a.getEmail());
    }

    @Test
    void 아이_사진_업로드_조회_삭제() throws Exception {
        // 업로드
        mockMvc.perform(put("/api/v1/children/{id}/photo", childAId).header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.IMAGE_PNG).content(PNG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUpdatedAt").isNotEmpty());

        // 목록 응답에 photoUpdatedAt 채워짐
        mockMvc.perform(get("/api/v1/classrooms/{cid}/children",
                        childRepository.findById(childAId).orElseThrow().getClassroomId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$[0].photoUpdatedAt").isNotEmpty());

        // 바이트 조회
        mockMvc.perform(get("/api/v1/children/{id}/photo", childAId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));

        // 삭제 후 404
        mockMvc.perform(delete("/api/v1/children/{id}/photo", childAId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/children/{id}/photo", childAId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    /**
     * 재검증 경로. 304 로 본문을 아끼는 것뿐 아니라, 이때 사진 바이트를 읽지 않는 것이 핵심이라
     * 응답 계약(304·ETag·Cache-Control)을 고정해 둔다.
     */
    @Test
    void If_None_Match_가_같으면_304_다르면_바이트를_준다() throws Exception {
        mockMvc.perform(put("/api/v1/children/{id}/photo", childAId).header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.IMAGE_PNG).content(PNG))
                .andExpect(status().isOk());

        String etag = mockMvc.perform(get("/api/v1/children/{id}/photo", childAId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(get("/api/v1/children/{id}/photo", childAId)
                        .header("Authorization", "Bearer " + tokenA)
                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string(HttpHeaders.ETAG, etag))
                .andExpect(header().exists(HttpHeaders.CACHE_CONTROL))
                .andExpect(content().bytes(new byte[0]));

        // 오래된 ETag 를 들고 오면 정상적으로 바이트를 받아야 한다.
        mockMvc.perform(get("/api/v1/children/{id}/photo", childAId)
                        .header("Authorization", "Bearer " + tokenA)
                        .header(HttpHeaders.IF_NONE_MATCH, "\"0\""))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG));
    }

    @Test
    void 사진_없으면_404() throws Exception {
        mockMvc.perform(get("/api/v1/children/{id}/photo", childAId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void 타_교사_아이_사진은_업로드할_수_없다_404() throws Exception {
        mockMvc.perform(put("/api/v1/children/{id}/photo", childBId).header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.IMAGE_PNG).content(PNG))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHILD_NOT_FOUND"));
    }

    @Test
    void 허용되지_않은_타입은_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(put("/api/v1/children/{id}/photo", childAId).header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 교사_본인_사진_업로드_조회() throws Exception {
        mockMvc.perform(put("/api/v1/teachers/me/photo").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.IMAGE_PNG).content(PNG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUpdatedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/teachers/me/photo").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));
    }
}
