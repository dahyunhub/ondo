package com.ondo.warmth.dto;

import com.ondo.warmth.domain.WarmthLevel;

/**
 * 아이 1명의 온도 판정 결과.
 *
 * <p><b>점수·메모 건수 필드를 두지 않는다.</b> 응답에 숫자를 실으면 언젠가 화면에 노출되고,
 * 그때부터 아이들끼리 비교되는 랭킹이 된다(spec-child-warmth Never 조항).
 */
public record ChildWarmth(
        Long childId,
        WarmthLevel level
) {
}
