package com.ondo.auth.throttle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthAttemptRepository extends JpaRepository<AuthAttempt, AuthAttempt.AttemptId> {

    Optional<AuthAttempt> findByScopeAndAttemptKey(AuthAttemptScope scope, String attemptKey);

    /**
     * 오래된 카운터 정리. 잠겨 있지 않고 창도 한참 지난 행만 지운다.
     * 공격자가 아무 이메일이나 넣어 행을 늘리는 것을 되돌리기 위한 것으로, 정상 사용자의 행은
     * 어차피 창이 지나면 재사용되므로 지워져도 무해하다.
     */
    @Modifying
    @Query("DELETE FROM AuthAttempt a WHERE a.windowStart < :before "
            + "AND (a.lockedUntil IS NULL OR a.lockedUntil < :now)")
    int deleteStale(@Param("before") LocalDateTime before, @Param("now") LocalDateTime now);
}
