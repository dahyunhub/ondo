package com.ondo.journal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 반의 일지 목록 항목(api-spec [12b], FR-3). 홈·일지 허브의 "지금까지 만든 일지" 리스트용.
 * summary 는 저장된 평탄화 content 의 요약 문단(목록 미리보기).
 */
public record JournalSummaryResponse(
        Long id,
        LocalDate journalDate,
        String status,
        String summary,
        LocalDateTime analyzedAt
) {
}
