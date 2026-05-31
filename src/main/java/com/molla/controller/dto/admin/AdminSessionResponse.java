package com.molla.controller.dto.admin;

import com.molla.domain.callsession.CallSession;
import java.time.LocalDateTime;

public record AdminSessionResponse(
        String id,
        String userId,
        String phoneNumber,
        String sessionType,
        String status,
        Integer durationSeconds,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
    public static AdminSessionResponse from(CallSession s) {
        return new AdminSessionResponse(
                s.getId(), s.getUserId(), s.getPhoneNumber(),
                s.getSessionType(), s.getStatus(),
                s.getDurationSeconds(), s.getStartedAt(), s.getEndedAt()
        );
    }
}
