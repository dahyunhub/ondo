package com.ondo.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 카카오 로그인 요청. 프론트가 카카오에서 받은 인가 코드와, 인가 요청 때 쓴 redirect_uri 를 그대로 전달한다.
 * (카카오 토큰 교환 시 redirect_uri 일치 검증이 필요하므로 프론트가 값을 함께 보낸다.)
 * 응답은 이메일 로그인과 동일한 {@link LoginResponse}.
 */
public record KakaoLoginRequest(
        @NotBlank String code,
        @NotBlank String redirectUri
) {
}
