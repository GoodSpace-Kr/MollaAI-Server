ALTER TABLE call_sessions
    ADD COLUMN phone_number VARCHAR(20) NULL AFTER user_id;

UPDATE call_sessions cs
JOIN users u ON cs.user_id = u.id
SET cs.phone_number = u.phone_number
WHERE cs.phone_number IS NULL;

ALTER TABLE call_sessions
    MODIFY COLUMN user_id CHAR(36) NULL,
    MODIFY COLUMN phone_number VARCHAR(20) NOT NULL;
