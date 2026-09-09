package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import com.ondo.auth.dto.ChangePasswordRequest;
import com.ondo.auth.dto.LoginRequest;
import com.ondo.auth.dto.UpdateMyProfileRequest;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @PersistenceContext private EntityManager em;
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
    void 비밀번호_변경_성공시_200_과_새_토큰을_주고_새_비밀번호로_로그인된다() throws Exception {
        mockMvc.perform(post("/api/v1/teachers/me/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest(RAW_PASSWORD, "new-password-1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "new-password-1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    /** 변경과 동시에 옛 토큰이 죽으므로, 응답으로 받은 새 토큰은 곧바로 쓸 수 있어야 한다. */
    @Test
    void 비밀번호_변경_응답의_새_토큰은_바로_사용할_수_있다() throws Exception {
        String body = mockMvc.perform(post("/api/v1/teachers/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest(RAW_PASSWORD, "new-password-1234"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String reissued = objectMapper.readTree(body).get("accessToken").asString();

        mockMvc.perform(get("/api/v1/classrooms").header("Authorization", "Bearer " + reissued))
                .andExpect(status().isOk());
    }

    /**
     * 비밀번호 변경 시각보다 먼저 발급된 토큰은 거절된다 — 계정을 도난당했을 때 공격자의 토큰이
     * 만료(1h)까지 살아 있던 구멍을 막는 규칙이다.
     *
     * <p>JWT 의 iat 는 초 단위라 같은 초에 발급된 토큰은 통과하므로(재발급 토큰을 살리기 위한
     * 설계), 테스트가 실행 속도에 좌우되지 않도록 변경 시각을 명시적으로 뒤로 밀어 검증한다.
     */
    @Test
    void 비밀번호_변경보다_먼저_발급된_토큰은_401_AUTH_TOKEN_REVOKED() throws Exception {
        em.createQuery("UPDATE Teacher t SET t.passwordChangedAt = :at WHERE t.id = :id")
                .setParameter("at", LocalDateTime.now(ZoneOffset.UTC).plusSeconds(5))
                .setParameter("id", teacherId)
                .executeUpdate();

        mockMvc.perform(get("/api/v1/classrooms").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_REVOKED"));
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
