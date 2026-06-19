package com.ondo.ai.prompt;

import com.ondo.memo.domain.CurriculumArea;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 일지 분석 응답의 JSON 스키마(ai-integration-spec §3). OpenAI structured outputs(strict)용으로
 * AiRequest.responseSchema(Map) 에 그대로 전달한다. strict 요건: 모든 property required + additionalProperties:false.
 */
public final class JournalAnalysisSchema {

    private JournalAnalysisSchema() {
    }

    /** 누리 5영역 enum 이름 목록(스키마 키·enum 값과 단일 출처). */
    private static final List<String> AREA_NAMES =
            Arrays.stream(CurriculumArea.values()).map(Enum::name).toList();

    /** {summary, areas{5영역 전부}, memoClassifications[{index, area}]} 스키마. */
    public static Map<String, Object> schema() {
        return object(
                List.of("summary", "areas", "memoClassifications"),
                Map.of(
                        "summary", Map.of("type", "string"),
                        "areas", areasSchema(),
                        "memoClassifications", memoClassificationsSchema()));
    }

    private static Map<String, Object> areasSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        for (String area : AREA_NAMES) {
            props.put(area, Map.of("type", "string"));
        }
        return object(AREA_NAMES, props);
    }

    private static Map<String, Object> memoClassificationsSchema() {
        Map<String, Object> item = object(
                List.of("index", "area"),
                Map.of(
                        "index", Map.of("type", "integer"),
                        "area", Map.of("type", "string", "enum", AREA_NAMES)));
        return Map.of("type", "array", "items", item);
    }

    private static Map<String, Object> object(List<String> required, Map<String, Object> properties) {
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("type", "object");
        obj.put("additionalProperties", false);
        obj.put("required", required);
        obj.put("properties", properties);
        return obj;
    }
}
