package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import com.ondo.auth.dto.ChangePasswordRequest;
import com.ondo.auth.dto.KakaoLoginRequest;
import com.ondo.auth.dto.LoginRequest;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.support.IntegrationTestSupport;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 카카오 로그인 통합테스트. 카카오 token/user API 를 MockWebServer 로 목킹해 I/O 매트릭스 전 행을 검증한다.
 * ondo.kakao.token-uri / user-uri 를 @DynamicPropertySource 로 MockWebServer URL 로 교체한다.
 */
@AutoConfigureMockMvc
@Transactional
class KakaoLoginIntegrationTest extends IntegrationTestSupport {

    private static final MockWebServer KAKAO = new MockWebServer();

    static {
        try {
            KAKAO.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void kakaoProps(DynamicPropertyRegistry registry) {
        String base = KAKAO.url("/").toString(); // http://localhost:<port>/
        registry.add("ondo.kakao.client-id", () -> "test-client-id");
        registry.add("ondo.kakao.client-secret", () -> "");
        registry.add("ondo.kakao.token-uri", () -> base + "oauth/token");
        registry.add("ondo.kakao.user-uri", () -> base + "v2/user/me");
        registry.add("ondo.kakao.timeout-seconds", () -> "2");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void drainQueue() throws Exception {
        // 테스트가 소비하지 않은 응답이 다음 테스트로 새지 않도록 큐를 비운다.
        while (KAKAO.takeRequest(1, TimeUnit.MILLISECONDS) != null) {
            // drain
        }
    }

    @AfterAll
    static void shutdownServer() throws Exception {
        KAKAO.shutdown();
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code)
                .setHeader("Content-Type", "application/json").setBody(body);
    }

    /** 카카오 token → user 순서로 성공 응답 2개를 큐에 넣는다. */
    private void enqueueKakao(long id, String emailJsonField) {
        KAKAO.enqueue(json(200, "{\"access_token\":\"kakao-access-token\",\"token_type\":\"bearer\"}"));
        KAKAO.enqueue(json(200, "{\"id\":" + id + ",\"kakao_account\":{"
                + emailJsonField
                + "\"profile\":{\"nickname\":\"카카오쌤\"}}}"));
    }

    private String kakaoLoginBody() {
        try {
            return objectMapper.writeValueAsString(
                    new KakaoLoginRequest("auth-code", "http://localhost:5273/oauth/kakao/callback"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void 신규_카카오_사용자면_계정을_생성하고_200_과_로그인응답을_반환한다() throws Exception {
        enqueueKakao(100001L, "\"email\":\"new@kakao.com\",\"is_email_verified\":true,");

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(kakaoLoginBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.teacher.email").value("new@kakao.com"))
                .andExpect(jsonPath("$.teacher.name").value("카카오쌤"));

        Teacher saved = teacherRepository.findByProviderAndProviderId("kakao", "100001").orElseThrow();
        assertThat(saved.getPasswordHash()).isNull();
        assertThat(saved.getEmail()).isEqualTo("new@kakao.com");
    }

    @Test
    void provider_id_가_일치하면_기존_카카오계정으로_재로그인한다() throws Exception {
        Teacher existing = teacherRepository.save(
                Teacher.createFromKakao("100002", "returning@kakao.com", "원래닉"));
        enqueueKakao(100002L, "\"email\":\"returning@kakao.com\",\"is_email_verified\":true,");

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(kakaoLoginBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacher.id").value(existing.getId()));

        // 새 계정이 생기지 않았다.
        assertThat(teacherRepository.count()).isEqualTo(1);
    }

    @Test
    void 동일_이메일의_기존_이메일계정이_있으면_자동_연동한다() throws Exception {
        Teacher emailAccount = teacherRepository.save(
                Teacher.create("teacher@ondo.dev", passwordEncoder.encode("password1234"), "이메일쌤"));
        enqueueKakao(100003L, "\"email\":\"teacher@ondo.dev\",\"is_email_verified\":true,");

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(kakaoLoginBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacher.id").value(emailAccount.getId()));

        Teacher linked = teacherRepository.findByProviderAndProviderId("kakao", "100003").orElseThrow();
        assertThat(linked.getId()).isEqualTo(emailAccount.getId());
        assertThat(linked.getPasswordHash()).isNotNull(); // 기존 비번 유지 → 이후 두 방식 모두 로그인 가능
        assertThat(teacherRepository.count()).isEqualTo(1);
    }

    @Test
    void 이메일_미검증이면_연동하지_않고_이메일없이_신규생성한다() throws Exception {
        Teacher emailAccount = teacherRepository.save(
                Teacher.create("owner@ondo.dev", passwordEncoder.encode("password1234"), "진짜주인"));
        // 카카오가 email 을 주지만 is_email_verified=false → 연동 금지, email NULL 신규 생성
        enqueueKakao(100004L, "\"email\":\"owner@ondo.dev\",\"is_email_verified\":false,");

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(kakaoLoginBody()))
                .andExpect(status().isOk());

        Teacher created = teacherRepository.findByProviderAndProviderId("kakao", "100004").orElseThrow();
        assertThat(created.getId()).isNotEqualTo(emailAccount.getId());
        assertThat(created.getEmail()).isNull(); // 미검증 이메일은 저장하지 않는다
        // 기존 계정은 그대로(연동되지 않음)
        assertThat(teacherRepository.findByEmail("owner@ondo.dev").orElseThrow().getProvider()).isNull();
    }

    @Test
    void 카카오가_이메일을_안주면_email없이_신규생성한다() throws Exception {
        enqueueKakao(100005L, ""); // kakao_account 에 email 필드 없음

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(kakaoLoginBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacher.name").value("카카오쌤"));

        Teacher created = teacherRepository.findByProviderAndProviderId("kakao", "100005").orElseThrow();
        assertThat(created.getEmail()).isNull();
    }

    @Test
    void 무효_인가코드면_401_AUTH_KAKAO_FAILED() throws Exception {
        KAKAO.enqueue(json(401, "{\"error\":\"invalid_grant\"}")); // token 교환 4xx

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(kakaoLoginBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_KAKAO_FAILED"));
    }

    @Test
    void 카카오_API_5xx면_502_KAKAO_UNAVAILABLE() throws Exception {
        KAKAO.enqueue(json(500, "{}")); // token 교환 5xx

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(kakaoLoginBody()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("KAKAO_UNAVAILABLE"));
    }

    @Test
    void 카카오_토큰_교환_타임아웃이면_502_KAKAO_UNAVAILABLE() throws Exception {
        // read timeout(2s)보다 긴 지연 → ResourceAccessException → KAKAO_UNAVAILABLE
        KAKAO.enqueue(json(200, "{\"access_token\":\"x\"}").setBodyDelay(4, TimeUnit.SECONDS));

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(kakaoLoginBody()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("KAKAO_UNAVAILABLE"));
    }

    @Test
    void 카카오_사용자조회_5xx면_502_KAKAO_UNAVAILABLE() throws Exception {
        KAKAO.enqueue(json(200, "{\"access_token\":\"kakao-access-token\"}")); // token 교환 성공
        KAKAO.enqueue(json(500, "{}"));                                         // user/me 5xx

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(kakaoLoginBody()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("KAKAO_UNAVAILABLE"));
    }

    @Test
    void 이미_다른_소셜계정에_연동된_이메일이면_덮어쓰지_않고_401() throws Exception {
        // 이메일 dup@kakao.com 이 이미 카카오 계정(providerId=500100)에 연동돼 있다.
        teacherRepository.save(Teacher.createFromKakao("500100", "dup@kakao.com", "선점쌤"));
        // 다른 카카오 회원번호(500999)가 같은 검증 이메일을 들고 로그인 시도 → 기존 행 덮어쓰기 거부.
        enqueueKakao(500999L, "\"email\":\"dup@kakao.com\",\"is_email_verified\":true,");

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(kakaoLoginBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_KAKAO_FAILED"));

        // 기존 계정의 provider_id 는 그대로여야 한다(탈취 방지).
        Teacher untouched = teacherRepository.findByEmail("dup@kakao.com").orElseThrow();
        assertThat(untouched.getProviderId()).isEqualTo("500100");
        // 새 계정도 만들어지지 않았다.
        assertThat(teacherRepository.findByProviderAndProviderId("kakao", "500999")).isEmpty();
    }

    @Test
    void 소셜전용_계정으로_비번로그인하면_401_AUTH_INVALID_CREDENTIALS_NPE없이() throws Exception {
        teacherRepository.save(Teacher.createFromKakao("100006", "social@kakao.com", "소셜쌤"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("social@kakao.com", "anything123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void 소셜전용_계정의_비번변경은_409_SOCIAL_ACCOUNT_NO_PASSWORD() throws Exception {
        Teacher social = teacherRepository.save(
                Teacher.createFromKakao("100007", "social2@kakao.com", "소셜쌤2"));
        String token = jwtProvider.createToken(social.getId(), social.getEmail());

        mockMvc.perform(post("/api/v1/teachers/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("current123", "newpassword123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOCIAL_ACCOUNT_NO_PASSWORD"));
    }
}
