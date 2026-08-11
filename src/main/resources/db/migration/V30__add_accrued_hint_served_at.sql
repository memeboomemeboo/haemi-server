ALTER TABLE accrued_hints
    ADD COLUMN last_served_at TIMESTAMP;

CREATE INDEX idx_accrued_hints_photo_reuse
    ON accrued_hints (elder_id, photo_id, active, last_served_at);
