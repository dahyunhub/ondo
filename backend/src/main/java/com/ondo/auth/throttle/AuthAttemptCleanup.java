package com.ondo.auth.throttle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 오래된 시도 카운터 정리.
 *
 * <p>정상 사용자의 행은 창이 지나면 재사용되므로 늘지 않는다. 늘어나는 경로는 공격자가 아무
 * 이메일이나 넣어 새 행을 만드는 경우뿐이라, 하루가 지난 비잠금 행을 지워 되돌린다.
 *
 * <p>Cloud Run 은 min-instances=0 이라 인스턴스가 자고 있으면 이 잡이 그날 돌지 않을 수 있다.
 * 행이 아주 작아 며칠 밀려도 문제가 없으므로 best-effort 로 둔다.
 */
@Component
public class AuthAttemptCleanup {

    private static final Logger log = LoggerFactory.getLogger(AuthAttemptCleanup.class);

    private final AuthAttemptRepository repository;

    public AuthAttemptCleanup(AuthAttemptRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "${ondo.throttle.cleanup-cron:0 30 4 * * *}", zone = "Asia/Seoul")
    @Transactional
    public void purgeStale() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int deleted = repository.deleteStale(now.minusDays(1), now);
        if (deleted > 0) {
            log.info("인증 시도 카운터 정리: {}건 삭제", deleted);
        }
    }
}
