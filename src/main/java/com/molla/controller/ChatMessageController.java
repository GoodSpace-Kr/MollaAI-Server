package com.molla.controller;

import com.molla.common.response.ApiResponse;
import com.molla.controller.dto.chatmessage.ChatExchangeResponse;
import com.molla.controller.dto.chatmessage.ChatMessageResponse;
import com.molla.controller.dto.chatmessage.SendMessageRequest;
import com.molla.domain.chatmessage.ChatMessageService;
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

import java.util.List;

@Tag(name = "ChatMessage", description = "채팅 API — AI 코치와 자유 채팅")
@RestController
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @Operation(
            summary = "채팅 목록 조회",
            description = "JWT로 인증된 유저의 전체 채팅 내역을 시간순으로 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ChatMessageResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/api/v1/chat")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getAllMessages() {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatMessageService.getAllMessages(userId)));
    }

    @Operation(
            summary = "채팅 메시지 전송",
            description = """
                    AI 코치에게 메시지를 보내고 응답을 받습니다.
                    - 통화와 무관하게 언제든 사용 가능합니다.
                    - 개인화 메모리(user_memories)를 항상 참고합니다.
                    - user 발화 3턴마다 채팅 내용을 메모리에 자동 반영합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "전송 및 AI 응답 성공",
                    content = @Content(schema = @Schema(implementation = ChatExchangeResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/api/v1/chat")
    public ResponseEntity<ApiResponse<ChatExchangeResponse>> sendMessage(
            @RequestBody @Valid SendMessageRequest request
    ) {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatMessageService.sendMessage(userId, request)));
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
