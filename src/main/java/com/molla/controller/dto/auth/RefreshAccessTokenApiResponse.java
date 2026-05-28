package com.molla.controller.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access Token 재발급 성공 응답")
public record RefreshAccessTokenApiResponse(

        @Schema(description = "요청 성공 여부", example = "true")
        boolean success,

        @Schema(description = "응답 메시지", example = "요청이 성공했습니다.")
        String message,

        @Schema(description = "재발급된 Access Token 데이터")
        AccessTokenResponse data
) {
}
