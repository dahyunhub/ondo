package com.ondo.ai.dto;

/**
 * 일지 분석 프롬프트에 들어가는 비식별화된 메모 한 건(요청 스코프 인덱스 포함).
 * 텍스트 필드는 이미 [[CHILD_n]] 으로 비식별화된 상태여야 한다(복원은 응답 처리 단계).
 *
 * @param index        user 프롬프트에 표기되는 1-based 정수 인덱스(FR-2: memoClassifications 와 매핑)
 * @param content      메모 본문(nullable)
 * @param playActivity 놀이(nullable)
 * @param interaction  상호작용(nullable)
 * @param attitude     태도(nullable)
 */
public record MemoPromptInput(
        int index,
        String content,
        String playActivity,
        String interaction,
        String attitude
) {
}
