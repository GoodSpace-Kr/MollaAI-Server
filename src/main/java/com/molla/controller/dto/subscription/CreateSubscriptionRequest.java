package com.molla.controller.dto.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "구독 생성 요청")
public record CreateSubscriptionRequest(

        @Schema(description = "구독 플랜 타입 (free / premium)", example = "premium")
        @NotBlank(message = "플랜 타입을 입력해주세요.")
        @Pattern(regexp = "^(free|premium)$", message = "플랜 타입은 free 또는 premium 이어야 합니다.")
        String planType,

        @Schema(description = "하루 통화 가능 분 (기본값: 30)", example = "60")
        @Min(value = 1, message = "통화 가능 분은 1분 이상이어야 합니다.")
        Integer dailyLimitMinutes,

        @Schema(description = "구독 만료 일수 (null이면 무기한)", example = "30")
        Integer durationDays
) {}
