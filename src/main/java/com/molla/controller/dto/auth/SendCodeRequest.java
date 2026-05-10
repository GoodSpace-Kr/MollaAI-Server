package com.molla.controller.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "인증번호 발송 요청")
public record SendCodeRequest(

        @Schema(description = "전화번호 (숫자만, 하이픈 없이)", example = "01012345678")
        @NotBlank(message = "전화번호를 입력해주세요.")
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 전화번호 형식이 아닙니다.")
        String phoneNumber
) {}
