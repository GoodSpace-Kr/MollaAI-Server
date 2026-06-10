package com.molla.domain.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.domain.callsession.CallSession;
import com.molla.domain.callsession.CallSessionRepository;
import com.molla.domain.callsession.CallSessionTurn;
import com.molla.domain.callsession.SessionEndedEvent;
import com.molla.domain.feedbackreport.FeedbackReport;
import com.molla.domain.feedbackreport.FeedbackReportRepository;
import com.molla.domain.feedbackreport.Report;
import com.molla.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallSessionWorker {

    private static final int MINIMUM_REPORTABLE_DURATION_SECONDS = 180;

    private final CallSessionRepository callSessionRepository;
    private final FeedbackReportRepository feedbackReportRepository;
    private final UserRepository userRepository;
    private final OpenAiClient openAiClient;
    private final QdrantClient qdrantClient;
    private final ReportAudioEnricher reportAudioEnricher;
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

        if (isShortCompletedSession(session)) {
            log.info("짧은 통화 세션 스킵 — sessionId: {}, duration: {}초",
                    sessionId, session.getDurationSeconds());
            return;
        }

        List<CallSessionTurn> turns = readTurns(session.getTurnsJson());

        // ── Step 0: AI 발화 번역 ─────────────────────
        try {
            turns = translateAssistantTurns(turns, session);
            log.info("Step 0 완료 — AI 발화 번역, sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("Step 0 실패 — 번역 오류, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
            // 번역 실패해도 리포트 생성은 계속 진행
        }

        // ── Step 1: 리포트 생성 ──────────────────────
        Report reportData = null;
        FeedbackReport savedReport = null;

        try {
            if (turns.isEmpty()) {
                log.warn("통화 턴 없음 — 리포트 생성 스킵, sessionId: {}", sessionId);
            } else {
                reportData = openAiClient.generateReport(ReportTurnInput.from(turns), session.getSessionType());
                reportData = reportAudioEnricher.attachTurnAudio(reportData, turns);
                savedReport = saveReport(sessionId, session.getSessionType(), reportData);
                log.info("Step 1 완료 — 리포트 생성, sessionId: {}", sessionId);

                if (isLevelTest && userId != null && savedReport.getLevelResult() != null) {
                    updateUserEnglishLevel(userId, savedReport.getLevelResult());
                }
            }
        } catch (Exception e) {
            log.error("Step 1 실패 — 리포트 생성 오류, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
        }

        // ── Step 2: Qdrant upsert ────────────────────
        try {
            if (turns.isEmpty()) {
                log.info("통화 턴 없음 — Qdrant upsert 스킵, sessionId: {}", sessionId);
            } else {
                qdrantClient.upsertTurns(sessionId, userId, session.getPhoneNumber(), turns);
                log.info("Step 2 완료 — Qdrant upsert, sessionId: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Step 2 실패 — Qdrant upsert 오류, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
        }

        log.info("워커 완료 — sessionId: {}", sessionId);
    }

    // ──────────────────────────────────────────────
    // Step 0: AI 발화 번역 후 turns_json 업데이트
    // ──────────────────────────────────────────────

    @Transactional
    public List<CallSessionTurn> translateAssistantTurns(
            List<CallSessionTurn> turns,
            CallSession session
    ) throws Exception {
        // 번역이 필요한 assistant 텍스트만 추출
        List<String> textsToTranslate = turns.stream()
                .filter(t -> t.assistant() != null
                        && t.assistant().text() != null
                        && !t.assistant().text().isBlank()
                        && t.assistant().translatedText() == null) // 이미 번역된 건 스킵
                .map(t -> t.assistant().text())
                .toList();

        if (textsToTranslate.isEmpty()) {
            log.info("번역할 AI 발화 없음 — sessionId: {}", session.getId());
            return turns;
        }

        // OpenAI로 일괄 번역
        List<String> translations = openAiClient.translateTexts(textsToTranslate);

        // 번역 결과를 turns에 적용
        List<CallSessionTurn> translatedTurns = new ArrayList<>();
        int translationIndex = 0;

        for (CallSessionTurn turn : turns) {
            if (turn.assistant() != null
                    && turn.assistant().text() != null
                    && !turn.assistant().text().isBlank()
                    && turn.assistant().translatedText() == null
                    && translationIndex < translations.size()) {

                String translated = translations.get(translationIndex++);

                CallSessionTurn.AssistantTurn enrichedAssistant = new CallSessionTurn.AssistantTurn(
                        turn.assistant().text(),
                        translated,
                        turn.assistant().createdAt()
                );

                translatedTurns.add(new CallSessionTurn(
                        turn.index(),
                        turn.createdAt(),
                        turn.user(),
                        enrichedAssistant
                ));
            } else {
                translatedTurns.add(turn);
            }
        }

        // turns_json 업데이트 (DB 저장)
        session.updateTurnsJson(objectMapper.writeValueAsString(translatedTurns));
        callSessionRepository.save(session);

        log.info("번역 완료 — sessionId: {}, 번역 건수: {}", session.getId(), translationIndex);
        return translatedTurns;
    }

    // ──────────────────────────────────────────────
    // 기존 유틸 (변경 없음)
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
                reportData.levelPercentage(),
                reportData.levelAnalysis(),
                toJsonString(reportData.coreSentences()),
                toJsonString(reportData.habitAnalyses()),
                toJsonString(reportData.scores()),
                toJsonString(reportData.weakPoints()),
                reportData.levelResult()
        );

        return feedbackReportRepository.save(report);
    }

    private String toJsonString(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private List<CallSessionTurn> readTurns(String turnsJson) {
        if (turnsJson == null || turnsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(turnsJson, new TypeReference<List<CallSessionTurn>>() {});
        } catch (Exception e) {
            log.error("turns JSON 파싱 실패 — error: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private boolean isShortCompletedSession(CallSession session) {
        Integer durationSeconds = session.getDurationSeconds();
        return durationSeconds != null && durationSeconds < MINIMUM_REPORTABLE_DURATION_SECONDS;
    }
}
