package com.ondo.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 인앱 피드백 전송 요청.
 *
 * @param message  의견 본문(필수)
 * @param category 분류(버그/제안/기타 등, 선택)
 * @param page     보낸 화면(라우트명, 선택) — 맥락 파악용
 */
public record FeedbackRequest(
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 30) String category,
        @Size(max = 200) String page
) {
}
