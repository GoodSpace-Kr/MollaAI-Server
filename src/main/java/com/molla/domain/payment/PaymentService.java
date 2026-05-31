package com.molla.domain.payment;

import com.molla.controller.dto.payment.PaymentApproveRequest;
import com.molla.controller.dto.payment.PaymentApproveResponse;
import com.molla.domain.subscription.SubscriptionService;
import com.molla.controller.dto.subscription.CreateSubscriptionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final NicepayClient nicepayClient;
    private final PaymentRepository paymentRepository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public PaymentApproveResponse approve(String userId, PaymentApproveRequest request) {

        // 중복 결제 방지
        if (paymentRepository.existsByOrderId(request.orderId())) {
            throw new PaymentException("이미 처리된 주문입니다: " + request.orderId());
        }

        // 나이스페이 승인 API 호출
        Map<String, Object> result = nicepayClient.approve(request.tid(), request.amount());

        String resultCode = (String) result.get("resultCode");

        if (!"0000".equals(resultCode)) {
            String resultMsg = (String) result.getOrDefault("resultMsg", "승인 실패");
            log.error("나이스페이 승인 실패 — orderId: {}, resultCode: {}, msg: {}",
                    request.orderId(), resultCode, resultMsg);
            throw new PaymentException("결제 승인 실패: " + resultMsg);
        }

        // 결제 내역 저장
        Payment payment = Payment.create(
                userId,
                request.tid(),
                request.orderId(),
                request.planType(),
                request.amount()
        );
        paymentRepository.save(payment);

        // 결제 성공 → 구독 자동 생성
        // [교체 필요] 실서비스 전환 시 플랜별 dailyLimitMinutes, durationDays 정책 맞게 조정
        CreateSubscriptionRequest subRequest = new CreateSubscriptionRequest(
                request.planType(),
                "premium".equals(request.planType()) ? 300 : 30,
                30  // 30일 구독
        );
        subscriptionService.createSubscription(userId, subRequest);

        log.info("결제 및 구독 생성 완료 — userId: {}, orderId: {}, amount: {}",
                userId, request.orderId(), request.amount());

        return new PaymentApproveResponse(
                payment.getId(),
                payment.getTid(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt()
        );
    }
}
