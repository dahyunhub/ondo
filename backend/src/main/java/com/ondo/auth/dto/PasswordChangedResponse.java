package com.ondo.auth.dto;

/**
 * 비밀번호 변경 성공 응답.
 *
 * <p>변경과 동시에 이전에 발급된 토큰이 전부 무효화되므로(Teacher.changePassword),
 * 지금 요청을 보낸 기기가 그대로 로그아웃되지 않도록 새 토큰을 함께 내려준다.
 * 다른 기기·브라우저의 세션만 끊긴다.
 */
public record PasswordChangedResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
