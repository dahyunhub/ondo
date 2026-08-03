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
    private static final String REPORT_TEMPLATE = "prompts/child-report.txt";

    /** 고정 system 프롬프트(누리 5영역·놀이 제안·또래 상호작용·교사 발문·출력 형식 강제). 시작 시 1회 로드. */
    private final String journalSystemPrompt;
    /** 개인평가 고정 system 프롬프트(관찰된 영역 subset·출력 형식 강제). 시작 시 1회 로드. */
    private final String reportSystemPrompt;

    public PromptTemplateLoader() {
        this.journalSystemPrompt = load(JOURNAL_TEMPLATE);
        this.reportSystemPrompt = load(REPORT_TEMPLATE);
    }

    /** 일지 분석 system 프롬프트(고정 템플릿). */
    public String journalSystemPrompt() {
        return journalSystemPrompt;
    }

    /** 개인평가 system 프롬프트(고정 템플릿). */
    public String reportSystemPrompt() {
        return reportSystemPrompt;
    }

    /**
     * 한 아이의 기간 관찰 메모 묶음을 평가용 user 프롬프트로 렌더. 항목 제시 방식은 일지와 동일하나
     * 도입부가 '기간 개인 관찰 평가'용이다(평가는 메모 영역 분류 없음). 빈 리스트는 프로그래밍 오류로 거절.
     * subjectToken 은 이 평가의 '대상 아동' 가명 토큰([[CHILD_n]]) — 모델이 주어를 추정하다 엉뚱한 아이 토큰을
     * 찍는 것을 막는다(오지칭 방지). 메모에 다른 아이가 등장해도 평가 주어는 이 토큰으로 고정된다.
     */
    public String renderReportMemos(String subjectToken, List<MemoPromptInput> memos) {
        if (memos == null || memos.isEmpty()) {
            throw new IllegalArgumentException("렌더할 메모가 없습니다(평가 경로 전제: 비어있지 않은 메모 묶음).");
        }
        StringBuilder sb = new StringBuilder();
        if (subjectToken != null && !subjectToken.isBlank()) {
            sb.append("이 개인 관찰 평가의 대상 아동은 ").append(subjectToken.strip())
              .append(" 입니다. 평가 전체에서 대상 아동을 가리킬 때는 반드시 이 토큰만 사용하고, ")
              .append("다른 아이 토큰이나 새 토큰으로 바꾸지 마세요. 메모에 등장하는 다른 아이는 각자의 토큰으로 두세요.\n\n");
        }
        sb.append("아래는 한 아이에 대해 일정 기간 동안 기록한 관찰 메모입니다. 이를 바탕으로 개인 관찰 평가를 작성해 주세요.\n");
        for (MemoPromptInput m : memos) {
            sb.append('\n').append('[').append(m.index()).append(']');
            appendField(sb, " 놀이: ", m.playActivity());
            appendField(sb, " / 상호작용: ", m.interaction());
            appendField(sb, " / 태도: ", m.attitude());
            appendField(sb, " / 메모: ", m.content());
        }
        return sb.toString();
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
        StringBuilder sb = new StringBuilder("아래는 오늘 하루 동안 기록한 관찰 메모입니다. 각 항목을 분석해 일지를 작성해 주세요.\n");
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
