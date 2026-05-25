ALTER TABLE feedback_reports
    ADD COLUMN level_percentage INT NULL AFTER one_line_summary,
    ADD COLUMN level_analysis TEXT NULL AFTER level_percentage;
