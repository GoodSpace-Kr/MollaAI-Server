package com.molla.domain.payment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @Column(length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "tid", nullable = false, length = 100, unique = true)
    private String tid;                 // 나이스페이 거래 ID

    @Column(name = "order_id", nullable = false, length = 100, unique = true)
    private String orderId;             // 가맹점 주문번호

    @Column(name = "plan_type", nullable = false, length = 20)
    private String planType;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 20)
    private String status;              // paid / failed / cancelled

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Payment create(
            String userId,
            String tid,
            String orderId,
            String planType,
            int amount
    ) {
        Payment p = new Payment();
        p.id = UUID.randomUUID().toString();
        p.userId = userId;
        p.tid = tid;
        p.orderId = orderId;
        p.planType = planType;
        p.amount = amount;
        p.status = "paid";
        p.paidAt = LocalDateTime.now();
        p.createdAt = LocalDateTime.now();
        return p;
    }
}
