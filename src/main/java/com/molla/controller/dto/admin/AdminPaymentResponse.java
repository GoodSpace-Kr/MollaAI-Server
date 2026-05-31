package com.molla.controller.dto.admin;

import com.molla.domain.payment.Payment;
import java.time.LocalDateTime;

public record AdminPaymentResponse(
        String id,
        String userId,
        String tid,
        String orderId,
        String planType,
        int amount,
        String status,
        LocalDateTime paidAt
) {
    public static AdminPaymentResponse from(Payment p) {
        return new AdminPaymentResponse(
                p.getId(), p.getUserId(), p.getTid(),
                p.getOrderId(), p.getPlanType(),
                p.getAmount(), p.getStatus(), p.getPaidAt()
        );
    }
}
