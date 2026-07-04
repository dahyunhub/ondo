package com.ondo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 내 프로필 수정 요청. 이메일은 로그인 ID 라 수정 불가 — 이름만 받는다.
 * 이름 상한은 DB 컬럼(teacher.name VARCHAR(100)) — 초과 시 409 로 새는 것을 400 에서 차단.
 */
public record UpdateMyProfileRequest(
        @NotBlank @Size(max = 100) String name
) {
}
