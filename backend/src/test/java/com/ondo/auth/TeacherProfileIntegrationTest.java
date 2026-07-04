package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import com.ondo.auth.dto.ChangePasswordRequest;
import com.ondo.auth.dto.LoginRequest;
import com.ondo.auth.dto.UpdateMyProfileRequest;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class TeacherProfileIntegrationTest extends IntegrationTestSupport {

    private static final String EMAIL = "teacher@ondo.dev";
    private static final String RAW_PASSWORD = "password1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private ObjectMapper objectMapper;

    private Long teacherId;
    private String token;

    @BeforeEach
    void setUp() {
        Teacher teacher = teacherRepository.save(
                Teacher.create(EMAIL, passwordEncoder.encode(RAW_PASSWORD), "민지"));
        teacherId = teacher.getId();
        token = jwtProvider.createToken(teacher.getId(), teacher.getEmail());
    }

    // ---------- 이름 수정 ----------

    @Test
    void 이름_수정시_200_과_갱신된_프로필을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/teachers/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMyProfileRequest("수진"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(teacherId))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value("수진"))
                .andExpect(jsonPath("$.photoUpdatedAt").hasJsonPath());

        assertThat(teacherRepository.findById(teacherId).orElseThrow().getName()).isEqualTo("수진");
    }

    @Test
    void 이름이_100자를_넘으면_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(patch("/api/v1/teachers/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMyProfileRequest("가".repeat(101)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void 이름이_공백이면_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(patch("/api/v1/teachers/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMyProfileRequest("  "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    // ---------- 비밀번호 변경 ----------

    @Test
    void 비밀번호_변경_성공시_204_이후_새_비밀번호로_로그인된다() throws Exception {
        mockMvc.perform(post("/api/v1/teachers/me/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest(RAW_PASSWORD, "new-password-1234"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "new-password-1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void 현재_비밀번호가_틀리면_401_AUTH_INVALID_CREDENTIALS_이고_변경되지_않는다() throws Exception {
        mockMvc.perform(post("/api/v1/teachers/me/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("wrong-password", "new-password-1234"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));

        String hash = teacherRepository.findById(teacherId).orElseThrow().getPasswordHash();
        assertThat(passwordEncoder.matches(RAW_PASSWORD, hash)).isTrue();
    }

    @Test
    void 새_비밀번호가_8자_미만이면_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(post("/api/v1/teachers/me/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest(RAW_PASSWORD, "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 새_비밀번호가_72바이트를_넘으면_400_VALIDATION_FAILED() throws Exception {
        String tooLong = "한".repeat(25); // 25자 × 3바이트 = 75바이트 > 72
        mockMvc.perform(post("/api/v1/teachers/me/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest(RAW_PASSWORD, tooLong))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 현재_비밀번호가_72바이트를_넘으면_400_VALIDATION_FAILED() throws Exception {
        // BCrypt matches 가 72바이트 초과에서 예외를 던지므로 500 이 아닌 400 에서 차단돼야 한다
        mockMvc.perform(post("/api/v1/teachers/me/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("한".repeat(25), "new-password-1234"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 토큰_없이_호출하면_401_AUTH_UNAUTHENTICATED() throws Exception {
        mockMvc.perform(patch("/api/v1/teachers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMyProfileRequest("수진"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHENTICATED"));

        mockMvc.perform(post("/api/v1/teachers/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest(RAW_PASSWORD, "new-password-1234"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHENTICATED"));
    }
}
