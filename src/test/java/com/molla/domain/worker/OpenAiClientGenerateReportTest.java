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
                  "levelPercentage": 38,
                  "levelAnalysis": "중상위권 표현력은 보이지만 문장 정확도와 확장성이 더 필요합니다.",
                  "coreSentences": [
                    {
                      "sourceTurnIndex": 1,
                      "originSentence": "I go to there yesterday.",
                      "improvedSentence": "I went there yesterday to meet my friend.",
                      "keyExpression": "went there yesterday"
                    },
                    {
                      "sourceTurnIndex": 3,
                      "originSentence": "She don't like spicy food.",
                      "improvedSentence": "She doesn't like spicy food, so we chose another restaurant.",
                      "keyExpression": "doesn't like"
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

        var turns = java.util.List.of(
                new ReportTurnInput(1, "I go to there yesterday.", "What did you do there?"),
                new ReportTurnInput(2, "I meet my friend and we talk about work.", null),
                new ReportTurnInput(3, "She don't like spicy food.", "Understood.")
        );

        Report report = client.generateReport(turns, "practice");

        JsonNode requestRoot = objectMapper.readTree(capturedRequestBody.get());
        JsonNode userMessageJson = objectMapper.readTree(requestRoot.path("messages").get(1).path("content").asText());

        assertThat(requestRoot.path("model").asText()).isEqualTo("gpt-test");
        assertThat(requestRoot.path("response_format").path("type").asText()).isEqualTo("json_object");
        assertThat(userMessageJson.path("sessionType").asText()).isEqualTo("practice");
        assertThat(userMessageJson.path("turns")).hasSize(3);
        assertThat(userMessageJson.path("turns").get(2).path("index").asInt()).isEqualTo(3);
        assertThat(userMessageJson.path("turns").get(2).path("userText").asText()).isEqualTo("She don't like spicy food.");

        assertThat(report.oneLineSummary()).contains("전반적으로 자연스럽지만");
        assertThat(report.levelPercentage()).isEqualTo(38);
        assertThat(report.levelAnalysis()).contains("중상위권");
        assertThat(report.coreSentences()).hasSize(2);
        assertThat(report.coreSentences().get(0).sourceTurnIndex()).isEqualTo(1);
        assertThat(report.coreSentences().get(0).originSentence()).isEqualTo("I go to there yesterday.");
        assertThat(report.coreSentences().get(0).improvedSentence()).isEqualTo("I went there yesterday to meet my friend.");
        assertThat(report.coreSentences().get(0).keyExpression()).isEqualTo("went there yesterday");
        assertThat(report.habitAnalyses()).hasSize(1);
        assertThat(report.scores()).hasSize(3);
        assertThat(report.weakPoints()).contains("시제 정확도", "전치사 선택");
        assertThat(report.levelResult()).isNull();
    }
}
