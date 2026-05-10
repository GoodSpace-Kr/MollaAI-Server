package com.molla.controller;

import com.molla.common.response.ApiResponse;
import com.molla.controller.dto.auth.AccessTokenResponse;
import com.molla.controller.dto.auth.RefreshTokenRequest;
import com.molla.controller.dto.auth.RegisterRequest;
import com.molla.controller.dto.auth.SendCodeRequest;
import com.molla.controller.dto.auth.TokenResponse;
import com.molla.controller.dto.auth.VerifyCodeRequest;
import com.molla.controller.dto.user.UserResponse;
import com.molla.domain.auth.AuthService;
import com.molla.domain.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API — SMS 인증 및 JWT 발급")
@RestController
@RequiredArgsConstructor
@SecurityRequirements
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(
            summary = "인증번호 SMS 발송",
            description = "입력한 전화번호로 6자리 인증번호를 SMS 발송합니다. 유효 시간은 5분이며, 재요청 시 기존 코드는 무효 처리됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발송 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "전화번호 형식 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500", description = "SMS 발송 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/api/v1/auth/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(
            @RequestBody @Valid SendCodeRequest request
    ) {
        authService.sendVerificationCode(request.phoneNumber());
        return ResponseEntity.ok(ApiResponse.success("인증번호가 발송되었습니다.", null));
    }

    @Operation(
            summary = "인증번호 확인 + JWT 발급",
            description = """
                    인증번호를 확인하고 JWT를 발급합니다.
                    - 신규 유저: `isNewUser: true` → 프론트에서 이름 입력창 표시 → `/api/v1/auth/register` 호출
                    - 기존 유저: `isNewUser: false` → 바로 로그인 완료
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "인증 성공 + JWT 발급",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "인증번호 불일치 또는 만료",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/api/v1/auth/verify-code")
    public ResponseEntity<ApiResponse<TokenResponse>> verifyCode(
            @RequestBody @Valid VerifyCodeRequest request
    ) {
        TokenResponse token = authService.verifyCode(request.phoneNumber(), request.code());
        return ResponseEntity.ok(ApiResponse.success(token));
    }

    @Operation(
            summary = "회원가입 — 이름 등록",
            description = """
                    신규 유저가 이름을 등록하고 가입을 완료합니다.
                    - `verify-code`에서 `isNewUser: true` 받은 경우에만 호출
                    - Access Token 필요 (verify-code에서 발급받은 토큰)
                    - 이미 가입된 유저가 재요청하면 409 반환
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "가입 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "이미 가입된 유저",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @io.swagger.v3.oas.annotations.security.SecurityRequirements(
            value = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @PostMapping("/api/v1/auth/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @RequestBody @Valid RegisterRequest request
    ) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserResponse response = userService.register(userId, request);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    @Operation(
            summary = "Access Token 재발급",
            description = "만료된 Access Token을 Refresh Token으로 재발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "재발급 성공",
                    content = @Content(schema = @Schema(implementation = AccessTokenResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Refresh Token 무효 또는 만료",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/api/v1/auth/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refresh(
            @RequestBody @Valid RefreshTokenRequest request
    ) {
        AccessTokenResponse token = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(token));
    }
}
