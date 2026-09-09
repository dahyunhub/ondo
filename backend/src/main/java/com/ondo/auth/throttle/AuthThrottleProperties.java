package com.ondo.auth.throttle;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 경로별 시도 제한 정책. 운영에서 env 로 조절할 수 있게 프로퍼티로 뺀다.
 *
 * @param login           로그인 — 정상 사용자가 오타로 몇 번 틀리는 것을 감안해 여유를 둔다
 * @param passwordConfirm 현재 비밀번호 확인 — 이미 유효 토큰을 쥔 공격자가 두드리는 곳이라 더 빡빡하게
 * @param resetRequest    재설정 메일 요청 — 메일함 폭탄·SMTP 할당량 소진 방지(횟수 자체를 센다)
 */
@ConfigurationProperties(prefix = "ondo.throttle")
public record AuthThrottleProperties(Policy login, Policy passwordConfirm, Policy resetRequest) {

    public AuthThrottleProperties {
        login = login != null ? login : new Policy(10, Duration.ofMinutes(15), Duration.ofMinutes(15));
        passwordConfirm = passwordConfirm != null ? passwordConfirm
                : new Policy(5, Duration.ofMinutes(15), Duration.ofMinutes(15));
        resetRequest = resetRequest != null ? resetRequest
                : new Policy(5, Duration.ofHours(1), Duration.ofHours(1));
    }

    /**
     * @param maxAttempts 창 안에서 허용할 시도 횟수(이 횟수에 도달하면 잠근다)
     * @param window      시도를 세는 창의 길이
     * @param lockFor     임계치를 넘겼을 때 막아 둘 시간
     */
    public record Policy(int maxAttempts, Duration window, Duration lockFor) {

        /** 일부 값만 override 해도 나머지가 null 로 남지 않도록 기본값을 채운다. */
        public Policy {
            if (maxAttempts <= 0) {
                maxAttempts = 10;
            }
            if (window == null) {
                window = Duration.ofMinutes(15);
            }
            if (lockFor == null) {
                lockFor = Duration.ofMinutes(15);
            }
        }
    }

    public Policy of(AuthAttemptScope scope) {
        return switch (scope) {
            case LOGIN -> login;
            case PASSWORD_CONFIRM -> passwordConfirm;
            case RESET_REQUEST -> resetRequest;
        };
    }
}
