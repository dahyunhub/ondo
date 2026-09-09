-- V10__create_auth_attempt.sql : 인증 시도 카운터(브루트포스 가드)
-- 정본: backend/src/main/java/com/ondo/auth/throttle/AuthAttempt.java
--
-- 로그인·비밀번호 확인이 무제한으로 시도를 받아 온라인 오라클이 되던 것을 막는다.
-- Cloud Run 은 인스턴스가 늘거나 잠들기 때문에 메모리 카운터로는 보증이 되지 않아 DB 에 둔다
-- (인스턴스가 여러 개여도 같은 카운터를 보고, 재배포·콜드스타트에도 유지된다).
--
-- 고정 창(window) 방식: window_start 부터 window 길이 안의 시도를 세고, 임계치를 넘으면
-- locked_until 까지 막는다. 창이 지나면 첫 시도에서 창을 새로 연다(행 재사용 — 무한 증가 없음).

CREATE TABLE auth_attempt (
    scope        VARCHAR(20)  NOT NULL,  -- LOGIN / PASSWORD_CONFIRM / RESET_REQUEST
    attempt_key  VARCHAR(190) NOT NULL,  -- 정규화된 이메일 또는 teacherId
    fail_count   INT          NOT NULL,
    window_start DATETIME(6)  NOT NULL,
    locked_until DATETIME(6)  NULL,      -- 이 시각까지 차단(NULL = 잠기지 않음)
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (scope, attempt_key),
    INDEX idx_auth_attempt_window (window_start)  -- 만료분 정리용
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
