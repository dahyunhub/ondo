package com.ondo.ai.prompt;

import com.ondo.ai.dto.JournalAnalysisResult;
import com.ondo.common.exception.AiAnalysisException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.memo.domain.CurriculumArea;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** JournalResultParser 단위 테스트 — 복원된 JSON → JournalAnalysisResult. */
class JournalResultParserTest {

    private final JournalResultParser parser = new JournalResultParser();

    @Test
    void 정상_JSON을_결과로_매핑한다() {
        String json = """
                {
                  "summary": "오늘은 블록과 그림 놀이가 활발했어요",
                  "areas": {
                    "PHYSICAL_HEALTH": "대근육 활동",
                    "COMMUNICATION": "친구와 대화",
                    "SOCIAL": "협력 놀이",
                    "ART": "그림 표현",
                    "NATURE": "관찰 탐구"
                  },
                  "memoClassifications": [
                    {"index": 1, "area": "SOCIAL"},
                    {"index": 2, "area": "ART"}
                  ]
                }
                """;

        JournalAnalysisResult r = parser.parse(json);

        assertThat(r.summary()).contains("블록");
        assertThat(r.areas()).containsKeys(CurriculumArea.values());
        assertThat(r.memoClassifications()).hasSize(2);
        assertThat(r.memoClassifications().get(0).area()).isEqualTo(CurriculumArea.SOCIAL);
    }

    @Test
    void 깨진_JSON은_AI_OUTPUT_INVALID() {
        assertThatThrownBy(() -> parser.parse("{ this is not json"))
                .isInstanceOf(AiAnalysisException.class)
                .extracting(e -> ((AiAnalysisException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_OUTPUT_INVALID);
    }

    @Test
    void 알수없는_영역enum은_AI_OUTPUT_INVALID() {
        String json = """
                {"summary":"s","areas":{"UNKNOWN_AREA":"x"},"memoClassifications":[]}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AiAnalysisException.class)
                .extracting(e -> ((AiAnalysisException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_OUTPUT_INVALID);
    }
}
