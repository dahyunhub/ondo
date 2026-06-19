package com.ondo.ai.prompt;

import com.ondo.ai.dto.JournalAnalysisResult;
import com.ondo.common.exception.AiAnalysisException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.memo.domain.CurriculumArea;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 일지 분석 결과의 '내용 완전성' 검증(ai-integration-spec §3). structured outputs(strict)가 보장하는 형태 위에
 * 누락·빈값·미커버를 추가 검사한다. 위반 시 AI_OUTPUT_INVALID — 1회 재요청 루프는 오케스트레이션(Story 3.5)이 소유한다.
 * 인프라(OpenAiClient)를 모른다(의존성 방향 고정).
 */
@Component
public class OutputValidator {

    /**
     * @param result               파싱된 일지 분석 결과
     * @param expectedMemoIndices  입력 user 프롬프트에 제시한 메모 인덱스 전체(빠짐없이 커버되어야 함)
     * @throws AiAnalysisException AI_OUTPUT_INVALID — summary 공백 / 5영역 누락·공백 / 메모 인덱스 미커버 시
     */
    public void validate(JournalAnalysisResult result, Collection<Integer> expectedMemoIndices) {
        if (result == null || result.summary() == null || result.summary().isBlank()) {
            throw invalid();
        }
        validateAreas(result);
        validateMemoCoverage(result, expectedMemoIndices);
    }

    private void validateAreas(JournalAnalysisResult result) {
        if (result.areas() == null) {
            throw invalid();
        }
        for (CurriculumArea area : CurriculumArea.values()) {
            String text = result.areas().get(area);
            if (text == null || text.isBlank()) {
                throw invalid();
            }
        }
    }

    private void validateMemoCoverage(JournalAnalysisResult result, Collection<Integer> expectedMemoIndices) {
        if (result.memoClassifications() == null) {
            throw invalid();
        }
        Set<Integer> covered = new HashSet<>();
        for (JournalAnalysisResult.MemoArea ma : result.memoClassifications()) {
            if (ma.area() == null) { // enum 이라 파싱 시점에 유효하지만 방어적으로 확인
                throw invalid();
            }
            if (!covered.add(ma.index())) { // 같은 인덱스 중복 분류
                throw invalid();
            }
        }
        // 입력 인덱스와 '정확히' 일치해야 한다 — 초과(999·0 등)·누락 모두 거절(이후 index→memo.id 매핑 안전)
        if (!covered.equals(new HashSet<>(expectedMemoIndices))) {
            throw invalid();
        }
    }

    private AiAnalysisException invalid() {
        return new AiAnalysisException(ErrorCode.AI_OUTPUT_INVALID);
    }
}
