package com.molla.controller;

import com.molla.common.response.ApiResponse;
import com.molla.controller.dto.payment.PaymentApproveRequest;
import com.molla.controller.dto.payment.PaymentApproveResponse;
import com.molla.domain.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment", description = "결제 API — 나이스페이 연동")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(
            summary = "결제 승인",
            description = """
                    나이스페이 결제창 인증 완료 후 서버 승인을 처리합니다.
                    - 프론트에서 나이스페이 JS SDK로 인증 완료 후 tid를 전달
                    - 승인 성공 시 구독이 자동 생성됩니다
                    """
    )
    @PostMapping("/api/v1/payments/approve")
    public ResponseEntity<ApiResponse<PaymentApproveResponse>> approve(
            @RequestBody @Valid PaymentApproveRequest request
    ) {
        String userId = getCurrentUserId();
        PaymentApproveResponse response = paymentService.approve(userId, request);
        return ResponseEntity.ok(ApiResponse.success("결제가 완료되었습니다.", response));
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
