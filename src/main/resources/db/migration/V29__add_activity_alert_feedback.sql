ALTER TABLE cognitive_change_alerts
    ADD COLUMN false_positive_at TIMESTAMP;

CREATE INDEX idx_cognitive_alerts_false_positive
    ON cognitive_change_alerts (elder_id, false_positive_at);
