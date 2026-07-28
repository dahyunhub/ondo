package com.ondo.warmth.domain;

/**
 * 아이별 관찰 온도. 의도적으로 2단계뿐이다 — 중간 단계나 연속값을 두면 교사가 등급·점수로 읽고,
 * 그 순간 "인지 도구"가 "성적표"가 된다(spec-child-warmth Intent).
 */
public enum WarmthLevel {

    /** 최근 기록이 반 평균 수준으로 쌓여 있음. 기본값 — 판정이 애매하면 항상 이쪽. */
    WARM,

    /** 최근 기록이 반 평균 대비 현저히 옅음. 소수에게만 붙는다. */
    LOW
}
