package com.jaram.memo.domain;

/**
 * 누리과정 5영역(data-model-spec §2). memo.curriculum_area 에 저장.
 * 저장 시점엔 NULL(미분류) — Epic 3 일지 분석 패스(FR-2)에서 자동 분류되거나 타임라인에서 수동 수정(FR-7).
 * JSON/DB 는 enum 값을 그대로 사용하고, 한글 영역명은 프론트가 매핑.
 */
public enum CurriculumArea {
    PHYSICAL_HEALTH,  // 신체운동·건강
    COMMUNICATION,    // 의사소통
    SOCIAL,           // 사회관계
    ART,              // 예술경험
    NATURE            // 자연탐구
}
