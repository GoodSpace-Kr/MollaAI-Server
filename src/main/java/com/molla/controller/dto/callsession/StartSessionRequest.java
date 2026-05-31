package com.molla.controller.dto.callsession;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "통화 세션 시작 요청 (내부 API — AI 오케스트레이션 서버 전용)")
public record StartSessionRequest(

        @Schema(description = "전화번호 (숫자만, 하이픈 없이)", example = "01012345678")
        @NotBlank(message = "phoneNumber를 입력해주세요.")
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 phoneNumber 형식이 아닙니다.")
        String phoneNumber,

        @Schema(description = "통화 공급자가 발급한 통화 ID", example = "CA1234abcd")
        String callSid
) {}
