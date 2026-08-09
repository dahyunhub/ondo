package com.ondo.feedback;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 피드백 설정 바인딩(`ondo.feedback.*`).
 */
@Configuration
@EnableConfigurationProperties(FeedbackProperties.class)
public class FeedbackConfig {
}
