package com.molla.controller;

import com.molla.common.response.ApiResponse;
import com.molla.config.JwtProvider;
import com.molla.controller.dto.auth.TokenResponse;
import com.molla.domain.user.User;
import com.molla.domain.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "Dev", description = "개발 전용 API — 운영 환경에서 비활성화")
@Slf4j
@RestController
@RequiredArgsConstructor
@Profile("!prod") // prod 프로파일에서는 자동 비활성화
public class DevController {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Operation(
            summary = "[개발 전용] SMS 인증 없이 바로 로그인",
            description = "전화번호만 입력하면 JWT를 바로 발급합니다. 운영 환경에서는 비활성화됩니다."
    )
    @PostMapping("/api/v1/dev/login")
    @Transactional
    public ResponseEntity<ApiResponse<TokenResponse>> devLogin(
            @RequestBody @Valid DevLoginRequest request
    ) {
        // 유저 없으면 자동 생성
        User user = userRepository.findByPhoneNumber(request.phoneNumber())
                .orElseGet(() -> userRepository.save(User.createByPhone(request.phoneNumber())));

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getPhoneNumber());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        user.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(30));

        log.warn("[DEV] 개발용 로그인 사용 — phoneNumber: {}", request.phoneNumber());

        boolean isNewUser = !user.isRegistered();
        return ResponseEntity.ok(ApiResponse.success(TokenResponse.of(accessToken, refreshToken, isNewUser)));
    }

    record DevLoginRequest(
            @NotBlank(message = "전화번호를 입력해주세요.")
            @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 전화번호 형식이 아닙니다.")
            String phoneNumber
    ) {}
}
