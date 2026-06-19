package com.ondo.ai.prompt;

import com.ondo.ai.dto.MemoPromptInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** PromptTemplateLoader 단위 테스트(스프링 컨텍스트 불필요 — classpath 템플릿 로드). */
class PromptTemplateLoaderTest {

    private final PromptTemplateLoader loader = new PromptTemplateLoader();

    @Test
    void 시스템_프롬프트는_도메인_강제요구를_담는다() {
        String sys = loader.journalSystemPrompt();

        assertThat(sys)
                .contains("PHYSICAL_HEALTH", "COMMUNICATION", "SOCIAL", "ART", "NATURE") // 5영역 코드
                .contains("또래 상호작용")
                .contains("발문")     // 교사 지원/발문
                .contains("놀이");    // 다양한 놀이 제안
    }

    @Test
    void 메모를_인덱스와_3항목으로_렌더하고_빈항목은_생략한다() {
        String user = loader.renderMemos(List.of(
                new MemoPromptInput(1, "본문A", "블록놀이", "친구와 협력", "적극적"),
                new MemoPromptInput(2, null, "그림그리기", null, null)));

        assertThat(user).contains("[1]").contains("[2]");
        assertThat(user)
                .contains("놀이: 블록놀이")
                .contains("상호작용: 친구와 협력")
                .contains("태도: 적극적")
                .contains("메모: 본문A")
                .contains("놀이: 그림그리기");
        assertThat(user).doesNotContain("null"); // 빈 항목 라벨이 새지 않음
    }

    @Test
    void 빈_메모_묶음은_계약위반으로_거절한다() {
        assertThatThrownBy(() -> loader.renderMemos(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
