package com.ondo.auth.throttle;

import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 인증 시도 제한(브루트포스 가드).
 *
 * <p><b>트랜잭션 주의.</b> 기록 메서드는 모두 {@code REQUIRES_NEW} 다. 로그인 실패는 호출부에서
 * 예외로 끝나 바깥 트랜잭션이 롤백되는데, 같은 트랜잭션에서 세면 <b>실패 횟수까지 함께 사라져</b>
 * 가드가 통째로 무력해진다. 별도 빈으로 두는 이유도 같다 — 자기호출(self-invocation)로는
 * 프록시를 타지 않아 전파 설정이 먹지 않는다.
 */
@Service
public class AuthThrottleService {

    private static final Logger log = LoggerFactory.getLogger(AuthThrottleService.class);

    private final AuthAttemptRepository repository;
    private final AuthThrottleProperties properties;

    public AuthThrottleService(AuthAttemptRepository repository, AuthThrottleProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** 이메일을 키로 쓸 때의 정규화 — 대소문자를 바꿔 카운터를 우회하지 못하게 한다. */
    public static String key(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        return normalized.length() > 190 ? normalized.substring(0, 190) : normalized;
    }

    /** 차단 중이면 429 로 끊는다(로그인·비밀번호 확인). */
    @Transactional(readOnly = true)
    public void assertNotLocked(AuthAttemptScope scope, String attemptKey) {
        if (isLocked(scope, attemptKey)) {
            throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS);
        }
    }

    /** 차단 여부만 확인(응답을 바꾸면 안 되는 경로 — 재설정 메일 요청에서 쓴다). */
    @Transactional(readOnly = true)
    public boolean isLocked(AuthAttemptScope scope, String attemptKey) {
        return repository.findByScopeAndAttemptKey(scope, attemptKey)
                .map(a -> a.isLocked(LocalDateTime.now(ZoneOffset.UTC)))
                .orElse(false);
    }

    /** 시도 1건 기록. 임계치에 도달하면 이 호출에서 잠긴다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuthAttemptScope scope, String attemptKey) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        AuthThrottleProperties.Policy policy = properties.of(scope);
        AuthAttempt attempt = repository.findByScopeAndAttemptKey(scope, attemptKey)
                .orElseGet(() -> repository.save(AuthAttempt.start(scope, attemptKey, now)));

        attempt.record(now, policy.window(), policy.maxAttempts(), policy.lockFor());
        repository.save(attempt);

        if (attempt.isLocked(now)) {
            // 키(이메일)는 남기지 않는다 — 로그가 계정 목록이 되지 않도록 스코프만 기록한다.
            log.warn("인증 시도 제한 발동: scope={}, lockedUntil={}", scope, attempt.getLockedUntil());
        }
    }

    /** 성공했을 때 카운터를 지운다. 정상 사용자가 오타 몇 번 뒤 성공하면 흔적이 남지 않는다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clear(AuthAttemptScope scope, String attemptKey) {
        repository.findByScopeAndAttemptKey(scope, attemptKey)
                .ifPresent(a -> {
                    a.clear(LocalDateTime.now(ZoneOffset.UTC));
                    repository.save(a);
                });
    }
}
