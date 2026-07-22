package com.ondo.config;

import com.ondo.auth.kakao.KakaoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 카카오 OAuth 설정 활성화. KakaoOAuthClient 는 @Component 로 KakaoProperties 를 주입받는다.
 */
@Configuration
@EnableConfigurationProperties(KakaoProperties.class)
public class KakaoConfig {
}
