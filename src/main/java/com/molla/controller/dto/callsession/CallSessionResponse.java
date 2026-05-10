package com.molla.controller.dto.callsession;

import com.molla.domain.callsession.CallSession;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "통화 세션 응답")
public record CallSessionResponse(

        @Schema(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "세션 타입 (level_test / practice)", example = "practice")
        String sessionType,

        @Schema(description = "통화 시작 시점의 유저 상태 (unregistered / registered / subscribed)", example = "subscribed")
        String userStateAtCall,

        @Schema(description = "통화 주제", example = "비즈니스 미팅 영어")
        String topic,

        @Schema(description = "통화 시작 일시")
        LocalDateTime startedAt,

        @Schema(description = "통화 종료 일시")
        LocalDateTime endedAt,

        @Schema(description = "통화 시간 (초)", example = "183")
        Integer durationSeconds,

        @Schema(description = "세션 상태 (in_progress / completed / failed)", example = "completed")
        String status
) {
    public static CallSessionResponse from(CallSession session) {
        return new CallSessionResponse(
                session.getId(),
                session.getSessionType(),
                session.getUserStateAtCall(),
                session.getTopic(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getDurationSeconds(),
                session.getStatus()
        );
    }
}
