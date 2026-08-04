CREATE TABLE memory (
    memory_id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    author_user_id UUID NOT NULL,
    text_content VARCHAR(500),
    author_name VARCHAR(50) NOT NULL,
    author_relation VARCHAR(30) NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    moderation_status VARCHAR(20) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_memory_group FOREIGN KEY (group_id) REFERENCES family_groups(id)
);

CREATE INDEX idx_memory_group_created ON memory(group_id, created_at DESC);
CREATE INDEX idx_memory_elder_visibility ON memory(group_id, visibility, moderation_status, created_at DESC);

CREATE TABLE memory_media (
    id UUID PRIMARY KEY,
    memory_id UUID NOT NULL,
    type VARCHAR(10) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    thumb_key VARCHAR(255),
    duration_ms BIGINT,
    display_order INTEGER NOT NULL,
    CONSTRAINT fk_memory_media_memory FOREIGN KEY (memory_id) REFERENCES memory(memory_id)
);
