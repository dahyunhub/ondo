package com.ondo.ai.prompt;

import com.ondo.ai.dto.JournalAnalysisResult;
import com.ondo.ai.dto.JournalAnalysisResult.MemoArea;
import com.ondo.common.exception.AiAnalysisException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.memo.domain.CurriculumArea;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** OutputValidator 단위 테스트 — 내용 완전성 규칙(§3). */
class OutputValidatorTest {

    private final OutputValidator validator = new OutputValidator();
    private final Set<Integer> expected = Set.of(1, 2);

    private Map<CurriculumArea, String> fullAreas() {
        Map<CurriculumArea, String> m = new EnumMap<>(CurriculumArea.class);
        for (CurriculumArea a : CurriculumArea.values()) {
            m.put(a, a.name() + " 서술");
        }
        return m;
    }

    private List<MemoArea> coverBoth() {
        return List.of(new MemoArea(1, CurriculumArea.SOCIAL), new MemoArea(2, CurriculumArea.ART));
    }

    @Test
    void 정상_결과는_통과한다() {
        JournalAnalysisResult r = new JournalAnalysisResult("요약", fullAreas(), coverBoth());
        assertThatCode(() -> validator.validate(r, expected)).doesNotThrowAnyException();
    }

    @Test
    void summary_공백이면_실패() {
        JournalAnalysisResult r = new JournalAnalysisResult("  ", fullAreas(), coverBoth());
        assertInvalid(r);
    }

    @Test
    void 영역이_하나라도_누락이면_실패() {
        Map<CurriculumArea, String> areas = fullAreas();
        areas.remove(CurriculumArea.NATURE);
        assertInvalid(new JournalAnalysisResult("요약", areas, coverBoth()));
    }

    @Test
    void 영역_서술이_공백이면_실패() {
        Map<CurriculumArea, String> areas = fullAreas();
        areas.put(CurriculumArea.ART, "");
        assertInvalid(new JournalAnalysisResult("요약", areas, coverBoth()));
    }

    @Test
    void 입력_메모_인덱스를_빠짐없이_커버하지_못하면_실패() {
        // expected {1,2} 인데 1만 분류됨
        List<MemoArea> partial = List.of(new MemoArea(1, CurriculumArea.SOCIAL));
        assertInvalid(new JournalAnalysisResult("요약", fullAreas(), partial));
    }

    @Test
    void 입력에_없는_초과_인덱스가_있으면_실패() {
        // expected {1,2} 인데 999 가 추가됨
        List<MemoArea> excess = List.of(
                new MemoArea(1, CurriculumArea.SOCIAL),
                new MemoArea(2, CurriculumArea.ART),
                new MemoArea(999, CurriculumArea.NATURE));
        assertInvalid(new JournalAnalysisResult("요약", fullAreas(), excess));
    }

    @Test
    void 같은_인덱스를_중복_분류하면_실패() {
        List<MemoArea> dup = List.of(
                new MemoArea(1, CurriculumArea.SOCIAL),
                new MemoArea(1, CurriculumArea.ART),
                new MemoArea(2, CurriculumArea.NATURE));
        assertInvalid(new JournalAnalysisResult("요약", fullAreas(), dup));
    }

    private void assertInvalid(JournalAnalysisResult r) {
        assertThatThrownBy(() -> validator.validate(r, expected))
                .isInstanceOf(AiAnalysisException.class)
                .extracting(e -> ((AiAnalysisException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_OUTPUT_INVALID);
    }
}
