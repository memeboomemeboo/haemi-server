-- 어르신 앱 설정 저장 테이블
CREATE TABLE elder_display_settings (
    elder_id UUID PRIMARY KEY,
    font_size_level INTEGER NOT NULL DEFAULT 1,
    voice_feature_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT ck_elder_display_settings_font_size CHECK (font_size_level BETWEEN 1 AND 3)
);

-- 가족 추억글 댓글 테이블
CREATE TABLE memory_post_comments (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    member_id VARCHAR(100) NOT NULL,
    member_name VARCHAR(50) NOT NULL,
    relation VARCHAR(30) NOT NULL,
    content VARCHAR(200) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6)
);

CREATE INDEX idx_memory_post_comments_post ON memory_post_comments(post_id);
