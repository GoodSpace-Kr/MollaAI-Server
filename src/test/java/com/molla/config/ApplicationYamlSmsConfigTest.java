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
}
