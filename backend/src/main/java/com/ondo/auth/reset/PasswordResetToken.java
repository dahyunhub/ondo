package com.ondo.auth.reset;

import com.ondo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 토큰(spec-password-reset).
 *
 * <p><b>원문 토큰을 저장하지 않는다.</b> 난수는 메일에만 실리고 여기엔 SHA-256 해시만 남는다.
 * DB 가 통째로 유출돼도 남의 비밀번호를 바꿀 수 없어야 하기 때문이다.
 *
 * <p>유효 조건은 셋 다 만족해야 한다 — 만료 전, 미사용. 판정을 엔티티 안에 두어
 * 서비스마다 조건이 갈리는 일을 막는다.
 */
@Entity
@Table(name = "password_reset_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends BaseTimeEntity {

    /** 발급 후 유효 시간(분). 짧을수록 안전하지만 메일 확인 시간은 줘야 한다. */
    public static final int VALID_MINUTES = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /** 원문이 아니라 SHA-256 hex(64자). */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 사용 시각. NULL 이면 아직 안 씀. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    private PasswordResetToken(Long teacherId, String tokenHash, LocalDateTime expiresAt) {
        this.teacherId = teacherId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static PasswordResetToken issue(Long teacherId, String tokenHash, LocalDateTime now) {
        return new PasswordResetToken(teacherId, tokenHash, now.plusMinutes(VALID_MINUTES));
    }

    /** 지금 쓸 수 있는 토큰인가. 만료·재사용을 한 곳에서 판정한다. */
    public boolean isUsable(LocalDateTime now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    /** 1회용 소진. 되돌리지 않는다. */
    public void markUsed(LocalDateTime now) {
        this.usedAt = now;
    }

    /** 새 토큰 발급·비밀번호 변경 시 남은 토큰을 무효화할 때 쓴다. */
    public void invalidate(LocalDateTime now) {
        if (this.usedAt == null) {
            this.usedAt = now;
        }
    }
}
