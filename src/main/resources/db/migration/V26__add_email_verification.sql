ALTER TABLE members ADD COLUMN email_verified_at TIMESTAMP(6);
ALTER TABLE invitations DROP COLUMN invitee_phone_hash;
ALTER TABLE invitations ADD COLUMN invitee_email_hash VARCHAR(64) NOT NULL DEFAULT '';

CREATE TABLE email_verifications (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL,
    token VARCHAR(96) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    used_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_email_verifications_token UNIQUE (token),
    CONSTRAINT fk_email_verifications_member FOREIGN KEY (member_id) REFERENCES members(id)
);
CREATE INDEX idx_email_verifications_member ON email_verifications(member_id);
