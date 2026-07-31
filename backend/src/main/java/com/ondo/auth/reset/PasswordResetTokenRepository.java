package com.ondo.auth.reset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /** 확정 단계 — 메일로 받은 원문을 해시해서 찾는다. 원문으로는 조회할 수 없다. */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** 재발급·비밀번호 변경 시 무효화 대상(아직 안 쓴 토큰). */
    List<PasswordResetToken> findByTeacherIdAndUsedAtIsNull(Long teacherId);
}
