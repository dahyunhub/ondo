package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import com.ondo.auth.reset.PasswordResetToken;
import com.ondo.auth.reset.PasswordResetTokenRepository;
import com.ondo.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 비밀번호 재설정(spec-password-reset).
 *
 * <p>가장 중요한 단언은 "가입/미가입/카카오 세 갈래의 응답이 완전히 같다"이다 —
 * 여기가 갈리면 이 엔드포인트가 계정 조회기가 된다.
 * 메일은 비-prod 라 LoggingMailSender 가 동작하므로 SMTP 없이 전 구간이 돈다.
 */
@AutoConfigureMockMvc
@Transactional
class PasswordResetIntegrationTest extends IntegrationTestSupport {

    private static final String RAW_PASSWORD = "oldpassword1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;
    @PersistenceContext private EntityManager em;

    private String email;
    private Long teacherId;

    @BeforeEach
    void setUp() {
        email = "reset+" + System.nanoTime() + "@ondo.dev";
        teacherId = teacherRepository.save(
                Teacher.create(email, passwordEncoder.encode(RAW_PASSWORD), "교사A")).getId();
    }

    // ---------- 헬퍼 ----------

    private MvcResult requestReset(String targetEmail) throws Exception {
        em.flush();
        em.clear();
        return mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + targetEmail + "\"}"))
                .andReturn();
    }

    /**
     * 발급된 원문 토큰은 서비스 밖으로 나오지 않으므로(메일에만 실림) 테스트에서는
     * 알려진 원문의 해시를 직접 심어 확정 단계를 검증한다. 원문 미저장 원칙 자체는 별도 테스트에서 확인.
     */
    private String plantToken(String rawToken, LocalDateTime expiresAt, LocalDateTime usedAt) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                        INSERT INTO password_reset_token
                            (teacher_id, token_hash, expires_at, used_at, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)""",
                teacherId, sha256Hex(rawToken), Timestamp.valueOf(expiresAt),
                usedAt == null ? null : Timestamp.valueOf(usedAt),
                Timestamp.valueOf(now), Timestamp.valueOf(now));
        return rawToken;
    }

    private static String sha256Hex(String raw) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of()
                    .formatHex(digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void confirm(String token, String newPassword, int expectedStatus) throws Exception {
        em.flush();
        em.clear();
        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private void login(String password, int expectedStatus) throws Exception {
        em.flush();
        em.clear();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    // ---------- 계정 열거 방지 ----------

    @Test
    void 가입된_이메일과_미가입_이메일의_응답이_완전히_같다() throws Exception {
        MvcResult registered = requestReset(email);
        MvcResult unknown = requestReset("nobody+" + System.nanoTime() + "@ondo.dev");

        assertThat(registered.getResponse().getStatus())
                .isEqualTo(unknown.getResponse().getStatus());
        assertThat(registered.getResponse().getContentAsString())
                .isEqualTo(unknown.getResponse().getContentAsString());
    }

    @Test
    void 미가입_이메일은_토큰을_만들지_않는다() throws Exception {
        requestReset("nobody+" + System.nanoTime() + "@ondo.dev");

        assertThat(tokenRepository.findAll()).isEmpty();
    }

    @Test
    void 카카오_전용_계정은_토큰없이_같은_응답을_준다() throws Exception {
        String kakaoEmail = "kakao+" + System.nanoTime() + "@ondo.dev";
        Long kakaoId = teacherRepository.save(
                Teacher.createFromKakao("kko" + System.nanoTime(), kakaoEmail, "카카오교사")).getId();

        MvcResult result = requestReset(kakaoEmail);

        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(tokenRepository.findByTeacherIdAndUsedAtIsNull(kakaoId)).isEmpty();
    }

    @Test
    void 이메일_형식이_아니면_400이다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    // ---------- 토큰 취급 ----------

    @Test
    void 발급된_토큰의_원문은_DB에_남지_않는다() throws Exception {
        requestReset(email);

        em.flush();
        em.clear();
        List<PasswordResetToken> tokens = tokenRepository.findByTeacherIdAndUsedAtIsNull(teacherId);
        assertThat(tokens).hasSize(1);
        // 저장된 값은 64자 hex 여야 한다(원문은 Base64URL 43자라 형태부터 다르다)
        assertThat(tokens.get(0).getTokenHash()).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void 재요청하면_이전_토큰이_즉시_무효가_된다() throws Exception {
        requestReset(email);
        requestReset(email);

        em.flush();
        em.clear();
        // 살아있는 토큰은 마지막 1개뿐
        assertThat(tokenRepository.findByTeacherIdAndUsedAtIsNull(teacherId)).hasSize(1);
        assertThat(tokenRepository.findAll()).hasSize(2);
    }

    // ---------- 확정 ----------

    @Test
    void 유효한_토큰으로_비밀번호를_바꾸면_새_비밀번호로_로그인된다() throws Exception {
        String token = plantToken("valid-token-1", LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10), null);

        confirm(token, "newpassword1234", 204);

        login("newpassword1234", 200);
        login(RAW_PASSWORD, 401);
    }

    @Test
    void 이미_사용한_토큰은_거부된다() throws Exception {
        String token = plantToken("used-token-1", LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10), null);
        confirm(token, "newpassword1234", 204);

        confirm(token, "anotherpassword1234", 400);
    }

    @Test
    void 만료된_토큰은_거부된다() throws Exception {
        String token = plantToken("expired-token-1", LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1), null);

        confirm(token, "newpassword1234", 400);
        login(RAW_PASSWORD, 200); // 비밀번호는 그대로
    }

    @Test
    void 위조된_토큰은_거부된다() throws Exception {
        confirm("this-token-never-existed", "newpassword1234", 400);
        login(RAW_PASSWORD, 200);
    }

    @Test
    void 만료_재사용_위조는_같은_에러코드를_준다() throws Exception {
        String expired = plantToken("exp-2", LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1), null);
        String used = plantToken("used-2", LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10),
                LocalDateTime.now(ZoneOffset.UTC));

        for (String token : List.of(expired, used, "forged-2")) {
            em.flush();
            em.clear();
            mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"" + token + "\",\"newPassword\":\"newpassword1234\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));
        }
    }

    @Test
    void 비밀번호가_짧으면_400이고_토큰은_살아있다() throws Exception {
        String token = plantToken("short-pw-token", LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10), null);

        confirm(token, "short", 400);
        // 토큰을 태우지 않았으므로 곧바로 다시 시도할 수 있어야 한다
        confirm(token, "newpassword1234", 204);
        login("newpassword1234", 200);
    }

    @Test
    void 비밀번호를_바꾸면_남은_토큰도_함께_폐기된다() throws Exception {
        String first = plantToken("multi-1", LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10), null);
        String second = plantToken("multi-2", LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10), null);

        confirm(first, "newpassword1234", 204);

        confirm(second, "yetanotherpw1234", 400);
    }

    @Test
    void 미인증_상태에서도_재설정_엔드포인트에_접근할_수_있다() throws Exception {
        // 비밀번호를 잊은 사람은 토큰이 없다 — permitAll 이 아니면 기능 자체가 성립하지 않는다
        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isNoContent());
    }
}
