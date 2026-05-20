package com.molla.domain.worker;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiClientPromptTest {

    @Test
    void reportPromptRequiresMultipleCoreSentences() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/molla/domain/worker/OpenAiClient.java"));

        assertThat(source).contains("coreSentences는 반드시 여러 문장으로 구성하세요. 최소 15개 이상 작성");
        assertThat(source).contains("sourceTurnIndex");
        assertThat(source).contains("\"coreSentences\": [");
    }
}
