package com.ondo.report;

/**
 * 월말 자동 평가 1건의 처리 결과(FR-9). 스케줄러가 카운트·로깅에 사용.
 */
public enum MonthlyOutcome {
    CREATED,
    SKIPPED_EXISTS,   // 이미 그달 평가 존재(멱등)
    SKIPPED_NO_MEMO   // 그달 메모 없음
}
