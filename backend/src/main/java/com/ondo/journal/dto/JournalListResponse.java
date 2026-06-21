package com.ondo.journal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 반·날짜 일지 조회 응답 + 재분석 안내(api-spec [12], FR-6).
 * reanalysisNeeded: 분석 이후 추가된 메모 존재 여부. newMemoIds: 그 메모 id 들.
 */
public record JournalListResponse(
        Long id,
        Long classroomId,
        LocalDate journalDate,
        String status,
        Map<String, String> content,
        LocalDateTime analyzedAt,
        boolean reanalysisNeeded,
        List<Long> newMemoIds
) {
}
