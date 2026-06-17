package com.jaram.common.exception;

import com.jaram.common.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 전역 예외 처리 단일 지점. 모든 예외를 ErrorCode → 표준 에러 응답(ApiError)으로 변환한다.
 * 로그에 아이 실명·민감정보를 남기지 않는다(NFR-1).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        // 4xx(클라이언트 귀책)는 WARN, 5xx(장애)는 ERROR
        if (code.getStatus().is5xxServerError()) {
            log.error("BusinessException [{}] at {}", code, request.getRequestURI(), ex);
        } else {
            log.warn("BusinessException [{}] at {}: {}", code, request.getRequestURI(), ex.getMessage());
        }
        ApiError body = ApiError.of(code.getStatus().value(), code.name(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorCode code = ErrorCode.VALIDATION_FAILED;
        ApiError body = ApiError.of(code.getStatus().value(), code.name(), code.getDefaultMessage(),
                request.getRequestURI(), details);
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex,
                                                     HttpServletRequest request) {
        // 잘못된 JSON·enum 값 등 본문 파싱 실패 → 500 대신 400
        ErrorCode code = ErrorCode.VALIDATION_FAILED;
        ApiError body = ApiError.of(code.getStatus().value(), code.name(), code.getDefaultMessage(),
                request.getRequestURI());
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        // 유니크/FK 등 DB 제약 위반(예: token_alias 동시 등록 경쟁) → 500 대신 명확한 409
        ErrorCode code = ErrorCode.DATA_CONFLICT;
        log.warn("DataIntegrityViolation at {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        ApiError body = ApiError.of(code.getStatus().value(), code.name(), code.getDefaultMessage(),
                request.getRequestURI());
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                           HttpServletRequest request) {
        ErrorCode code = ErrorCode.METHOD_NOT_ALLOWED;
        ApiError body = ApiError.of(code.getStatus().value(), code.name(), code.getDefaultMessage(),
                request.getRequestURI());
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleFallback(Exception ex, HttpServletRequest request) {
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        ApiError body = ApiError.of(code.getStatus().value(), code.name(), code.getDefaultMessage(),
                request.getRequestURI());
        return ResponseEntity.status(code.getStatus()).body(body);
    }
}
