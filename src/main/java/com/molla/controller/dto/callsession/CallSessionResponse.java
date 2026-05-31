package com.molla.controller.dto.callsession;

import com.molla.domain.callsession.CallSession;
import com.molla.controller.dto.subscription.SubscriptionWithRemainingResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "통화 세션 응답. 내부 start 응답에서는 subscription이 함께 내려오고, 목록/상세/종료 응답에서는 null일 수 있습니다.")
public record CallSessionResponse(

        @Schema(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "세션 타입 (level_test / practice)", example = "practice")
        String sessionType,

        @Schema(description = "통화 시작 시점의 유저 상태 (unregistered / registered / subscribed)", example = "subscribed")
        String userStateAtCall,

        @Schema(description = "통화 시작 일시")
        LocalDateTime startedAt,

        @Schema(description = "통화 종료 일시")
        LocalDateTime endedAt,

        @Schema(description = "저장된 통화 시간 (초). 내부 end 요청에서 durationMinutes를 보내면 서버가 초 단위로 변환해 저장합니다.", example = "180")
        Integer durationSeconds,

        @Schema(description = "현재 활성 구독 정보와 오늘 잔여 통화 시간. 내부 start 세션 응답에서는 채워지고, 그 외 응답에서는 null일 수 있습니다.")
        SubscriptionWithRemainingResponse subscription,

        @Schema(description = "세션 상태 (in_progress / completed / failed)", example = "completed")
        String status
) {
    public static CallSessionResponse from(CallSession session) {
        return from(session, null);
    }

    public static CallSessionResponse from(
            CallSession session,
            SubscriptionWithRemainingResponse subscription
    ) {
        return new CallSessionResponse(
                session.getId(),
                session.getSessionType(),
                session.getUserStateAtCall(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getDurationSeconds(),
                subscription,
                session.getStatus()
        );
    }
}
