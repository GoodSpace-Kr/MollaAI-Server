package com.molla.controller.dto.inquiry;

import com.molla.domain.inquiry.Inquiry;

import java.time.LocalDateTime;

public record InquiryResponse(
        String id,
        String name,
        String email,
        String content,
        boolean read,
        LocalDateTime createdAt
) {
    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getName(),
                inquiry.getEmail(),
                inquiry.getContent(),
                inquiry.isRead(),
                inquiry.getCreatedAt()
        );
    }
}
