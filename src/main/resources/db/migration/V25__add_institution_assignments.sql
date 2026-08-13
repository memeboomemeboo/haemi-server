CREATE TABLE institution_assignments (
    id UUID PRIMARY KEY,
    elder_id UUID NOT NULL,
    institution_id VARCHAR(100) NOT NULL,
    institution_admin_member_id UUID NOT NULL,
    assigned_by_member_id UUID NOT NULL,
    active BOOLEAN NOT NULL,
    assigned_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6),
    CONSTRAINT uk_institution_assignments_elder_admin
        UNIQUE (elder_id, institution_admin_member_id),
    CONSTRAINT fk_institution_assignments_elder
        FOREIGN KEY (elder_id) REFERENCES elders(id)
);

CREATE INDEX idx_institution_assignments_admin_active
    ON institution_assignments (institution_admin_member_id, active);
