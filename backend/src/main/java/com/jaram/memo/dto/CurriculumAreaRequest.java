package com.jaram.memo.dto;

import com.jaram.memo.domain.CurriculumArea;
import jakarta.validation.constraints.NotNull;

/**
 * 메모 영역 수정 요청(API [9], FR-7 Story 2.3). 5영역 중 하나 필수(미분류로 되돌림 비허용).
 */
public record CurriculumAreaRequest(
        @NotNull CurriculumArea curriculumArea
) {
}
