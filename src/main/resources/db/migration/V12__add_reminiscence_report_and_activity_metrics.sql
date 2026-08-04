ALTER TABLE cognitive_daily_metrics
    ADD COLUMN voice_detected_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE cognitive_daily_metrics
    ADD COLUMN average_dwell_ms DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE cognitive_daily_metrics
    ADD COLUMN hint_playback_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE cognitive_daily_metrics
    ADD COLUMN hint_no_response_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE cognitive_daily_metrics
    ADD COLUMN top_memory_topic VARCHAR(100);
ALTER TABLE cognitive_daily_metrics
    ADD COLUMN top_dwelled_photo VARCHAR(255);

ALTER TABLE cognitive_reports
    ADD COLUMN report_mode VARCHAR(30) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE cognitive_reports
    ADD COLUMN days_together INTEGER NOT NULL DEFAULT 0;
ALTER TABLE cognitive_reports
    ADD COLUMN remembered_topics VARCHAR(1000);
ALTER TABLE cognitive_reports
    ADD COLUMN top_dwelled_photos VARCHAR(1000);
ALTER TABLE cognitive_reports
    ADD COLUMN voice_response_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE cognitive_reports
    ADD COLUMN family_contribution_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE cognitive_reports
    ADD COLUMN activity_message VARCHAR(1000);
