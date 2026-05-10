package com.molla.controller.dto.chatmessage;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "채팅 메시지 전송 요청")
public record SendMessageRequest(

        @Schema(description = "메시지 내용", example = "오늘 공부할 표현 추천해줘")
        @NotBlank(message = "메시지 내용을 입력해주세요.")
        @Size(max = 2000, message = "메시지는 2000자 이하여야 합니다.")
        String content
) {}
