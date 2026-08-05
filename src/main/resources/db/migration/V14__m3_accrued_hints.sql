-- #41 F3-03 사전 적립형 손주 한마디
CREATE TABLE accrued_hints (
    id UUID PRIMARY KEY,
    elder_id VARCHAR(255) NOT NULL,
    photo_id UUID,
    person_name VARCHAR(255),
    source VARCHAR(255) NOT NULL,
    author_member_id VARCHAR(255) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    hint_text VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_accrued_hints_photo
    ON accrued_hints (elder_id, photo_id, active, created_at);
CREATE INDEX idx_accrued_hints_general
    ON accrued_hints (elder_id, active, created_at);
