package com.ondo.auth.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * 카카오 OAuth 인프라. RestClient raw HTTP 로 인가 코드를 검증한다(oauth2-client 스타터 미사용).
 * <p>
 * 흐름: 인가 코드 → {@code /oauth/token}(access_token 교환) → {@code /v2/user/me}(id·email·nickname 조회).
 * access_token 은 검증에만 쓰고 저장하지 않는다.
 * <p>
 * 에러 매핑(OpenAiClient 패턴): 카카오 4xx(무효/만료 코드·토큰) → {@link ErrorCode#AUTH_KAKAO_FAILED}(401),
 * 5xx·타임아웃·연결 실패 → {@link ErrorCode#KAKAO_UNAVAILABLE}(502). 재시도는 하지 않는다.
 * 생성자 주입(KakaoProperties)이 테스트 주입점 — MockWebServer URL 로 교체 가능.
 */
@Component
public class KakaoOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoOAuthClient.class);
    private static final long CONNECT_TIMEOUT_SECONDS = 3L;

    private final RestClient rest;
    private final String tokenUri;
    private final String userUri;
    private final String clientId;
    private final String clientSecret;

    public KakaoOAuthClient(KakaoProperties props) {
        long readTimeout = Math.max(1, props.timeoutSeconds());
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(Math.min(CONNECT_TIMEOUT_SECONDS, readTimeout)));
        factory.setReadTimeout(Duration.ofSeconds(readTimeout));
        this.rest = RestClient.builder().requestFactory(factory).build();
        this.tokenUri = props.tokenUri();
        this.userUri = props.userUri();
        this.clientId = props.clientId();
        this.clientSecret = props.clientSecret();

        if (clientId == null || clientId.isBlank()) {
            log.warn("카카오 client-id 미설정(ondo.kakao.client-id / KAKAO_CLIENT_ID) — 실제 로그인 시 401 로 실패한다");
        }
    }

    /**
     * 인가 코드를 검증하고 카카오 사용자 정보를 반환한다.
     *
     * @param code        프론트가 카카오에서 받은 인가 코드
     * @param redirectUri 인가 요청 때와 동일한 redirect_uri(카카오가 값 일치를 검증)
     */
    public KakaoUser exchange(String code, String redirectUri) {
        String accessToken = requestToken(code, redirectUri);
        return requestUser(accessToken);
    }

    private String requestToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        try {
            KakaoTokenResponse resp = rest.post().uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
            if (resp == null || resp.accessToken() == null || resp.accessToken().isBlank()) {
                log.warn("카카오 토큰 응답에 access_token 이 없음");
                throw new BusinessException(ErrorCode.AUTH_KAKAO_FAILED);
            }
            return resp.accessToken();
        } catch (HttpStatusCodeException e) {
            throw mapHttpError(e, "카카오 토큰 교환");
        } catch (ResourceAccessException e) {
            log.warn("카카오 토큰 교환 통신 실패(타임아웃/연결) — {}", e.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_UNAVAILABLE, e);
        } catch (RestClientException e) {
            log.warn("카카오 토큰 응답 처리 실패 — {}", e.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_UNAVAILABLE, e);
        }
    }

    private KakaoUser requestUser(String accessToken) {
        try {
            KakaoUserResponse resp = rest.get().uri(userUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);
            if (resp == null || resp.id() == null) {
                log.warn("카카오 사용자 응답에 id 가 없음");
                throw new BusinessException(ErrorCode.AUTH_KAKAO_FAILED);
            }
            String email = null;
            boolean emailVerified = false;
            String nickname = null;
            KakaoUserResponse.KakaoAccount account = resp.kakaoAccount();
            if (account != null) {
                email = blankToNull(account.email());
                emailVerified = Boolean.TRUE.equals(account.isEmailVerified());
                if (account.profile() != null) {
                    nickname = blankToNull(account.profile().nickname());
                }
            }
            if (nickname == null && resp.properties() != null) {
                nickname = blankToNull(resp.properties().nickname());
            }
            return new KakaoUser(String.valueOf(resp.id()), email, emailVerified, nickname);
        } catch (HttpStatusCodeException e) {
            throw mapHttpError(e, "카카오 사용자 조회");
        } catch (ResourceAccessException e) {
            log.warn("카카오 사용자 조회 통신 실패(타임아웃/연결) — {}", e.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_UNAVAILABLE, e);
        } catch (RestClientException e) {
            log.warn("카카오 사용자 응답 처리 실패 — {}", e.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_UNAVAILABLE, e);
        }
    }

    /** 카카오 4xx(무효/만료 코드·토큰)는 인증 실패(401), 5xx 는 서비스 장애(502)로 매핑. */
    private BusinessException mapHttpError(HttpStatusCodeException e, String what) {
        int status = e.getStatusCode().value();
        if (status >= 500) {
            log.warn("{} 5xx({}) — 원문: {}", what, status, e.getResponseBodyAsString());
            return new BusinessException(ErrorCode.KAKAO_UNAVAILABLE, e);
        }
        log.warn("{} 4xx({}) — 원문: {}", what, status, e.getResponseBodyAsString());
        return new BusinessException(ErrorCode.AUTH_KAKAO_FAILED, e);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /** 검증 결과. provider_id 는 카카오 회원번호(Long)를 문자열로 담는다. */
    public record KakaoUser(String providerId, String email, boolean emailVerified, String nickname) {
    }

    /** 카카오 토큰 응답(필요 필드만). */
    record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    /** 카카오 사용자 응답(필요 필드만). 미사용 필드는 무시. */
    record KakaoUserResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount,
            Properties properties
    ) {
        record KakaoAccount(
                String email,
                @JsonProperty("is_email_verified") Boolean isEmailVerified,
                Profile profile
        ) {
            record Profile(String nickname) {
            }
        }

        record Properties(String nickname) {
        }
    }
}
