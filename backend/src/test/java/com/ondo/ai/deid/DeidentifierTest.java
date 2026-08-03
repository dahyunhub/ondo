package com.ondo.ai.deid;

import com.ondo.common.exception.DeidentificationException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Deidentifier·RestorationContext 순수 단위 테스트(스프링 컨텍스트·DB·HTTP 불필요).
 * 정본: docs/specs/ai-integration-spec.md §5 비식별화 프로토콜.
 */
class DeidentifierTest {

    private final Deidentifier deidentifier = new Deidentifier();

    @Test
    void 실명을_토큰으로_치환하고_다시_복원한다() {
        RestorationContext ctx = deidentifier.newContext(List.of("김민준", "박서윤"));
        String text = "김민준이가 박서윤과 블록놀이를 했어요";

        String deid = deidentifier.deidentify(text, ctx);

        assertThat(deid).doesNotContain("김민준", "박서윤");
        assertThat(deid).contains("[[CHILD_1]]", "[[CHILD_2]]");
        assertThat(ctx.restore(deid)).isEqualTo(text); // AI 가 토큰을 그대로 돌려줬다고 가정
    }

    @Test
    void 부분문자열_이름도_긴이름부터_정확히_치환된다() {
        // "김민"이 "김민준"의 부분문자열 — 긴 이름부터 치환해야 오치환이 없다.
        RestorationContext ctx = deidentifier.newContext(List.of("김민", "김민준"));
        String text = "김민준과 김민이 함께 놀았어요";

        String deid = deidentifier.deidentify(text, ctx);

        assertThat(deid).doesNotContain("김민준", "김민");
        assertThat(ctx.restore(deid)).isEqualTo(text);
    }

    @Test
    void 같은_이름이_여러번_나와도_동일_토큰으로_안정_치환된다() {
        RestorationContext ctx = deidentifier.newContext(List.of("이도윤"));
        String text = "이도윤 놀이 / 이도윤 상호작용 / 이도윤 태도";

        String deid = deidentifier.deidentify(text, ctx);

        assertThat(deid).isEqualTo("[[CHILD_1]] 놀이 / [[CHILD_1]] 상호작용 / [[CHILD_1]] 태도");
        assertThat(ctx.restore(deid)).isEqualTo(text);
    }

    @Test
    void 복원시_매핑없는_정상형식_센티넬은_중립어로_치환된다() {
        RestorationContext ctx = deidentifier.newContext(List.of("최아인"));
        // 모델이 환각해 매핑에 없는 '정상 형식' 토큰을 끼워 넣은 경우 — 실명이 아니므로 "아이"로 흘려보낸다.
        String aiText = "{\"summary\":\"[[CHILD_1]] 와 [[CHILD_9]] 가 놀이했어요\"}";

        String restored = ctx.restore(aiText);

        assertThat(restored).isEqualTo("{\"summary\":\"최아인 와 아이 가 놀이했어요\"}");
        assertThat(restored).doesNotContain("[[CHILD_");
    }

    @Test
    void 복원후_형식깨진_센티넬_잔존시_DeidentificationException() {
        RestorationContext ctx = deidentifier.newContext(List.of("최아인"));
        // 정상 형식([[CHILD_<digits>]])이 아닌 깨진 센티넬은 진짜 이상 → 방어적 차단 유지(NFR-1).
        String aiText = "{\"summary\":\"[[CHILD_oops]] 가 놀이했어요\"}";

        assertThatThrownBy(() -> ctx.restore(aiText))
                .isInstanceOf(DeidentificationException.class);
    }

    @Test
    void 서로_다른_컨텍스트는_독립적인_매핑을_가진다() {
        RestorationContext ctx1 = deidentifier.newContext(List.of("강하준"));
        RestorationContext ctx2 = deidentifier.newContext(List.of("정시우"));

        String deid1 = deidentifier.deidentify("강하준 놀이", ctx1);
        String deid2 = deidentifier.deidentify("정시우 놀이", ctx2);

        // 같은 [[CHILD_1]] 을 쓰더라도 각 컨텍스트는 자기 실명으로만 복원(요청 스코프 격리)
        assertThat(ctx1.restore(deid1)).isEqualTo("강하준 놀이");
        assertThat(ctx2.restore(deid2)).isEqualTo("정시우 놀이");
        assertThat(ctx1.restore(deid1)).doesNotContain("정시우");
    }

    @Test
    void 빈이름과_중복이름은_무시된다() {
        RestorationContext ctx = deidentifier.newContext(Arrays.asList("김하나", "", null, "김하나"));

        String deid = deidentifier.deidentify("김하나 놀이", ctx);

        assertThat(deid).isEqualTo("[[CHILD_1]] 놀이"); // 중복은 같은 토큰, 빈/널은 스킵
        assertThat(ctx.restore(deid)).isEqualTo("김하나 놀이");
    }

    @Test
    void 성을_뗀_이름_호칭도_같은_토큰으로_치환된다() {
        // 교사·시드는 "고은서"가 아니라 "은서"로 적는 경우가 많다 — 축약형도 마스킹되어야 유출이 없다.
        RestorationContext ctx = deidentifier.newContext(List.of("고은서", "김서준"));
        String text = "은서가 서준이와 블록을 하나씩 번갈아 쌓음";

        String deid = deidentifier.deidentify(text, ctx);

        assertThat(deid).doesNotContain("은서", "서준");
        assertThat(deid).contains("[[CHILD_1]]", "[[CHILD_2]]");
        // 복원은 언제나 풀네임으로 — 축약형이 실명(풀네임)으로 안전하게 되돌아온다.
        assertThat(ctx.restore(deid)).contains("고은서", "김서준").doesNotContain("[[CHILD_");
    }

    @Test
    void 동명이인의_축약형은_모호하므로_치환하지_않는다() {
        // "서준"이 김서준·이서준 둘 다를 가리킬 수 있으면 어느 실명인지 알 수 없다 → 별칭 보류(엉뚱한 복원 방지).
        RestorationContext ctx = deidentifier.newContext(List.of("김서준", "이서준"));

        String deid = deidentifier.deidentify("서준이가 놀아요", ctx);

        assertThat(deid).isEqualTo("서준이가 놀아요"); // 축약형 별칭 없음(풀네임만 매핑)
    }

    @Test
    void 축약형이_다른아이_풀네임과_겹치면_그_풀네임_매핑을_보존한다() {
        // "은서"(2글자 실명)와 "고은서"가 공존 — "고은서"의 축약형 별칭이 "은서"의 자기 매핑을 침해하면 안 된다.
        RestorationContext ctx = deidentifier.newContext(List.of("은서", "고은서"));

        String deid = deidentifier.deidentify("고은서와 은서", ctx);

        assertThat(ctx.restore(deid)).isEqualTo("고은서와 은서"); // 각자 자기 실명으로 복원
    }

    @Test
    void null_텍스트는_그대로_통과한다() {
        RestorationContext ctx = deidentifier.newContext(List.of("김민준"));

        assertThat(deidentifier.deidentify(null, ctx)).isNull();
        assertThat(ctx.restore(null)).isNull();
        assertThatNoException().isThrownBy(() -> ctx.restore("토큰 없는 평범한 텍스트"));
    }
}
