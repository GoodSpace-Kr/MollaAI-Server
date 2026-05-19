package com.molla.domain.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.domain.feedbackreport.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiClient(
            @Qualifier("openAiWebClient") WebClient webClient,
            ObjectMapper objectMapper,
            @Value("${openai.model}") String model
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    public Report generateReport(String transcript, String sessionType) {
        String systemPrompt = """
                당신은 영어 학습 코치입니다. 아래 통화 대화록을 분석해서 반드시 아래 JSON 형식으로만 응답하세요.
                다른 텍스트는 절대 포함하지 마세요.
                coreSentences는 반드시 여러 문장으로 구성하세요. 최소 3개 이상 작성하고, 각 항목은 transcript에서 실제로 중요한 문장을 골라
                sentence, grammarCorrection, improvedSentence를 1:1:1로 대응시켜 주세요.
                coreSentences의 각 sentence는 서로 다른 문장이어야 하며, 같은 문장을 중복해서 넣지 마세요.
                
                {
                  "oneLineSummary": "한 줄 요약",
                  "coreSentences": [
                    {"sentence": "", "grammarCorrection": "", "improvedSentence": ""},
                    {"sentence": "", "grammarCorrection": "", "improvedSentence": ""},
                    {"sentence": "", "grammarCorrection": "", "improvedSentence": ""}
                  ],
                  "habitAnalyses": [{"habit": "", "evidence": "", "suggestion": ""}],
                  "scores": [{"exam": "IELTS", "score": ""}, {"exam": "TOEIC", "score": ""}, {"exam": "OPIC", "score": ""}],
                  "levelResult": "레벨 테스트일 때만 '상위 N%' 형식으로, 아니면 null",
                  "weakPoints": ["약점1", "약점2"]
                }
                """;

        String userPrompt = "세션 타입: " + sessionType + "\n\n대화록:\n" + transcript;
        return parseReport(callChatApi(systemPrompt, userPrompt));
    }

    public List<Float> createEmbedding(String text, String embeddingModel) {
        Map<String, Object> body = Map.of(
                "model", embeddingModel,
                "input", text
        );

        try {
            String response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode dataArray = root.path("data");

            if (!dataArray.isArray() || dataArray.isEmpty()) {
                throw new RuntimeException("임베딩 응답 data 배열이 비어있음");
            }

            JsonNode embeddingArray = dataArray.get(0).path("embedding");
            return objectMapper.readerForListOf(Float.class).readValue(embeddingArray);

        } catch (Exception e) {
            throw new RuntimeException("임베딩 생성 실패: " + e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────

    private String callChatApi(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3,
                "response_format", Map.of("type", "json_object")
        );

        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);

            // null-safe 파싱 — choices 배열 존재 여부 및 길이 확인
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.error("OpenAI 응답에 choices 없음. 응답: {}", response);
                throw new RuntimeException("OpenAI 응답 파싱 실패: choices 없음");
            }

            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                log.error("OpenAI 응답에 content 없음. 응답: {}", response);
                throw new RuntimeException("OpenAI 응답 파싱 실패: content 없음");
            }

            return content.asText();

        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI API 호출 실패: " + e.getMessage(), e);
        }
    }

    private Report parseReport(String reportJson) {
        try {
            return objectMapper.readValue(reportJson, Report.class);
        } catch (Exception e) {
            log.error("리포트 JSON 파싱 실패: {}", e.getMessage(), e);
            throw new RuntimeException("리포트 JSON 파싱 실패: " + e.getMessage(), e);
        }
    }
}
