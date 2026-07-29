package com.ondo.warmth.dto;

import java.util.List;

/**
 * 반 전체 온도 응답(API [20]).
 *
 * <p>{@code enabled=false} 는 "판정하기엔 기록이 아직 모자라다"는 뜻이다. 이때 프론트는 온도 관련 UI 를
 * 통째로 감춘다 — 가입 직후 반 전원이 옅게 보이면 그건 정보가 아니라 질책이기 때문이다.
 */
public record WarmthResponse(
        boolean enabled,
        int windowDays,
        List<ChildWarmth> items
) {

    /** 판정 불가(콜드 스타트·대상 아이 없음). 프론트가 기능 자체를 숨기도록 빈 목록을 함께 준다. */
    public static WarmthResponse disabled(int windowDays) {
        return new WarmthResponse(false, windowDays, List.of());
    }
}
