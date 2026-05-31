ALTER TABLE call_sessions
    ADD COLUMN turns_json LONGTEXT NULL AFTER duration_seconds;
