package com.molla.controller;

import com.molla.common.response.ApiResponse;
import com.molla.controller.dto.admin.*;
import com.molla.controller.dto.feedbackreport.FeedbackReportResponse;
import com.molla.domain.callsession.CallSession;
import com.molla.domain.callsession.CallSessionRepository;
import com.molla.domain.feedbackreport.FeedbackReport;
import com.molla.domain.feedbackreport.FeedbackReportRepository;
import com.molla.domain.feedbackreport.FeedbackReportViewMapper;
import com.molla.domain.inquiry.InquiryRepository;
import com.molla.domain.payment.PaymentRepository;
import com.molla.domain.subscription.SubscriptionRepository;
import com.molla.domain.user.User;
import com.molla.domain.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final CallSessionRepository callSessionRepository;
    private final FeedbackReportRepository feedbackReportRepository;
    private final FeedbackReportViewMapper feedbackReportViewMapper;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InquiryRepository inquiryRepository;

    // ── 대시보드 ──────────────────────────────────
    @Operation(summary = "대시보드 KPI 집계")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> dashboard() {
        long totalUsers = userRepository.count();
        long registeredUsers = userRepository.findAll().stream()
                .filter(User::isRegistered).count();
        long totalSessions = callSessionRepository.count();
        long totalReports = feedbackReportRepository.count();
        long totalPayments = paymentRepository.count();
        long totalPaymentAmount = paymentRepository.sumTotalAmount();
        long unreadInquiries = inquiryRepository.countByReadFalse();

        return ResponseEntity.ok(ApiResponse.success(new AdminDashboardResponse(
                totalUsers, registeredUsers, totalSessions,
                totalReports, totalPayments, totalPaymentAmount, unreadInquiries
        )));
    }

    // ── 회원 ──────────────────────────────────────
    @Operation(summary = "전체 회원 목록")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> users() {
        List<AdminUserResponse> list = userRepository.findAll().stream()
                .map(AdminUserResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @Operation(summary = "회원 상세 조회 (통화 수, 구독 포함)")
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> userDetail(
            @PathVariable String id
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        int totalSessions = callSessionRepository.countCompletedByUserId(id);
        int totalDurationSeconds = callSessionRepository.sumDurationSecondsByUserId(id);
        var subscription = subscriptionRepository.findActiveByUserId(id).orElse(null);

        return ResponseEntity.ok(ApiResponse.success(
                AdminUserDetailResponse.of(user, totalSessions, totalDurationSeconds, subscription)
        ));
    }

    @Operation(summary = "회원 상태 변경 (active/suspended)")
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable String id,
            @RequestParam String status
    ) {
        userRepository.findById(id).ifPresent(user -> {
            if ("suspended".equals(status)) user.suspend();
            else if ("active".equals(status)) user.restore();
            userRepository.save(user);
        });
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "회원 삭제 (관리자 — 완전 삭제)")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("유저 없음: " + id);
        }
        userRepository.deleteById(id);
        log.info("관리자 회원 삭제 — userId: {}", id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ── 통화 세션 ──────────────────────────────────
    @Operation(summary = "전체 통화 세션 목록")
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<AdminSessionResponse>>> sessions() {
        List<AdminSessionResponse> list = callSessionRepository.findAllByOrderByStartedAtDesc()
                .stream()
                .map(AdminSessionResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // ── 리포트 ────────────────────────────────────
    @Operation(summary = "전체 리포트 목록")
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<AdminReportResponse>>> reports() {
        List<AdminReportResponse> list = feedbackReportRepository.findAll().stream()
                .map(AdminReportResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @Operation(summary = "리포트 상세 조회 (관리자)")
    @GetMapping("/reports/{sessionId}")
    public ResponseEntity<ApiResponse<FeedbackReportResponse>> reportDetail(
            @PathVariable String sessionId
    ) {
        FeedbackReport report = feedbackReportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("리포트 없음"));
        CallSession session = callSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션 없음"));
        return ResponseEntity.ok(ApiResponse.success(feedbackReportViewMapper.toDetailResponse(report, session)));
    }

    // ── 결제 ──────────────────────────────────────
    @Operation(summary = "전체 결제 내역")
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<AdminPaymentResponse>>> payments() {
        List<AdminPaymentResponse> list = paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(AdminPaymentResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
