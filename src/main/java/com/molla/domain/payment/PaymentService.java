package com.molla.domain.payment;

import com.molla.controller.dto.payment.PaymentApproveRequest;
import com.molla.controller.dto.payment.PaymentApproveResponse;
import com.molla.controller.dto.subscription.CreateSubscriptionRequest;
import com.molla.domain.subscription.SubscriptionService;
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

        // 결제 금액 검증
        int expectedAmount = SubscriptionService.getPriceByPlan(request.planType());
        if (expectedAmount == 0) {
            throw new PaymentException("무료 플랜은 결제가 필요하지 않습니다.");
        }
        if (expectedAmount != request.amount()) {
            throw new PaymentException(
                    "결제 금액이 올바르지 않습니다. 예상: " + expectedAmount + "원, 요청: " + request.amount() + "원");
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

        // 구독 자동 생성 (dailyLimit은 플랜에서 자동 결정)
        CreateSubscriptionRequest subRequest = new CreateSubscriptionRequest(
                request.planType(),
                30  // 30일 구독
        );
        subscriptionService.createSubscription(userId, subRequest);

        log.info("결제 및 구독 생성 완료 — userId: {}, planType: {}, orderId: {}",
                userId, request.planType(), request.orderId());

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
