package com.molla.domain.feedbackreport;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "feedback_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackReport {

    @Id
    @Column(length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "session_id", nullable = false, length = 36, unique = true, columnDefinition = "CHAR(36)")
    private String sessionId;

    @Column(name = "report_type", nullable = false, length = 20)
    private String reportType;               // level_test / practice

    @Column(name = "one_line_summary", columnDefinition = "TEXT")
    private String oneLineSummary;

    @Column(name = "core_sentences", columnDefinition = "JSON")
    private String coreSentences;            // [{sentence, grammarCorrection, improvedSentence}]

    @Column(name = "habit_analyses", columnDefinition = "JSON")
    private String habitAnalyses;            // [{habit, evidence, suggestion}]

    @Column(name = "scores", columnDefinition = "JSON")
    private String scores;                   // [{exam, score}]

    @Column(name = "weak_points", columnDefinition = "JSON")
    private String weakPoints;               // ["weak point 1", "weak point 2"]

    @Column(name = "level_result", length = 50)
    private String levelResult;              // 레벨테스트 시만 사용. "상위 23%"

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ──────────────────────────────────────────────
    // 정적 팩토리
    // ──────────────────────────────────────────────

    public static FeedbackReport create(
            String sessionId,
            String reportType,
            String oneLineSummary,
            String coreSentences,
            String habitAnalyses,
            String scores,
            String weakPoints,
            String levelResult
    ) {
        FeedbackReport report = new FeedbackReport();
        report.id = UUID.randomUUID().toString();
        report.sessionId = sessionId;
        report.reportType = reportType;
        report.oneLineSummary = oneLineSummary;
        report.coreSentences = coreSentences;
        report.habitAnalyses = habitAnalyses;
        report.scores = scores;
        report.weakPoints = weakPoints;
        report.levelResult = levelResult;
        report.createdAt = LocalDateTime.now();
        return report;
    }
}
