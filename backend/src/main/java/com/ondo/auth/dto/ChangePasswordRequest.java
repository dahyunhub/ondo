package com.ondo.auth.dto;

import com.ondo.common.validation.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 요청. 현재 비밀번호 검증을 통과해야 변경된다(불일치 → AUTH_INVALID_CREDENTIALS).
 * 새 비밀번호 정책은 가입({@link RegisterRequest})과 동일 — BCrypt 72바이트 한계는 바이트로 검증.
 * currentPassword 도 72바이트 상한 — 초과 문자열은 BCrypt matches 가 예외를 던져 500 이 되므로 400 에서 차단.
 */
public record ChangePasswordRequest(
        @NotBlank @MaxBytes(72) String currentPassword,
        @NotBlank @Size(min = 8) @MaxBytes(72) String newPassword
) {
}
