package com.molla.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Bean(name = "openAiWebClient")
    public WebClient openAiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .codecs(config -> config
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024) // 2MB — 리포트 응답 대비
                )
                .build();
    }
}
