package com.molla.domain.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.domain.conversationturn.ConversationTurn;
import com.molla.domain.usermemory.UserMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public String generateReport(List<ConversationTurn> turns, String sessionType) {
        String transcript = buildTranscript(turns);

        String systemPrompt = """
                당신은 영어 학습 코치입니다. 아래 통화 대화록을 분석해서 반드시 아래 JSON 형식으로만 응답하세요.
                다른 텍스트는 절대 포함하지 마세요.
                
                {
                  "oneLineSummary": "한 줄 요약",
                  "grammarCorrections": [{"original": "", "corrected": "", "explanation": ""}],
                  "vocabularySuggestions": [{"used": "", "better": "", "reason": ""}],
                  "habitAnalysis": [{"pattern": "", "example": "", "suggestion": ""}],
                  "pronunciationNotes": "발음 피드백",
                  "overallScore": 0.0,
                  "levelResult": "레벨 테스트일 때만 '상위 N%' 형식으로, 아니면 null",
                  "weakPoints": ["약점1", "약점2"],
                  "habitPatterns": ["패턴1", "패턴2"],
                  "interests": ["관심사1", "관심사2"],
                  "goals": "대화에서 파악된 학습 목표"
                }
                """;

        String userPrompt = "세션 타입: " + sessionType + "\n\n대화록:\n" + transcript;
        return callChatApi(systemPrompt, userPrompt);
    }

    public String generateMemorySummary(UserMemory existingMemory, String reportJson) {
        String existingMemoryJson = existingMemory != null
                ? buildExistingMemoryJson(existingMemory)
                : "{}";

        String systemPrompt = """
                당신은 영어 학습 코치입니다. 기존 학습자 메모리와 이번 통화 리포트를 합쳐서
                업데이트된 메모리를 반드시 아래 JSON 형식으로만 응답하세요.
                다른 텍스트는 절대 포함하지 마세요.
                
                {
                  "summary": "학습자 전체 요약",
                  "weakPoints": ["약점1", "약점2"],
                  "habitPatterns": ["패턴1", "패턴2"],
                  "interests": ["관심사1", "관심사2"],
                  "goals": "학습 목표"
                }
                """;

        String userPrompt = "기존 메모리:\n" + existingMemoryJson
                + "\n\n이번 통화 리포트:\n" + reportJson;

        return callChatApi(systemPrompt, userPrompt);
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

    private String buildTranscript(List<ConversationTurn> turns) {
        return turns.stream()
                .map(t -> "[" + t.getSpeaker().toUpperCase() + "] " + t.getContent())
                .collect(Collectors.joining("\n"));
    }

    private String buildExistingMemoryJson(UserMemory memory) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "summary", memory.getSummary() != null ? memory.getSummary() : "",
                    "weakPoints", memory.getWeakPoints() != null ? memory.getWeakPoints() : "[]",
                    "habitPatterns", memory.getHabitPatterns() != null ? memory.getHabitPatterns() : "[]",
                    "interests", memory.getInterests() != null ? memory.getInterests() : "[]",
                    "goals", memory.getGoals() != null ? memory.getGoals() : ""
            ));
        } catch (Exception e) {
            return "{}";
        }
    }
}
