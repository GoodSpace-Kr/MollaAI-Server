package com.molla.controller.dto.callsession;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "통화 세션 종료 요청 (내부 API — AI 오케스트레이션 서버 전용)")
public record EndSessionRequest(

        @Schema(description = "종료 상태 (completed / failed). 기본값: completed", example = "completed")
        String status
) {
    public String resolvedStatus() {
        return (status != null && "failed".equals(status)) ? "failed" : "completed";
    }
}
