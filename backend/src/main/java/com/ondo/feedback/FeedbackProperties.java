package com.ondo.feedback;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 피드백 설정(`ondo.feedback.*`).
 *
 * @param to 피드백 알림을 받을 관리자 메일 주소. 비어 있으면 DB 에만 적재하고 메일은 보내지 않는다.
 */
@ConfigurationProperties(prefix = "ondo.feedback")
public record FeedbackProperties(
        String to
) {
}
