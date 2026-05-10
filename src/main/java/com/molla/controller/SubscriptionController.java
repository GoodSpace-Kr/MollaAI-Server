package com.molla.controller;

import com.molla.common.response.ApiResponse;
import com.molla.controller.dto.subscription.CreateSubscriptionRequest;
import com.molla.controller.dto.subscription.SubscriptionResponse;
import com.molla.controller.dto.subscription.SubscriptionWithRemainingResponse;
import com.molla.domain.subscription.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Subscription", description = "구독 API — 구독 조회 및 생성")
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(
            summary = "내 구독 정보 조회",
            description = """
                    현재 활성 구독 정보와 오늘 잔여 통화 가능 분을 반환합니다.
                    - 만료된 구독은 자동으로 expired 처리 후 404 반환
                    - 구독이 없으면 404 반환
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = SubscriptionWithRemainingResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "활성 구독 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/api/v1/subscriptions/me")
    public ResponseEntity<ApiResponse<SubscriptionWithRemainingResponse>> getMySubscription() {
        String userId = getCurrentUserId();
        SubscriptionWithRemainingResponse response = subscriptionService.getMySubscription(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "구독 생성",
            description = """
                    새 구독을 생성합니다.
                    - 이미 활성 구독이 있으면 409 반환
                    - planType: `free` (30분/일) / `premium` (직접 설정)
                    - durationDays가 null이면 무기한 구독
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "구독 생성 성공",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "이미 활성 구독 존재",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/api/v1/subscriptions")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(
            @RequestBody @Valid CreateSubscriptionRequest request
    ) {
        String userId = getCurrentUserId();
        SubscriptionResponse response = subscriptionService.createSubscription(userId, request);
        return ResponseEntity.ok(ApiResponse.success("구독이 생성되었습니다.", response));
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
