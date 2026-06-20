package com.ondo.journal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondo.ai.dto.JournalAnalysisResult;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.memo.domain.CurriculumArea;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 결과(summary + areas{5}) → 저장·응답용 평탄화 content({summary, PHYSICAL_HEALTH, …, NATURE}).
 * 정본: ai-integration-spec §3(저장형 평탄화). ObjectMapper 는 자체 인스턴스(주입 가능 빈 없음 — Story 3.3 교훈).
 */
@Component
public class JournalContentSerializer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 평탄화 content 맵(순서 고정: summary → 5영역). */
    public Map<String, String> toFlatContent(JournalAnalysisResult result) {
        Map<String, String> flat = new LinkedHashMap<>();
        flat.put("summary", result.summary());
        for (CurriculumArea area : CurriculumArea.values()) {
            flat.put(area.name(), result.areas().get(area));
        }
        return flat;
    }

    /** 평탄화 content 를 daily_journal.content 저장용 JSON 문자열로 직렬화. */
    public String toJson(Map<String, String> flatContent) {
        try {
            return objectMapper.writeValueAsString(flatContent);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 수정·확정(PUT) 시 저장형 계약 검증(ai-integration-spec §3): summary + 5영역이 모두 존재·non-blank 여야 한다.
     * analyze 경로의 OutputValidator 와 동일한 구조 보장을 PUT 에도 적용해 깨진 일지 저장을 막는다.
     */
    public void validateFlatContent(Map<String, String> content) {
        boolean valid = content != null
                && isNonBlank(content.get("summary"))
                && Arrays.stream(CurriculumArea.values()).allMatch(area -> isNonBlank(content.get(area.name())));
        if (!valid) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "일지 내용은 요약과 5개 영역을 모두 채워야 해요.");
        }
    }

    private boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }

    /** 저장된 content JSON 문자열 → 응답용 평탄화 맵(조회·수정 응답). */
    public Map<String, String> toMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
