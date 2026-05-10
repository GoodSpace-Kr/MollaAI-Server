package com.molla.controller.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청 — 이름만 입력")
public record RegisterRequest(

        @Schema(description = "이름 (앱에서 표시될 이름)", example = "홍길동")
        @NotBlank(message = "이름을 입력해주세요.")
        @Size(min = 1, max = 50, message = "이름은 1~50자입니다.")
        String username
) {}
