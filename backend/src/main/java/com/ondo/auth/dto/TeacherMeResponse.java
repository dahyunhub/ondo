package com.ondo.auth.dto;

import java.time.LocalDateTime;

/**
 * 내 프로필 응답. 필드 구성은 {@link LoginResponse.TeacherSummary}와 동일 —
 * 프론트 localStorage(ondo.teacher) 저장 포맷과 일치시킨다.
 */
public record TeacherMeResponse(Long id, String email, String name, LocalDateTime photoUpdatedAt) {
}
