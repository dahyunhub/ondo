package com.ondo.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 메일·링크 설정. 둘 다 환경변수라 도메인을 사거나 SMTP 제공자를 바꿔도 코드는 그대로다.
 *
 * @param from    발신자 주소
 * @param baseUrl 재설정 링크가 가리킬 프론트 주소(끝 슬래시 없이). 개발은 localhost, 배포는 실제 도메인
 */
@ConfigurationProperties(prefix = "ondo.mail")
public record MailProperties(
        String from,
        String baseUrl
) {

    /** 끝 슬래시가 붙어 있어도 링크가 `//` 로 깨지지 않게 정리한다. */
    public String normalizedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
