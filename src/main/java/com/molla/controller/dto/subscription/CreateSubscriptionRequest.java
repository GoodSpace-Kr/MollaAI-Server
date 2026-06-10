package com.molla.controller.dto.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "구독 생성 요청")
public record CreateSubscriptionRequest(

        @Schema(description = "구독 플랜 타입 (free / premium / max / professional)", example = "premium")
        @NotBlank(message = "플랜 타입을 입력해주세요.")
        @Pattern(regexp = "^(free|premium|max|professional)$",
                message = "플랜 타입은 free, premium, max, professional 중 하나여야 합니다.")
        String planType,

        @Schema(description = "구독 만료 일수 (null이면 30일)", example = "30")
        Integer durationDays
) {}
