package com.ondo.ai.prompt;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** JournalAnalysisSchema 단위 테스트 — OpenAI strict 요건 및 5영역 형태. */
class JournalAnalysisSchemaTest {

    @Test
    @SuppressWarnings("unchecked")
    void 스키마는_5영역과_strict요건을_갖춘다() {
        Map<String, Object> schema = JournalAnalysisSchema.schema();

        assertThat(schema).containsEntry("type", "object").containsEntry("additionalProperties", false);
        assertThat((List<String>) schema.get("required"))
                .containsExactly("summary", "areas", "memoClassifications");

        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        Map<String, Object> areas = (Map<String, Object>) props.get("areas");

        assertThat(areas).containsEntry("additionalProperties", false);
        assertThat((List<String>) areas.get("required"))
                .containsExactlyInAnyOrder("PHYSICAL_HEALTH", "COMMUNICATION", "SOCIAL", "ART", "NATURE");

        Map<String, Object> memoItems = (Map<String, Object>)
                ((Map<String, Object>) props.get("memoClassifications")).get("items");
        Map<String, Object> areaProp = (Map<String, Object>)
                ((Map<String, Object>) memoItems.get("properties")).get("area");
        assertThat((List<String>) areaProp.get("enum"))
                .containsExactlyInAnyOrder("PHYSICAL_HEALTH", "COMMUNICATION", "SOCIAL", "ART", "NATURE");
    }
}
