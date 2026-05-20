package com.molla.domain.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.domain.feedbackreport.Report;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiClientGenerateReportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generateReportSendsTranscriptAndParsesStructuredResponse() throws Exception {
        AtomicReference<String> capturedRequestBody = new AtomicReference<>();
        String reportJson = """
                {
                  "oneLineSummary": "전반적으로 자연스럽지만 시제와 전치사를 더 다듬으면 좋아요.",
                  "coreSentences": [
                    {
                      "sentence": "I go to there yesterday.",
                      "grammarCorrection": "I went there yesterday.",
                      "improvedSentence": "I went there yesterday to meet my friend."
                    },
                    {
                      "sentence": "She don't like spicy food.",
                      "grammarCorrection": "She doesn't like spicy food.",
                      "improvedSentence": "She doesn't like spicy food, so we chose another restaurant."
                    }
                  ],
                  "habitAnalyses": [
                    {
                      "habit": "짧은 문장 반복",
                      "evidence": "I like it. I use it. I want it.",
                      "suggestion": "접속사를 써서 문장을 조금 더 길게 연결해 보세요."
                    }
                  ],
                  "scores": [
                    {"exam": "IELTS", "score": "6.0"},
                    {"exam": "TOEIC", "score": "785"},
                    {"exam": "OPIC", "score": "IM2"}
                  ],
                  "weakPoints": ["시제 정확도", "전치사 선택", "문장 확장"],
                  "levelResult": null
                }
                """;

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            capturedRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            String responseBody = """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": %s
                          }
                        }
                      ]
                    }
                    """.formatted(objectMapper.writeValueAsString(reportJson));

            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        WebClient webClient = WebClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .build();
        OpenAiClient client = new OpenAiClient(webClient, objectMapper, "gpt-test");

        String transcript = """
                user: I go to there yesterday.
                assistant: What did you do there?
                user: I meet my friend and we talk about work.
                user: She don't like spicy food.
                """;

        Report report = client.generateReport(transcript, "practice");

        JsonNode requestRoot = objectMapper.readTree(capturedRequestBody.get());

        assertThat(requestRoot.path("model").asText()).isEqualTo("gpt-test");
        assertThat(requestRoot.path("response_format").path("type").asText()).isEqualTo("json_object");
        assertThat(requestRoot.path("messages").get(1).path("content").asText()).contains(transcript);
        assertThat(requestRoot.path("messages").get(1).path("content").asText()).contains("세션 타입: practice");

        assertThat(report.oneLineSummary()).contains("전반적으로 자연스럽지만");
        assertThat(report.coreSentences()).hasSize(2);
        assertThat(report.coreSentences().get(0).grammarCorrection()).isEqualTo("I went there yesterday.");
        assertThat(report.habitAnalyses()).hasSize(1);
        assertThat(report.scores()).hasSize(3);
        assertThat(report.weakPoints()).contains("시제 정확도", "전치사 선택");
        assertThat(report.levelResult()).isNull();
    }
}
