package com.ondo.auth.dto;

import com.ondo.common.validation.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청. password 72바이트 상한 — 초과 문자열은 BCrypt matches 가 예외를 던져 500 이 되므로 400 에서 차단
 * (가입 {@link RegisterRequest}·비밀번호 변경 {@link ChangePasswordRequest} 과 동일한 이유).
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank @MaxBytes(72) String password
) {
}
