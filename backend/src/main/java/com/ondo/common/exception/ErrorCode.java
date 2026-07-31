package com.ondo.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 표준 에러코드 카탈로그. 정본: docs/specs/error-code-catalog.md §3.
 * 모든 도메인/AI 예외는 이 enum 으로 매핑되어 단일 지점(GlobalExceptionHandler)에서 HTTP 응답으로 변환된다.
 */
public enum ErrorCode {

    // AUTH
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않아요."),
    AUTH_UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인이 만료됐어요. 다시 로그인해 주세요."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없어요."),
    AUTH_KAKAO_FAILED(HttpStatus.UNAUTHORIZED, "카카오 로그인에 실패했어요. 다시 시도해 주세요."),
    KAKAO_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "카카오 로그인이 잠시 원활하지 않아요. 잠시 후 다시 시도해 주세요."),

    // VALIDATION
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
    MEMO_EMPTY(HttpStatus.BAD_REQUEST, "내용 또는 항목 중 하나 이상을 입력해 주세요."),
    INVALID_CURRICULUM_AREA(HttpStatus.BAD_REQUEST, "누리과정 영역 값이 올바르지 않아요."),
    RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "링크가 만료되었거나 이미 사용된 링크예요. 비밀번호 재설정을 다시 요청해 주세요."),

    // NOT_FOUND
    TEACHER_NOT_FOUND(HttpStatus.NOT_FOUND, "교사를 찾을 수 없어요."),
    CLASSROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "반을 찾을 수 없어요."),
    CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "아이를 찾을 수 없어요."),
    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "메모를 찾을 수 없어요."),
    JOURNAL_NOT_FOUND(HttpStatus.NOT_FOUND, "일지를 찾을 수 없어요."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "평가를 찾을 수 없어요."),

    // CONFLICT / STATE
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일이에요."),
    SOCIAL_ACCOUNT_NO_PASSWORD(HttpStatus.CONFLICT, "카카오 로그인 계정이라 비밀번호를 사용하지 않아요."),
    ANALYSIS_IN_PROGRESS(HttpStatus.CONFLICT, "이미 분석이 진행 중이에요. 잠시 후 다시 시도해 주세요."),
    JOURNAL_ALREADY_EXISTS(HttpStatus.CONFLICT, "그 날짜의 일지가 이미 있어요. 재분석을 이용해 주세요."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "요청이 현재 상태와 충돌해요. 잠시 후 다시 시도해 주세요."),
    REPORT_NO_MEMO(HttpStatus.UNPROCESSABLE_ENTITY, "해당 기간에 작성된 메모가 없어요."),

    // AI (메모 보존 고지 포함 — FR-4)
    AI_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY, "분석에 실패했어요. 작성하신 메모는 그대로 저장돼 있어요. 잠시 후 다시 시도해 주세요."),
    AI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "분석이 시간 내에 끝나지 않았어요. 작성하신 메모는 그대로 저장돼 있어요."),
    AI_OUTPUT_INVALID(HttpStatus.BAD_GATEWAY, "분석 결과를 확인하지 못했어요. 작성하신 메모는 그대로 저장돼 있어요."),
    DEIDENTIFICATION_RESTORE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "결과 처리 중 문제가 생겼어요. 작성하신 메모는 그대로 저장돼 있어요."),

    // GENERIC
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 요청이에요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 문제가 발생했어요. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
