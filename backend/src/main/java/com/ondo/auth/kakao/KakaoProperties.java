package com.ondo.auth.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 카카오 OAuth 설정. application.yml 의 ondo.kakao.* 바인딩.
 * client-id/secret 은 운영 시 env 로만 주입(코드·저장소 노출 금지).
 *
 * @param clientId       카카오 REST API 키(인가 코드 → 토큰 교환에 사용)
 * @param clientSecret   카카오 client_secret(콘솔에서 사용함으로 설정한 경우에만; 미사용 시 빈 값)
 * @param tokenUri       토큰 교환 엔드포인트(테스트에서 MockWebServer URL 로 교체하는 주입점)
 * @param userUri        사용자 정보 엔드포인트(동일 주입점)
 * @param timeoutSeconds 서버 하드 타임아웃(읽기)
 */
@ConfigurationProperties(prefix = "ondo.kakao")
public record KakaoProperties(
        String clientId,
        String clientSecret,
        String tokenUri,
        String userUri,
        int timeoutSeconds
) {
}
