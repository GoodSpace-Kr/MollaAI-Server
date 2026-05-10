package com.molla.common.exception;

import com.molla.common.response.ApiResponse;
import com.molla.common.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 도메인 비즈니스 예외 처리
     * throw new GlobalException(ErrorCode.USER_NOT_FOUND) 형태로 사용
     */
    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(GlobalException ex) {
        log.warn("GlobalException: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());

        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * @Valid 검증 실패 처리
     * 여러 필드 에러가 있으면 ", " 로 합쳐서 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("ValidationException: {}", message);

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST, message));
    }

    /**
     * 처리되지 않은 예외 — 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
