package com.ondo.auth.dto;

import com.ondo.common.validation.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청(FR-12, NFR-6). 비밀번호는 평문으로 받아 서버에서 BCrypt 해시한다.
 * 응답은 로그인과 동일한 {@link LoginResponse}(가입 즉시 자동 로그인).
 * 비밀번호 상한은 BCrypt 72바이트 한계 보호 — {@code @Size}(문자 수)가 아닌 실제 바이트로 검증한다.
 * 이름 상한은 DB 컬럼(teacher.name VARCHAR(100)) — 초과 시 409 로 새는 것을 400 에서 차단.
 */
public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) @MaxBytes(72) String password
) {
}
