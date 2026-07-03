package com.ondo.memo.dto;

import jakarta.validation.constraints.Size;

/**
 * 메모 내용 수정 요청(API [7-편집], FR-1). curriculumArea 는 별도 엔드포인트에서 관리.
 * content/playActivity/interaction/attitude 중 최소 1개 non-blank 불변식은 서비스에서 강제(MEMO_EMPTY).
 */
public record MemoContentRequest(
        @Size(max = 2000) String content,
        @Size(max = 500) String playActivity,
        @Size(max = 500) String interaction,
        @Size(max = 500) String attitude
) {
}
