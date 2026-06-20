package com.ondo.journal;

import com.ondo.auth.TeacherRepository;
import com.ondo.auth.domain.Teacher;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.classroom.ClassroomRepository;
import com.ondo.classroom.domain.Classroom;
import com.ondo.journal.domain.DailyJournal;
import com.ondo.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [13] GET / [14] PUT /api/v1/journals/{id} 통합 테스트(Story 3.6). AI 미사용 — 일지를 직접 시드.
 * REQUIRES_NEW 가 없어 @Transactional 도 쓸 수 있으나, 3.5 패턴과 동일하게 테스트별 고유 teacher 로 격리한다.
 */
@AutoConfigureMockMvc
class JournalReviewIntegrationTest extends IntegrationTestSupport {

    private static final String SEED_CONTENT = """
            {"summary":"초안 요약","PHYSICAL_HEALTH":"신체","COMMUNICATION":"의사소통","SOCIAL":"사회","ART":"예술","NATURE":"자연"}""";

    @Autowired private MockMvc mockMvc;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private DailyJournalRepository dailyJournalRepository;
    @Autowired private JwtProvider jwtProvider;

    private String tokenA;
    private String tokenB; // 타 교사
    private Long journalId; // 교사 A 소유

    @BeforeEach
    void setUp() {
        Teacher a = teacherRepository.save(Teacher.create("a+" + System.nanoTime() + "@ondo.dev", "h", "교사A"));
        Teacher b = teacherRepository.save(Teacher.create("b+" + System.nanoTime() + "@ondo.dev", "h", "교사B"));
        Long classroomAId = classroomRepository.save(
                Classroom.create(a.getId(), "햇살반", 2026, LocalDate.of(2026, 3, 2))).getId();
        journalId = dailyJournalRepository.save(DailyJournal.createDraft(
                a.getId(), classroomAId, LocalDate.of(2026, 6, 15), SEED_CONTENT, LocalDateTime.now(ZoneOffset.UTC))).getId();
        tokenA = jwtProvider.createToken(a.getId(), a.getEmail());
        tokenB = jwtProvider.createToken(b.getId(), b.getEmail());
    }

    @Test
    void 본인_일지를_조회하면_200_content_객체() throws Exception {
        mockMvc.perform(get("/api/v1/journals/" + journalId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(journalId))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.content.summary").value("초안 요약"))
                .andExpect(jsonPath("$.content.SOCIAL").value("사회"));
    }

    @Test
    void 없는_일지_조회는_404() throws Exception {
        mockMvc.perform(get("/api/v1/journals/" + (journalId + 99999)).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOURNAL_NOT_FOUND"));
    }

    @Test
    void 타_교사_일지_조회는_404() throws Exception {
        mockMvc.perform(get("/api/v1/journals/" + journalId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOURNAL_NOT_FOUND"));
    }

    @Test
    void 수정_확정하면_200이고_재조회시_유지된다() throws Exception {
        String body = """
                {"content":{"summary":"교사가 수정","PHYSICAL_HEALTH":"x","COMMUNICATION":"x","SOCIAL":"x","ART":"x","NATURE":"x"},
                 "status":"CONFIRMED"}""";
        mockMvc.perform(put("/api/v1/journals/" + journalId).header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.content.summary").value("교사가 수정"));

        // 재조회 시 확정·수정 content 유지
        mockMvc.perform(get("/api/v1/journals/" + journalId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.content.summary").value("교사가 수정"));
    }

    @Test
    void content_누락_수정은_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(put("/api/v1/journals/" + journalId).header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void content가_빈객체면_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(put("/api/v1/journals/" + journalId).header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":{},\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void content에_영역이_하나라도_누락이면_400_VALIDATION_FAILED() throws Exception {
        // NATURE 누락
        String body = """
                {"content":{"summary":"s","PHYSICAL_HEALTH":"x","COMMUNICATION":"x","SOCIAL":"x","ART":"x"},
                 "status":"CONFIRMED"}""";
        mockMvc.perform(put("/api/v1/journals/" + journalId).header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 타_교사_일지_수정은_404() throws Exception {
        String body = """
                {"content":{"summary":"침범","PHYSICAL_HEALTH":"x","COMMUNICATION":"x","SOCIAL":"x","ART":"x","NATURE":"x"},
                 "status":"CONFIRMED"}""";
        mockMvc.perform(put("/api/v1/journals/" + journalId).header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOURNAL_NOT_FOUND"));
    }
}
