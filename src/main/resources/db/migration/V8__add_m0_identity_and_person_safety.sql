CREATE TABLE family_groups (
    id UUID PRIMARY KEY,
    owner_member_id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    member_count INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    owner_hold_until TIMESTAMP(6),
    CONSTRAINT ck_family_groups_member_count CHECK (member_count BETWEEN 0 AND 10)
);

CREATE TABLE family_group_members (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    member_id UUID NOT NULL,
    relation VARCHAR(30) NOT NULL,
    role VARCHAR(20) NOT NULL,
    notification_preference VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP(6) NOT NULL,
    removed_at TIMESTAMP(6),
    CONSTRAINT uk_family_group_member UNIQUE (group_id, member_id),
    CONSTRAINT fk_family_group_members_group FOREIGN KEY (group_id) REFERENCES family_groups(id)
);

CREATE INDEX idx_family_group_members_member ON family_group_members(member_id);

CREATE TABLE invitations (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    inviter_member_id UUID NOT NULL,
    invitee_phone_hash VARCHAR(64) NOT NULL,
    relation VARCHAR(30) NOT NULL,
    token VARCHAR(96) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    accepted_at TIMESTAMP(6),
    CONSTRAINT uk_invitations_token UNIQUE (token),
    CONSTRAINT fk_invitations_group FOREIGN KEY (group_id) REFERENCES family_groups(id)
);

CREATE INDEX idx_invitations_group_status ON invitations(group_id, status);

CREATE TABLE ownership_transfers (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    requested_by_member_id UUID NOT NULL,
    recipient_member_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    accepted_at TIMESTAMP(6),
    CONSTRAINT fk_ownership_transfers_group FOREIGN KEY (group_id) REFERENCES family_groups(id)
);

CREATE TABLE elders (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    org_id VARCHAR(100),
    name VARCHAR(10) NOT NULL,
    birth_year INTEGER NOT NULL,
    gender VARCHAR(20) NOT NULL,
    residence_type VARCHAR(30) NOT NULL,
    access_mode VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    personalization_level INTEGER NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_elders_group UNIQUE (group_id),
    CONSTRAINT ck_elders_birth_year CHECK (birth_year BETWEEN 1920 AND 1970),
    CONSTRAINT ck_elders_personalization_level CHECK (personalization_level BETWEEN 1 AND 5),
    CONSTRAINT fk_elders_group FOREIGN KEY (group_id) REFERENCES family_groups(id)
);

CREATE TABLE elder_health (
    elder_id UUID PRIMARY KEY,
    diagnosis_encrypted TEXT NOT NULL,
    consent_id VARCHAR(100) NOT NULL,
    diagnosed_at DATE,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_elder_health_elder FOREIGN KEY (elder_id) REFERENCES elders(id)
);

CREATE TABLE life_stories (
    id UUID PRIMARY KEY,
    elder_id UUID NOT NULL,
    category VARCHAR(30) NOT NULL,
    story_value VARCHAR(500) NOT NULL,
    weight INTEGER NOT NULL,
    source VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_life_stories_elder FOREIGN KEY (elder_id) REFERENCES elders(id)
);

CREATE INDEX idx_life_stories_elder ON life_stories(elder_id);

CREATE TABLE sensitive_topics (
    id UUID PRIMARY KEY,
    elder_id UUID NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    reason VARCHAR(300),
    created_by_member_id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_sensitive_topics_elder_keyword UNIQUE (elder_id, keyword),
    CONSTRAINT fk_sensitive_topics_elder FOREIGN KEY (elder_id) REFERENCES elders(id)
);

CREATE TABLE persons (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    name VARCHAR(10) NOT NULL,
    relation VARCHAR(30) NOT NULL,
    life_status VARCHAR(20) NOT NULL,
    deceased_at DATE,
    visibility VARCHAR(20) NOT NULL,
    nickname VARCHAR(30),
    profile_photo_id UUID,
    linked_member_id UUID,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_persons_group FOREIGN KEY (group_id) REFERENCES family_groups(id)
);

CREATE INDEX idx_persons_group_visibility ON persons(group_id, visibility);

CREATE TABLE photo_persons (
    id UUID PRIMARY KEY,
    photo_id UUID NOT NULL,
    person_id UUID NOT NULL,
    confidence NUMERIC(3,2) NOT NULL,
    confirmed_by_member_id UUID,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_photo_persons_photo_person UNIQUE (photo_id, person_id),
    CONSTRAINT fk_photo_persons_person FOREIGN KEY (person_id) REFERENCES persons(id),
    CONSTRAINT ck_photo_persons_confidence CHECK (confidence BETWEEN 0 AND 1)
);

CREATE INDEX idx_photo_persons_photo ON photo_persons(photo_id);
