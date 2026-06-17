package com.jaram.memo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 메모 저장 요청(API [7]). curriculumArea 는 받지 않음(밤 분석에서 분류).
 * content/playActivity/interaction/attitude 중 최소 1개 non-blank 불변식은 서비스에서 강제(MEMO_EMPTY).
 */
public record MemoRequest(
        @NotNull Long childId,
        @Size(max = 2000) String content,
        @Size(max = 500) String playActivity,
        @Size(max = 500) String interaction,
        @Size(max = 500) String attitude
) {
}
