package com.molla.domain.chatmessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.common.response.ErrorCode;
import com.molla.domain.usermemory.UserMemory;
import com.molla.domain.usermemory.UserMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ChatAiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public ChatAiClient(
            @Qualifier("openAiWebClient") WebClient webClient,
            ObjectMapper objectMapper,
            @Value("${openai.model}") String model
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    public String generateReply(
            List<ChatMessage> history,
            String username,
            UserMemory memory
    ) {
        String systemPrompt = buildSystemPrompt(username, memory);
        List<Map<String, String>> messages = buildMessages(systemPrompt, history);

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", messages,
                    "temperature", 0.7,
                    "max_tokens", 1000
            );

            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.error("ChatAI choices 없음. 응답: {}", response);
                throw new ChatMessageException(ErrorCode.INTERNAL_SERVER_ERROR);
            }

            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                log.error("ChatAI content 없음. 응답: {}", response);
                throw new ChatMessageException(ErrorCode.INTERNAL_SERVER_ERROR);
            }

            return content.asText();

        } catch (ChatMessageException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 채팅 응답 생성 실패: {}", e.getMessage(), e);
            throw new ChatMessageException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /** user 발화 3턴마다 채팅 내용 → user_memories 업데이트 */
    public void extractAndUpdateMemory(
            String userId,
            List<ChatMessage> history,
            String latestAiReply,
            UserMemoryService userMemoryService
    ) {
        long userTurnCount = history.stream()
                .filter(m -> "user".equals(m.getSender()))
                .count();

        if (userTurnCount == 0 || userTurnCount % 3 != 0) return;

        String systemPrompt = """
                아래 채팅 대화에서 학습자에 대한 중요한 정보를 추출하세요.
                반드시 아래 JSON 형식으로만 응답하세요. 변경 없는 필드는 null로 반환하세요.
                
                {
                  "summary": "학습자 전체 요약 (변경사항 있으면 업데이트, 없으면 null)",
                  "weakPoints": ["새로 발견된 약점들 (없으면 빈 배열)"],
                  "habitPatterns": ["새로 발견된 습관 패턴들 (없으면 빈 배열)"],
                  "interests": ["새로 발견된 관심사들 (없으면 빈 배열)"],
                  "goals": "새로 파악된 학습 목표 (없으면 null)"
                }
                """;

        int start = Math.max(0, history.size() - 10);
        StringBuilder chatSummary = new StringBuilder();
        for (ChatMessage msg : history.subList(start, history.size())) {
            chatSummary.append("user".equals(msg.getSender()) ? "[학습자] " : "[코치] ");
            chatSummary.append(msg.getContent()).append("\n");
        }
        chatSummary.append("[코치] ").append(latestAiReply);

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", "채팅 내용:\n" + chatSummary)
                    ),
                    "temperature", 0.3,
                    "max_tokens", 500,
                    "response_format", Map.of("type", "json_object")
            );

            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) return;

            String jsonContent = choices.get(0).path("message").path("content").asText();
            JsonNode extracted = objectMapper.readTree(jsonContent);

            userMemoryService.upsertMemory(
                    userId,
                    getTextOrNull(extracted, "summary"),
                    toJsonStringOrNull(extracted, "weakPoints"),
                    toJsonStringOrNull(extracted, "habitPatterns"),
                    toJsonStringOrNull(extracted, "interests"),
                    getTextOrNull(extracted, "goals"),
                    0
            );

            log.info("채팅 메모리 업데이트 — userId: {}, userTurnCount: {}", userId, userTurnCount);

        } catch (Exception e) {
            log.error("채팅 메모리 추출 실패: {}", e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────

    private String buildSystemPrompt(String username, UserMemory memory) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 친절한 영어 학습 코치입니다. ")
                .append("학습자의 질문에 한국어로 답변하되, 영어 예시나 교정은 영어로 작성하세요.\n");

        if (username != null && !username.isBlank()) {
            prompt.append("학습자의 이름은 ").append(username)
                    .append("입니다. 대화 중 자연스럽게 이름을 불러주세요.\n");
        }

        if (memory != null) {
            prompt.append("\n[학습자 개인화 정보]\n");
            if (memory.getSummary() != null)
                prompt.append("요약: ").append(memory.getSummary()).append("\n");
            if (memory.getWeakPoints() != null)
                prompt.append("약점: ").append(memory.getWeakPoints()).append("\n");
            if (memory.getHabitPatterns() != null)
                prompt.append("습관 패턴: ").append(memory.getHabitPatterns()).append("\n");
            if (memory.getInterests() != null)
                prompt.append("관심사: ").append(memory.getInterests()).append("\n");
            if (memory.getGoals() != null)
                prompt.append("학습 목표: ").append(memory.getGoals()).append("\n");
            prompt.append("위 정보를 바탕으로 개인화된 답변을 제공하세요.\n");
        }

        return prompt.toString();
    }

    private List<Map<String, String>> buildMessages(
            String systemPrompt,
            List<ChatMessage> history
    ) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        int start = Math.max(0, history.size() - 10);
        for (ChatMessage msg : history.subList(start, history.size())) {
            String role = "user".equals(msg.getSender()) ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", msg.getContent()));
        }

        return messages;
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNull() || value.isMissingNode() ? null : value.asText();
    }

    private String toJsonStringOrNull(JsonNode node, String field) {
        try {
            JsonNode value = node.path(field);
            if (value.isNull() || value.isMissingNode()) return null;
            if (value.isArray() && value.isEmpty()) return null;
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
