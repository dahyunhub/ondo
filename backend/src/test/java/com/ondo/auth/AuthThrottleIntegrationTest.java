package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import com.ondo.auth.dto.ChangePasswordRequest;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 브루트포스 가드 통합테스트. 임계치는 테스트에서 작게 내려 잡는다(운영 기본값으로 세면 요청 수만 늘고
 * 검증되는 것은 같다).
 *
 * <p><b>이 클래스는 @Transactional 을 쓰지 않는다.</b> 가드는 "실패는 REQUIRES_NEW 로 커밋하고,
 * 다음 요청이 그것을 읽는다"는 구조라 <b>트랜잭션 경계를 넘는 가시성</b> 자체가 검증 대상이다.
 * 테스트를 한 트랜잭션으로 감싸면 MySQL 기본 격리수준(REPEATABLE READ)에서 그 트랜잭션의 스냅샷이
 * 나중에 커밋된 카운터를 보지 못해, 실서비스에서는 잘 도는 가드가 테스트에서만 안 걸린다
 * (실제로 그렇게 6건이 실패했다). 실서비스는 요청마다 트랜잭션이 따로라 해당 없음.
 * 대신 만든 데이터를 직접 지운다.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "ondo.throttle.login.max-attempts=3",
        "ondo.throttle.password-confirm.max-attempts=2",
        "ondo.throttle.reset-request.max-attempts=2"
})
class AuthThrottleIntegrationTest extends IntegrationTestSupport {

    private static final String EMAIL = "throttle@ondo.dev";
    private static final String PASSWORD = "password1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long teacherId;
    private String token;

    @BeforeEach
    void setUp() {
        cleanUp();
        Teacher teacher = teacherRepository.save(
                Teacher.create(EMAIL, passwordEncoder.encode(PASSWORD), "김선생"));
        teacherId = teacher.getId();
        token = jwtProvider.createToken(teacher.getId(), teacher.getEmail());
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    /** 롤백이 없으므로 이 클래스가 만든 것만 직접 지운다(다른 테스트 데이터는 건드리지 않는다). */
    private void cleanUp() {
        jdbcTemplate.update(
                "DELETE FROM password_reset_token WHERE teacher_id IN (SELECT id FROM teacher WHERE email = ?)",
                EMAIL);
        jdbcTemplate.update("DELETE FROM teacher WHERE email = ?", EMAIL);
    }

    // ---------- 로그인 ----------

    @Test
    void 로그인_실패가_임계치에_닿으면_429_로_막힌다() throws Exception {
        for (int i = 0; i < 3; i++) {
            login(EMAIL, "wrong-password").andExpect(status().isUnauthorized());
        }

        login(EMAIL, "wrong-password").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
    }

    /** 잠긴 뒤에는 비밀번호가 맞아도 통과시키지 않는다 — 이게 없으면 가드가 아니라 지연일 뿐이다. */
    @Test
    void 잠긴_뒤에는_올바른_비밀번호도_429() throws Exception {
        for (int i = 0; i < 3; i++) {
            login(EMAIL, "wrong-password");
        }

        login(EMAIL, PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    void 임계치_전에_성공하면_카운터가_지워진다() throws Exception {
        login(EMAIL, "wrong-password").andExpect(status().isUnauthorized());
        login(EMAIL, "wrong-password").andExpect(status().isUnauthorized());

        login(EMAIL, PASSWORD).andExpect(status().isOk());

        // 카운터가 남아 있었다면 아래 두 번째 실패에서 잠겼을 것이다.
        login(EMAIL, "wrong-password").andExpect(status().isUnauthorized());
        login(EMAIL, "wrong-password").andExpect(status().isUnauthorized());
    }

    /** 미가입 이메일도 똑같이 세고 똑같이 429 를 준다 — 429 가 계정 존재 신호가 되면 안 된다. */
    @Test
    void 미가입_이메일도_같은_방식으로_429() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("nobody@ondo.dev", "whatever1234").andExpect(status().isUnauthorized());
        }

        login("nobody@ondo.dev", "whatever1234").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
    }

    /**
     * 대소문자를 바꿔 카운터를 우회할 수 없어야 한다. MySQL 기본 콜레이션이 대소문자를 구분하지 않아
     * 같은 계정으로 로그인되므로, 키를 정규화하지 않으면 대소문자만 바꿔 시도 횟수를 늘릴 수 있다.
     * (앞뒤 공백은 @Email 검증이 400 으로 먼저 막아 이 경로까지 오지 않는다 — key() 의 trim 은 방어용.)
     */
    @Test
    void 이메일_대소문자를_바꿔도_같은_카운터다() throws Exception {
        login(EMAIL, "wrong-password").andExpect(status().isUnauthorized());
        login(EMAIL.toUpperCase(), "wrong-password").andExpect(status().isUnauthorized());
        login("Throttle@Ondo.dev", "wrong-password").andExpect(status().isUnauthorized());

        login(EMAIL, "wrong-password").andExpect(status().isTooManyRequests());
    }

    // ---------- 현재 비밀번호 확인 ----------

    @Test
    void 비밀번호_확인_실패가_임계치에_닿으면_429() throws Exception {
        for (int i = 0; i < 2; i++) {
            changePassword("wrong-password").andExpect(status().isUnauthorized());
        }

        changePassword("wrong-password").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
        // 잠긴 동안에는 올바른 현재 비밀번호로도 진행되지 않는다.
        changePassword(PASSWORD).andExpect(status().isTooManyRequests());
    }

    // ---------- 재설정 메일 요청 ----------

    /**
     * 재설정 요청은 제한에 걸려도 <b>응답이 달라지면 안 된다</b> — 429 를 주면 그 자체가
     * "가입된 이메일"이라는 신호가 되어 계정 열거 방지가 무너진다. 발송(토큰 생성)만 멈춘다.
     */
    @Test
    void 재설정_요청은_제한돼도_응답이_같고_발송만_멈춘다() throws Exception {
        resetRequest().andExpect(status().isNoContent());
        resetRequest().andExpect(status().isNoContent());
        long issued = issuedTokens();
        assertThat(issued).isEqualTo(2);

        resetRequest().andExpect(status().isNoContent());
        resetRequest().andExpect(status().isNoContent());

        assertThat(issuedTokens()).isEqualTo(issued);
    }

    // ---------- helpers ----------

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions changePassword(String current) throws Exception {
        return mockMvc.perform(post("/api/v1/teachers/me/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ChangePasswordRequest(current, "brand-new-password-1"))));
    }

    private long issuedTokens() {
        Long n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM password_reset_token WHERE teacher_id = ?", Long.class, teacherId);
        return n == null ? 0 : n;
    }

    private org.springframework.test.web.servlet.ResultActions resetRequest() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + EMAIL + "\"}"));
    }
}
