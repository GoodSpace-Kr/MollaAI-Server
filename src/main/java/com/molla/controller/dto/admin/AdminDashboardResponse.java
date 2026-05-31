package com.molla.controller.dto.admin;

public record AdminDashboardResponse(
        long totalUsers,
        long registeredUsers,
        long totalSessions,
        long totalReports,
        long totalPayments,
        long totalPaymentAmount,
        long unreadInquiries
) {}
