package com.molla.controller.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "나이스페이 결제 승인 요청")
public record PaymentApproveRequest(

        @Schema(description = "나이스페이 인증 후 발급된 거래 ID", example = "nictest00m01011104191651591152")
        @NotBlank(message = "tid를 입력해주세요.")
        String tid,

        @Schema(description = "가맹점 주문번호 (프론트에서 생성)", example = "ORDER-20260531-001")
        @NotBlank(message = "orderId를 입력해주세요.")
        String orderId,

        @Schema(description = "결제 금액", example = "9900")
        @Min(value = 1, message = "결제 금액은 1원 이상이어야 합니다.")
        int amount,

        @Schema(description = "플랜 타입 (free / premium)", example = "premium")
        @NotBlank(message = "planType을 입력해주세요.")
        String planType
) {}
