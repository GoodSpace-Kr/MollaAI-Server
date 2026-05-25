package com.molla.domain.worker;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiClientPromptTest {

    @Test
    void reportPromptRequiresMultipleCoreSentences() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/molla/domain/worker/OpenAiClient.java"));

        assertThat(source).contains("\"levelPercentage\":");
        assertThat(source).contains("\"levelAnalysis\":");
        assertThat(source).contains("\"originSentence\":");
        assertThat(source).contains("\"keyExpression\":");
        assertThat(source).contains("weakPoints는 반드시 1개 이상 3개 이하");
    }
}
