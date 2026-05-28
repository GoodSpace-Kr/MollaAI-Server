package com.molla.controller.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증번호 확인 성공 응답")
public record VerifyCodeApiResponse(

        @Schema(description = "요청 성공 여부", example = "true")
        boolean success,

        @Schema(description = "응답 메시지", example = "요청이 성공했습니다.")
        String message,

        @Schema(description = "JWT 토큰 데이터")
        TokenResponse data
) {
}
