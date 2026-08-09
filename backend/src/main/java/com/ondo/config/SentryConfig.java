package com.ondo.config;

import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Sentry 수동 초기화.
 * <p>
 * Spring Boot 4 autoconfig 스타터에 의존하지 않고 코어 SDK 를 직접 {@code Sentry.init} 한다
 * (Spring 버전 독립·부팅 시점 예측 가능). DSN 이 비어 있으면 초기화를 건너뛰므로 테스트·dev 에
 * 영향이 없고, 이 경우 {@code Sentry.captureException} 은 no-op 이 된다.
 * <p>
 * 실제 예외 캡처는 {@code GlobalExceptionHandler} 가 5xx 경로에서 수행한다. 이 앱은 모든 예외를
 * 전역 핸들러로 흡수하므로 SDK 의 자동 캡처(미처리 예외 기준)에 기대지 않는다.
 */
@Configuration
@EnableConfigurationProperties(SentryProperties.class)
public class SentryConfig {

    private static final Logger log = LoggerFactory.getLogger(SentryConfig.class);

    private final SentryProperties props;
    private final Environment environment;

    public SentryConfig(SentryProperties props, Environment environment) {
        this.props = props;
        this.environment = environment;
    }

    @PostConstruct
    void init() {
        if (!StringUtils.hasText(props.getDsn())) {
            log.info("Sentry 비활성(ondo.sentry.dsn 미설정) — 에러 모니터링 미전송");
            return;
        }
        Sentry.init(options -> {
            options.setDsn(props.getDsn());
            options.setEnvironment(resolveEnvironment());
            if (StringUtils.hasText(props.getRelease())) {
                options.setRelease(props.getRelease());
            }
            options.setTracesSampleRate(props.getTracesSampleRate());
            // NFR-1: IP·요청 헤더 등 PII 미전송. 캡처 시 식별자·경로만 태그로 첨부한다.
            options.setSendDefaultPii(false);
        });
        log.info("Sentry 활성(environment={})", resolveEnvironment());
    }

    private String resolveEnvironment() {
        if (StringUtils.hasText(props.getEnvironment())) {
            return props.getEnvironment();
        }
        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0 ? profiles[0] : "default";
    }
}
