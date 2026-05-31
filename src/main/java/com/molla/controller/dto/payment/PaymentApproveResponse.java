package com.molla.controller.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "결제 승인 응답")
public record PaymentApproveResponse(
        String paymentId,
        String tid,
        String orderId,
        int amount,
        String status,
        LocalDateTime paidAt
) {}
