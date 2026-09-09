package com.ondo.auth.throttle;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 시도 제한 정책 프로퍼티 활성화(ondo.throttle.*). */
@Configuration
@EnableConfigurationProperties(AuthThrottleProperties.class)
public class AuthThrottleConfig {
}
