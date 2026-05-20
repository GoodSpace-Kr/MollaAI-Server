ALTER TABLE feedback_reports
    ADD COLUMN core_sentences JSON NULL AFTER one_line_summary,
    ADD COLUMN habit_analyses JSON NULL AFTER core_sentences,
    ADD COLUMN scores JSON NULL AFTER habit_analyses,
    ADD COLUMN weak_points JSON NULL AFTER scores;

ALTER TABLE feedback_reports
    DROP COLUMN grammar_corrections,
    DROP COLUMN vocabulary_suggestions,
    DROP COLUMN habit_analysis,
    DROP COLUMN pronunciation_notes,
    DROP COLUMN overall_score;
