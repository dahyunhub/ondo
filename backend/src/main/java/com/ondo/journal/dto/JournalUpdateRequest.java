package com.ondo.journal.dto;

import com.ondo.journal.domain.JournalStatus;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 일지 수정·확정 요청(api-spec [14], FR-5). AI 호출 없음 — content 와 status 만 저장한다.
 * content 는 평탄화 5영역 객체({summary, PHYSICAL_HEALTH, …, NATURE}).
 */
public record JournalUpdateRequest(
        @NotNull Map<String, String> content,
        @NotNull JournalStatus status
) {
}
