package com.ondo.auth.throttle;

import com.ondo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 인증 시도 카운터(브루트포스 가드). 테이블 auth_attempt 는 Flyway V10 정본.
 * 복합 PK(scope, attempt_key) — 경로별로 카운터를 따로 센다.
 */
@Entity
@Table(name = "auth_attempt")
@IdClass(AuthAttempt.AttemptId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthAttempt extends BaseTimeEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthAttemptScope scope;

    @Id
    @Column(name = "attempt_key", nullable = false, length = 190)
    private String attemptKey;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    /** 이 시각까지 차단. NULL 이면 잠기지 않은 상태. */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    private AuthAttempt(AuthAttemptScope scope, String attemptKey, LocalDateTime now) {
        this.scope = scope;
        this.attemptKey = attemptKey;
        this.failCount = 0;
        this.windowStart = now;
    }

    public static AuthAttempt start(AuthAttemptScope scope, String attemptKey, LocalDateTime now) {
        return new AuthAttempt(scope, attemptKey, now);
    }

    /** 지금 차단 중인가. */
    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * 시도 1건 기록. 창이 지났으면 창을 새로 열어 처음부터 센다(행을 재사용하므로 무한히 늘지 않는다).
     * 임계치를 넘기는 순간 잠금 시각을 찍는다.
     */
    public void record(LocalDateTime now, Duration window, int maxAttempts, Duration lockFor) {
        if (lockedUntil != null && !now.isBefore(lockedUntil)) {
            // 잠금이 이미 풀렸다 — 다음 창을 깨끗하게 시작한다.
            resetWindow(now);
        } else if (now.isAfter(windowStart.plus(window))) {
            resetWindow(now);
        }
        this.failCount++;
        if (this.failCount >= maxAttempts) {
            this.lockedUntil = now.plus(lockFor);
        }
    }

    /** 성공했을 때 — 카운터와 잠금을 모두 지운다. */
    public void clear(LocalDateTime now) {
        resetWindow(now);
        this.lockedUntil = null;
    }

    private void resetWindow(LocalDateTime now) {
        this.windowStart = now;
        this.failCount = 0;
        this.lockedUntil = null;
    }

    /** 복합 PK 클래스. */
    public static class AttemptId implements Serializable {
        private AuthAttemptScope scope;
        private String attemptKey;

        public AttemptId() {
        }

        public AttemptId(AuthAttemptScope scope, String attemptKey) {
            this.scope = scope;
            this.attemptKey = attemptKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AttemptId that)) return false;
            return scope == that.scope && Objects.equals(attemptKey, that.attemptKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scope, attemptKey);
        }
    }
}
