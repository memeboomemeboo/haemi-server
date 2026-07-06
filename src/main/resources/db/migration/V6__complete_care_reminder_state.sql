ALTER TABLE voice_alarms
    ADD COLUMN last_no_response_notified_at TIMESTAMP(6);

ALTER TABLE walk_routines
    ADD COLUMN last_reminded_at TIMESTAMP(6);
