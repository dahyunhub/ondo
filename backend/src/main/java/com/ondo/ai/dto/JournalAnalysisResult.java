package com.ondo.ai.dto;

import com.ondo.memo.domain.CurriculumArea;

import java.util.List;
import java.util.Map;

/**
 * 일지 분석 AI 응답의 파싱 타입(ai-integration-spec §3). 복원이 끝난 JSON 문자열에서 매핑된다.
 *
 * @param summary             일지 요약 서술
 * @param areas               누리 5영역별 서술(키는 CurriculumArea)
 * @param memoClassifications 입력 메모 인덱스 → 영역 분류(FR-2)
 */
public record JournalAnalysisResult(
        String summary,
        Map<CurriculumArea, String> areas,
        List<MemoArea> memoClassifications
) {
    /**
     * @param index 입력 user 프롬프트의 1-based 메모 인덱스
     * @param area  분류된 누리 영역
     */
    public record MemoArea(int index, CurriculumArea area) {
    }
}
