package com.molla.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("https://mollatalk.com").description("Production (EC2)"),
                        new Server().url("http://localhost:8080").description("Local")
                ))
                // 전역 Security — 모든 엔드포인트에 Bearer 인증 적용
                // 인증 불필요 엔드포인트는 Controller에서 @SecurityRequirements({}) 로 override
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerSecurityScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Molla API")
                .description("""
                        ## AI 전화 영어 학습 서비스 — Molla 백엔드 API
                        
                        ### 인증
                        - 전화번호 기반 SMS 인증 후 JWT 발급
                        - `Authorization: Bearer {accessToken}` 헤더로 전달
                        - Access Token 만료(1시간) 후 `/api/v1/auth/refresh` 로 재발급
                        
                        ### API 구분
                        - **프론트 앱/웹 API** : 일반 JWT 인증 필요
                        - **내부 API** (`/api/v1/internal/**`) : AI 오케스트레이션 서버 전용, 인증 제외
                        """)
                .version("v1.0.0")
                .contact(new Contact().name("Molla Dev Team").email("dev@molla.ai"));
    }

    private SecurityScheme bearerSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name(SECURITY_SCHEME_NAME)
                .description("JWT Access Token. 형식: `Bearer {token}`");
    }
}
