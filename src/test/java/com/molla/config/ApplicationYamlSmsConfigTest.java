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

        assertThat(source).contains("access-key-id: ${AWS_S3_ACCESS_KEY:${AWS_ACCESS_KEY_ID:}}");
        assertThat(source).contains("secret-access-key: ${AWS_S3_SECRET_KEY:${AWS_SECRET_ACCESS_KEY:}}");
    }
}
