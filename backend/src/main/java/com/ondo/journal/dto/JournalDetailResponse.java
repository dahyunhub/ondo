package com.ondo.journal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 일지 단건 조회·수정 응답(api-spec [13]/[14]). content 는 평탄화 5영역 객체.
 * 재분석 안내(reanalysisNeeded·newMemoIds)는 Story 3.7 범위라 포함하지 않는다.
 */
public record JournalDetailResponse(
        Long id,
        Long classroomId,
        LocalDate journalDate,
        String status,
        Map<String, String> content,
        LocalDateTime analyzedAt
) {
}
