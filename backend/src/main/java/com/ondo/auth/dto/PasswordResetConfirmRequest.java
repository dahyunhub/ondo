package com.ondo.auth.dto;

import com.ondo.common.validation.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 확정(API [24]).
 * 새 비밀번호 규칙은 가입과 동일하게 맞춘다 — 여기만 느슨하면 우회 경로가 된다.
 */
public record PasswordResetConfirmRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8) @MaxBytes(72) String newPassword
) {
}
