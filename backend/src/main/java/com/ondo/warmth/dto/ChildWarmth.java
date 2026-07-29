package com.ondo.warmth.dto;

import com.ondo.warmth.domain.WarmthLevel;

import java.time.LocalDate;

/**
 * 아이 1명의 온도 판정 결과.
 *
 * <p><b>점수·메모 건수 필드를 두지 않는다.</b> 응답에 숫자를 실으면 언젠가 화면에 노출되고,
 * 그때부터 아이들끼리 비교되는 랭킹이 된다(spec-child-warmth Never 조항).
 * {@code snoozedUntil} 은 점수가 아니라 상태 정보이며, "언제 돌아오는지"를 보여줘 접어두기가
 * 영구 제거가 아님을 계속 상기시키는 역할을 한다.
 */
public record ChildWarmth(
        Long childId,
        WarmthLevel level,
        /** {@code SNOOZED} 일 때만 채워지는 접어두기 만료일(KST). 그 외에는 null. */
        LocalDate snoozedUntil
) {

    public static ChildWarmth of(Long childId, WarmthLevel level) {
        return new ChildWarmth(childId, level, null);
    }

    public static ChildWarmth snoozed(Long childId, LocalDate until) {
        return new ChildWarmth(childId, WarmthLevel.SNOOZED, until);
    }
}
