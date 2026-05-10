package com.molla.controller;

import com.molla.common.response.ApiResponse;
import com.molla.controller.dto.user.UpdateUserRequest;
import com.molla.controller.dto.user.UserResponse;
import com.molla.domain.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "유저 API — 내 정보 조회/수정/탈퇴")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "JWT로 인증된 유저의 정보를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "유저 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/api/v1/users/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        String userId = getCurrentUserId();
        UserResponse response = userService.getMe(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "내 정보 수정",
            description = "닉네임, 영어 레벨을 수정합니다. null로 보낸 필드는 수정하지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PatchMapping("/api/v1/users/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @RequestBody @Valid UpdateUserRequest request
    ) {
        String userId = getCurrentUserId();
        UserResponse response = userService.updateMe(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "유저 status를 withdrawn으로 변경하고 Refresh Token을 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @DeleteMapping("/api/v1/users/me")
    public ResponseEntity<ApiResponse<Void>> withdraw() {
        String userId = getCurrentUserId();
        userService.withdraw(userId);
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (String) authentication.getPrincipal();
    }
}
