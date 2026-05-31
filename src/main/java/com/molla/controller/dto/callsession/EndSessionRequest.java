package com.molla.controller.dto.callsession;

import com.molla.domain.callsession.CallSessionTurn;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "통화 세션 종료 요청 (내부 API — AI 오케스트레이션 서버 전용)")
public record EndSessionRequest(

        @Schema(description = "종료 상태 (completed / failed). 기본값: completed", example = "completed")
        String status,

        @Schema(description = "실제 통화 시간(분). 서버는 이 값을 초 단위로 변환해 저장하고, 이후 오늘 잔여 통화 시간 계산에 사용합니다.", example = "3")
        Integer durationMinutes,

        @Schema(description = "통화 턴 목록")
        List<TurnPayload> turns
) {
    public String resolvedStatus() {
        return (status != null && "failed".equals(status)) ? "failed" : "completed";
    }

    public List<CallSessionTurn> toCallSessionTurns() {
        if (turns == null) {
            return List.of();
        }

        return turns.stream()
                .filter(turn -> turn != null)
                .map(turn -> new CallSessionTurn(
                        turn.index(),
                        turn.createdAt(),
                        turn.user() != null ? new CallSessionTurn.UserTurn(
                                turn.user().text(),
                                turn.user().sampleRate(),
                                turn.user().audioKey()
                        ) : null,
                        turn.assistant() != null ? new CallSessionTurn.AssistantTurn(
                                turn.assistant().text(),
                                turn.assistant().translatedText(),
                                turn.assistant().createdAt()
                        ) : null
                ))
                .toList();
    }

    @Schema(description = "통화 턴 데이터")
    public record TurnPayload(
            @Schema(description = "턴 순번", example = "1")
            Integer index,

            @Schema(description = "턴 생성 시각", example = "2026-05-20T12:00:01.123456+00:00")
            OffsetDateTime createdAt,

            @Schema(description = "사용자 발화")
            UserTurnPayload user,

            @Schema(description = "어시스턴트 발화")
            AssistantTurnPayload assistant
    ) {
    }

    @Schema(description = "사용자 발화 데이터")
    public record UserTurnPayload(
            @Schema(description = "음성 인식 텍스트", example = "Hello, I want to practice English.")
            String text,

            @Schema(description = "오디오 샘플레이트", example = "16000")
            Integer sampleRate,

            @Schema(description = "S3에 저장된 오디오 파일 키", example = "calls/CA539d3f817a9f5a5cb8f61487d63ebd22/turns/5.wav")
            String audioKey
    ) {
    }

    @Schema(description = "어시스턴트 발화 데이터")
    public record AssistantTurnPayload(
            @Schema(description = "어시스턴트 응답 텍스트", example = "Sure, what seems to be the issue?")
            String text,

            @Schema(description = "어시스턴트 응답 한국어 번역", example = "물론이죠, 무슨 문제인가요?")
            String translatedText,

            @Schema(description = "응답 생성 시각", example = "2026-05-20T12:00:02.234567+00:00")
            OffsetDateTime createdAt
    ) {
    }
}
