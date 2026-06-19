package com.ondo.ai.prompt;

import com.ondo.ai.dto.MemoPromptInput;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 일지 분석 프롬프트 구성(ai/prompt). system 은 고정 도메인 템플릿(prompts/journal.txt, prompt caching 유리),
 * user 는 요청마다 달라지는 비식별화 메모 묶음을 [n] 인덱스로 렌더한다. 정본: docs/specs/ai-integration-spec.md §1·§6.
 * 인프라(OpenAiClient)를 모른다(의존성 방향 고정).
 */
@Component
public class PromptTemplateLoader {

    private static final String JOURNAL_TEMPLATE = "prompts/journal.txt";

    /** 고정 system 프롬프트(누리 5영역·놀이 제안·또래 상호작용·교사 발문·출력 형식 강제). 시작 시 1회 로드. */
    private final String journalSystemPrompt;

    public PromptTemplateLoader() {
        this.journalSystemPrompt = load(JOURNAL_TEMPLATE);
    }

    /** 일지 분석 system 프롬프트(고정 템플릿). */
    public String journalSystemPrompt() {
        return journalSystemPrompt;
    }

    /**
     * 비식별화된 메모 묶음을 user 프롬프트로 렌더. 각 메모는 `[n]` 인덱스 + 놀이/상호작용/태도/메모 3+1 항목으로 제시(FR-2).
     * 빈 항목은 생략한다.
     */
    public String renderMemos(List<MemoPromptInput> memos) {
        // 계약: 분석 경로는 비어있지 않은 메모 묶음으로만 진입한다(3.5 오케스트레이션이 보장; 빈 묶음은 REPORT/journal 진입 전 차단).
        // 개별 메모는 저장 시 MEMO_EMPTY 로 최소 1개 항목이 보장됨. 여기서는 빈 리스트를 프로그래밍 오류로 거절한다.
        if (memos == null || memos.isEmpty()) {
            throw new IllegalArgumentException("렌더할 메모가 없습니다(분석 경로 전제: 비어있지 않은 메모 묶음).");
        }
        StringBuilder sb = new StringBuilder("아래는 오늘 하루 동안 기록한 관찰 메모입니다. 각 항목을 분석해 보육일지를 작성해 주세요.\n");
        for (MemoPromptInput m : memos) {
            sb.append('\n').append('[').append(m.index()).append(']');
            appendField(sb, " 놀이: ", m.playActivity());
            appendField(sb, " / 상호작용: ", m.interaction());
            appendField(sb, " / 태도: ", m.attitude());
            appendField(sb, " / 메모: ", m.content());
        }
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(value.strip());
        }
    }

    private String load(String classpathLocation) {
        try {
            return new ClassPathResource(classpathLocation).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            // 설정 오류 — 조용한 빈 프롬프트로 진행하지 않는다.
            throw new UncheckedIOException("프롬프트 템플릿을 찾을 수 없습니다: " + classpathLocation, e);
        }
    }
}
