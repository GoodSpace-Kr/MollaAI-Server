package com.molla.domain.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.domain.callsession.CallSession;
import com.molla.domain.callsession.CallSessionRepository;
import com.molla.domain.callsession.SessionEndedEvent;
import com.molla.domain.feedbackreport.FeedbackReport;
import com.molla.domain.feedbackreport.FeedbackReportRepository;
import com.molla.domain.feedbackreport.Report;
import com.molla.domain.user.UserRepository;
import com.molla.domain.usermemory.UserMemory;
import com.molla.domain.usermemory.UserMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallSessionWorker {

    private final CallSessionRepository callSessionRepository;
    private final FeedbackReportRepository feedbackReportRepository;
    private final UserMemoryService userMemoryService;
    private final UserRepository userRepository;
    private final OpenAiClient openAiClient;
    private final QdrantClient qdrantClient;
    private final ObjectMapper objectMapper;

    @Async("workerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processAfterCall(SessionEndedEvent event) {
        String sessionId = event.getSessionId();
        String userId = event.getUserId();
        boolean isLevelTest = event.isLevelTest();

        log.info("워커 시작 — sessionId: {}, userId: {}, isLevelTest: {}", sessionId, userId, isLevelTest);

        CallSession session = callSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            log.error("세션 없음 — 워커 종료, sessionId: {}", sessionId);
            return;
        }

        String transcript = session.getTranscript();

        // ── Step 1: 리포트 생성 ──────────────────────
        Report reportData = null;
        FeedbackReport savedReport = null;

        try {
            if (transcript == null || transcript.isBlank()) {
                log.warn("통화 전문 없음 — 리포트 생성 스킵, sessionId: {}", sessionId);
            } else {
                reportData = openAiClient.generateReport(transcript, session.getSessionType());
                savedReport = saveReport(sessionId, session.getSessionType(), reportData);
                log.info("Step 1 완료 — 리포트 생성, sessionId: {}", sessionId);

                // level_test 통화 완료 시 english_level 자동 업데이트
                if (isLevelTest && userId != null && savedReport.getLevelResult() != null) {
                    updateUserEnglishLevel(userId, savedReport.getLevelResult());
                }
            }
        } catch (Exception e) {
            log.error("Step 1 실패 — 리포트 생성 오류, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
        }

        // ── Step 2: user_memories 갱신 ──────────────
        try {
            if (reportData != null && userId != null) {
                UserMemory existingMemory = userMemoryService.getMemory(userId);
                String memorySummaryJson = openAiClient.generateMemorySummary(existingMemory, objectMapper.writeValueAsString(reportData));
                upsertUserMemory(userId, memorySummaryJson, session.getDurationSeconds());
                log.info("Step 2 완료 — user_memories 갱신, userId: {}", userId);
            } else if (reportData != null) {
                log.info("userId 없음 — user_memories 갱신 스킵, sessionId: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Step 2 실패 — user_memories 갱신 오류, userId: {}, error: {}", userId, e.getMessage(), e);
        }

        // ── Step 3: Qdrant upsert ────────────────────
        try {
            if (transcript == null || transcript.isBlank()) {
                log.info("통화 전문 없음 — Qdrant upsert 스킵, sessionId: {}", sessionId);
            } else {
                qdrantClient.upsertTranscript(sessionId, userId, session.getPhoneNumber(), transcript);
                log.info("Step 3 완료 — Qdrant upsert, sessionId: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Step 3 실패 — Qdrant upsert 오류, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
        }

        log.info("워커 완료 — sessionId: {}", sessionId);
    }

    // ──────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────

    @Transactional
    public void updateUserEnglishLevel(String userId, String levelResult) {
        userRepository.findById(userId).ifPresent(user -> {
            user.updateEnglishLevel(levelResult);
            log.info("english_level 업데이트 — userId: {}, level: {}", userId, levelResult);
        });
    }

    private FeedbackReport saveReport(String sessionId, String sessionType, Report reportData) throws Exception {
        FeedbackReport existingReport = feedbackReportRepository.findBySessionId(sessionId).orElse(null);
        if (existingReport != null) {
            log.info("기존 리포트 재사용 — sessionId: {}", sessionId);
            return existingReport;
        }

        FeedbackReport report = FeedbackReport.create(
                sessionId,
                sessionType,
                reportData.oneLineSummary(),
                toJsonString(reportData.coreSentences()),
                toJsonString(reportData.habitAnalyses()),
                toJsonString(reportData.scores()),
                toJsonString(reportData.weakPoints()),
                reportData.levelResult()
        );

        return feedbackReportRepository.save(report);
    }

    private void upsertUserMemory(String userId, String memorySummaryJson, Integer durationSeconds) throws Exception {
        JsonNode node = objectMapper.readTree(memorySummaryJson);
        int addedMinutes = durationSeconds != null ? (int) Math.ceil(durationSeconds / 60.0) : 0;

        userMemoryService.upsertMemory(
                userId,
                getTextOrNull(node, "summary"),
                toJsonString(node, "weakPoints"),
                toJsonString(node, "habitPatterns"),
                toJsonString(node, "interests"),
                getTextOrNull(node, "goals"),
                addedMinutes
        );
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNull() || value.isMissingNode() ? null : value.asText();
    }

    private String toJsonString(JsonNode node, String field) throws Exception {
        JsonNode value = node.path(field);
        if (value.isNull() || value.isMissingNode()) return null;
        return objectMapper.writeValueAsString(value);
    }

    private String toJsonString(Object value) throws Exception {
        if (value == null) return null;
        return objectMapper.writeValueAsString(value);
    }
}
