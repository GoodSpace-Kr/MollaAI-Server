package com.molla.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationYamlSmsConfigTest {

    @Test
    void usesApprovedNaverSensFromNumberAsDefault() throws Exception {
        String source = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(source).contains("from-number: ${NAVER_SENS_FROM_NUMBER:01057807344}");
    }

    @Test
    void prefersDedicatedS3CredentialsForAudioPresign() throws Exception {
        String source = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(source).contains("access-key-id: ${AWS_ACCESS_KEY_ID:${AWS_S3_ACCESS_KEY:}}");
        assertThat(source).contains("secret-access-key: ${AWS_SECRET_ACCESS_KEY:${AWS_S3_SECRET_KEY:}}");
        assertThat(source).contains("access-key-id: ${AWS_S3_ACCESS_KEY:}");
        assertThat(source).contains("secret-access-key: ${AWS_S3_SECRET_KEY:}");
    }

    @Test
    void usesStaticAgentTokenAndCloudflareRealtimeConfig() throws Exception {
        String source = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(source).contains("token: ${AI_AGENT_TOKEN:}");
        assertThat(source).contains("api-base: ${CLOUDFLARE_REALTIME_API_BASE:https://rtc.live.cloudflare.com/v1}");
        assertThat(source).contains("app-id: ${CLOUDFLARE_REALTIME_APP_ID:}");
        assertThat(source).contains("api-token: ${CLOUDFLARE_API_TOKEN:}");
        assertThat(source).doesNotContain("APP_REALTIME_WSS_URL");
        assertThat(source).doesNotContain("ORCHESTRATOR_WSS_URL");
        assertThat(source).doesNotContain("JWT_AGENT_SECRET");
    }
}
