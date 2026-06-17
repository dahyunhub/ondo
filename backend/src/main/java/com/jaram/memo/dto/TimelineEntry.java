package com.jaram.memo.dto;

import com.jaram.memo.domain.CurriculumArea;
import com.jaram.memo.domain.Memo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 타임라인 항목(API [8]). curriculumArea 는 미분류 시 null.
 * createdAt 은 UTC Instant(ISO-8601 'Z') — 표시 시각·날짜는 클라이언트가 로컬 타임존으로 변환.
 */
public record TimelineEntry(
        Long id,
        LocalDate date,
        String content,
        CurriculumArea curriculumArea,
        Instant createdAt
) {

    public static TimelineEntry from(Memo m) {
        return new TimelineEntry(m.getId(), m.getCreatedAt().toLocalDate(), displayText(m),
                m.getCurriculumArea(), m.getCreatedAt().toInstant(ZoneOffset.UTC));
    }

    /** 표시 텍스트: 자유 입력(content)이 있으면 그대로, 없으면 입력된 3항목을 합성. */
    private static String displayText(Memo m) {
        if (m.getContent() != null && !m.getContent().isBlank()) {
            return m.getContent();
        }
        return Stream.of(m.getPlayActivity(), m.getInteraction(), m.getAttitude())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" · "));
    }
}
