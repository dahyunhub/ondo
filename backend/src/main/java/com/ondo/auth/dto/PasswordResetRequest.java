package com.ondo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 비밀번호 재설정 요청(API [23]). 응답은 가입 여부와 무관하게 항상 동일하다. */
public record PasswordResetRequest(
        @NotBlank @Email String email
) {
}
