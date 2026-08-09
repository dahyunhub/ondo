package com.ondo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sentry(에러 모니터링) 설정(ondo.sentry.*).
 * <p>
 * {@code dsn} 이 비어 있으면 Sentry 를 초기화하지 않는다({@link SentryConfig}) → 테스트·로컬 dev 는
 * 완전 무동작. 운영에서만 {@code SENTRY_DSN} 을 주입해 활성화한다. 개인정보는 전송하지 않는다(NFR-1):
 * {@code sendDefaultPii=false}, 캡처 시 아이 실명·본문 없이 식별자·요청 경로만 태그로 남긴다.
 */
@ConfigurationProperties(prefix = "ondo.sentry")
public class SentryProperties {

    /** Sentry DSN. 비어 있으면 Sentry 비활성(기본). */
    private String dsn = "";

    /** 환경 태그(dev/prod 등). 기본은 활성 프로파일을 따른다. */
    private String environment = "";

    /** 릴리스 식별자(예: ondo@0.0.1). 비어 있으면 미설정. */
    private String release = "";

    /** 성능 트레이스 샘플링 비율(0.0=에러만). 초기엔 0 권장. */
    private double tracesSampleRate = 0.0;

    public String getDsn() {
        return dsn;
    }

    public void setDsn(String dsn) {
        this.dsn = dsn;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public double getTracesSampleRate() {
        return tracesSampleRate;
    }

    public void setTracesSampleRate(double tracesSampleRate) {
        this.tracesSampleRate = tracesSampleRate;
    }
}
