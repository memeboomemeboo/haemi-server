CREATE TABLE institution_portal_audit_logs (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL,
    elder_id UUID,
    action VARCHAR(40) NOT NULL,
    allowed BOOLEAN NOT NULL,
    occurred_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_institution_portal_audit_logs_member_occurred
    ON institution_portal_audit_logs (member_id, occurred_at);
