package com.ondo.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondo.ai.dto.JournalAnalysisResult;
import com.ondo.common.exception.AiAnalysisException;
import com.ondo.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 복원이 끝난 AI 원문(JSON 문자열) → {@link JournalAnalysisResult}. 정본: ai-integration-spec §3.
 * structured outputs(strict)면 형태가 보장되지만, 파싱 불가 시 방어적으로 AI_OUTPUT_INVALID 로 매핑한다.
 * 인프라(OpenAiClient)를 모른다(의존성 방향 고정).
 */
@Component
public class JournalResultParser {

    // 고정 스키마 파싱 전용 — 컨테이너의 ObjectMapper 빈에 의존하지 않는다(앱에 주입 가능한 빈이 없을 수 있음).
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 복원된 JSON 문자열을 파싱한다. 파싱 실패 시 AI_OUTPUT_INVALID(메모 보존 고지 포함). */
    public JournalAnalysisResult parse(String restoredJson) {
        try {
            return objectMapper.readValue(restoredJson, JournalAnalysisResult.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            // 원문/실명 로깅 금지 — 예외 메시지에도 포함하지 않는다.
            throw new AiAnalysisException(ErrorCode.AI_OUTPUT_INVALID);
        }
    }
}
