package com.molla.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    boolean existsByOrderId(String orderId);
    boolean existsByTid(String tid);
}
