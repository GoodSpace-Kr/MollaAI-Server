package com.molla.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공통 응답 래퍼")
@JsonInclude(JsonInclude.Include.NON_NULL) // data가 null이면 필드 자체를 응답에서 제외
public record ApiResponse<T>(

        @Schema(description = "응답 코드 (성공: SUCCESS, 실패: 에러 코드)", example = "SUCCESS")
        String code,

        @Schema(description = "응답 메시지", example = "요청이 성공했습니다.")
        String message,

        @Schema(description = "응답 데이터")
        T data,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "응답 시각", example = "2025-01-01T00:00:00")
        LocalDateTime timestamp
) {
    // ──────────────────────────────────────────────
    // 성공 응답
    // ──────────────────────────────────────────────

    /** 데이터 있는 성공 응답 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "요청이 성공했습니다.", data, LocalDateTime.now());
    }

    /** 데이터 없는 성공 응답 (204 No Content 대신 200 + 빈 응답) */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>("SUCCESS", "요청이 성공했습니다.", null, LocalDateTime.now());
    }

    /** 커스텀 메시지 성공 응답 */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data, LocalDateTime.now());
    }

    // ──────────────────────────────────────────────
    // 실패 응답
    // ──────────────────────────────────────────────

    /** ErrorCode 기반 실패 응답 */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null, LocalDateTime.now());
    }

    /** 커스텀 메시지 실패 응답 (validation 에러 등 동적 메시지) */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null, LocalDateTime.now());
    }
}
