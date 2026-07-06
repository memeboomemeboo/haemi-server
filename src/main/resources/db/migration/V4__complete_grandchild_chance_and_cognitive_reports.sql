ALTER TABLE cognitive_training_sessions
    ADD COLUMN last_chance_status VARCHAR(255) NOT NULL DEFAULT 'NONE';

ALTER TABLE cognitive_training_sessions
    ADD COLUMN last_chance_question_id VARCHAR(255);

ALTER TABLE cognitive_training_sessions
    ADD COLUMN last_chance_requested_at TIMESTAMP(6);

ALTER TABLE cognitive_training_sessions
    ADD COLUMN chance_unused_completion_badge_awarded BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE cognitive_training_sessions
    ALTER COLUMN last_chance_status DROP DEFAULT;

ALTER TABLE cognitive_training_sessions
    ALTER COLUMN chance_unused_completion_badge_awarded DROP DEFAULT;

ALTER TABLE cognitive_reports
    ADD COLUMN reminiscence_participation_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE cognitive_reports
    ADD COLUMN most_reacted_photo_type VARCHAR(255);

ALTER TABLE cognitive_reports
    ADD COLUMN accuracy_change_from_previous DOUBLE PRECISION NOT NULL DEFAULT 0;

ALTER TABLE cognitive_reports
    ADD COLUMN response_time_change_from_previous DOUBLE PRECISION NOT NULL DEFAULT 0;

ALTER TABLE cognitive_reports
    ADD COLUMN delivery_method VARCHAR(255) NOT NULL DEFAULT 'IN_APP';

ALTER TABLE cognitive_reports
    ADD COLUMN viewed_at TIMESTAMP(6);

ALTER TABLE cognitive_reports
    ALTER COLUMN reminiscence_participation_count DROP DEFAULT;

ALTER TABLE cognitive_reports
    ALTER COLUMN accuracy_change_from_previous DROP DEFAULT;

ALTER TABLE cognitive_reports
    ALTER COLUMN response_time_change_from_previous DROP DEFAULT;

ALTER TABLE cognitive_reports
    ALTER COLUMN delivery_method DROP DEFAULT;

CREATE TABLE cognitive_report_accuracy_trend (
    report_id UUID NOT NULL,
    metric_date DATE NOT NULL,
    accuracy_rate DOUBLE PRECISION NOT NULL,
    trend_order INTEGER NOT NULL,
    CONSTRAINT pk_cognitive_report_accuracy_trend PRIMARY KEY (report_id, trend_order),
    CONSTRAINT fk_cognitive_report_accuracy_trend_report
        FOREIGN KEY (report_id) REFERENCES cognitive_reports(id)
);
