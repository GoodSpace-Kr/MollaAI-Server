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

    // JSON 컬럼 — String으로 저장, 애플리케이션에서 파싱
    @Column(name = "grammar_corrections", columnDefinition = "JSON")
    private String grammarCorrections;       // [{original, corrected, explanation}]

    @Column(name = "vocabulary_suggestions", columnDefinition = "JSON")
    private String vocabularySuggestions;    // [{used, better, reason}]

    @Column(name = "habit_analysis", columnDefinition = "JSON")
    private String habitAnalysis;            // [{pattern, example, suggestion}]

    @Column(name = "pronunciation_notes", columnDefinition = "TEXT")
    private String pronunciationNotes;

    @Column(name = "overall_score")
    private Float overallScore;              // 0~100

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
            String grammarCorrections,
            String vocabularySuggestions,
            String habitAnalysis,
            String pronunciationNotes,
            Float overallScore,
            String levelResult
    ) {
        FeedbackReport report = new FeedbackReport();
        report.id = UUID.randomUUID().toString();
        report.sessionId = sessionId;
        report.reportType = reportType;
        report.oneLineSummary = oneLineSummary;
        report.grammarCorrections = grammarCorrections;
        report.vocabularySuggestions = vocabularySuggestions;
        report.habitAnalysis = habitAnalysis;
        report.pronunciationNotes = pronunciationNotes;
        report.overallScore = overallScore;
        report.levelResult = levelResult;
        report.createdAt = LocalDateTime.now();
        return report;
    }
}
