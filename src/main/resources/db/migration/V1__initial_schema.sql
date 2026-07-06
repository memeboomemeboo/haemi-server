CREATE TABLE albums (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP(6) NOT NULL,
    elder_profile_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(255) NOT NULL,
    owner_member_id VARCHAR(255) NOT NULL
);

CREATE TABLE members (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP(6) NOT NULL,
    email VARCHAR(255) NOT NULL,
    encoded_password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    refresh_token_hash VARCHAR(512),
    role VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    totp_secret VARCHAR(64),
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_members_email UNIQUE (email)
);

CREATE TABLE cognitive_change_alerts (
    id UUID PRIMARY KEY,
    album_id UUID,
    alert_type VARCHAR(255) NOT NULL,
    elder_id VARCHAR(255) NOT NULL,
    guide_link VARCHAR(255),
    message VARCHAR(1000) NOT NULL,
    sent_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE cognitive_daily_metrics (
    id UUID PRIMARY KEY,
    album_id UUID,
    average_response_seconds DOUBLE PRECISION NOT NULL,
    elder_id VARCHAR(255) NOT NULL,
    institution_id VARCHAR(255),
    memory_post_count INTEGER NOT NULL,
    metric_date DATE NOT NULL,
    most_reacted_photo_type VARCHAR(255),
    reminiscence_reaction_count INTEGER NOT NULL,
    training_accuracy_rate DOUBLE PRECISION NOT NULL,
    training_session_count INTEGER NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_cognitive_daily_metrics_elder_date UNIQUE (elder_id, metric_date)
);

CREATE TABLE cognitive_reports (
    id UUID PRIMARY KEY,
    album_id UUID,
    average_accuracy_rate DOUBLE PRECISION NOT NULL,
    average_response_seconds DOUBLE PRECISION NOT NULL,
    change_summary VARCHAR(1000),
    created_at TIMESTAMP(6) NOT NULL,
    elder_id VARCHAR(255) NOT NULL,
    memory_post_count INTEGER NOT NULL,
    participation_count INTEGER NOT NULL,
    pdf_key VARCHAR(255),
    period VARCHAR(255) NOT NULL,
    period_end DATE NOT NULL,
    period_start DATE NOT NULL
);

CREATE TABLE cognitive_training_sessions (
    id UUID PRIMARY KEY,
    album_id UUID NOT NULL,
    chance_used_count INTEGER NOT NULL,
    completed_at TIMESTAMP(6),
    current_question_index INTEGER NOT NULL,
    difficulty_level INTEGER NOT NULL,
    elder_id VARCHAR(255) NOT NULL,
    last_hint_responder VARCHAR(255),
    last_hint_text VARCHAR(500),
    session_date DATE NOT NULL,
    start_mode VARCHAR(255) NOT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    status VARCHAR(255) NOT NULL,
    CONSTRAINT uk_training_sessions_elder_date UNIQUE (elder_id, session_date)
);

CREATE TABLE difficulty_profiles (
    id UUID PRIMARY KEY,
    consecutive_correct INTEGER NOT NULL,
    consecutive_wrong INTEGER NOT NULL,
    current_level INTEGER NOT NULL,
    elder_id VARCHAR(255) NOT NULL,
    last_avg_response_seconds DOUBLE PRECISION,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_difficulty_profiles_elder UNIQUE (elder_id)
);

CREATE TABLE family_rankings (
    id UUID PRIMARY KEY,
    album_id UUID NOT NULL,
    computed_at TIMESTAMP(6),
    period VARCHAR(255) NOT NULL,
    period_end DATE NOT NULL,
    period_start DATE NOT NULL,
    CONSTRAINT uk_family_rankings_period UNIQUE (album_id, period, period_start)
);

CREATE TABLE memory_posts (
    id UUID PRIMARY KEY,
    album_id UUID NOT NULL,
    author_member_id VARCHAR(255) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    author_relation VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    reply_content VARCHAR(300),
    replied_at TIMESTAMP(6),
    reply_type VARCHAR(255),
    popularity_score INTEGER,
    published_at TIMESTAMP(6),
    is_read_by_elder BOOLEAN,
    status VARCHAR(255) NOT NULL,
    text_content VARCHAR(500),
    voice_memo_key VARCHAR(255)
);

CREATE TABLE photos (
    id UUID PRIMARY KEY,
    analysis_status VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    sha256_hash VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION,
    location_text VARCHAR(100),
    longitude DOUBLE PRECISION,
    memo VARCHAR(200),
    shot_at TIMESTAMP(6),
    time_period VARCHAR(50),
    uploaded_at TIMESTAMP(6) NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    album_id UUID NOT NULL,
    CONSTRAINT fk_photos_album FOREIGN KEY (album_id) REFERENCES albums(id)
);

CREATE TABLE reminiscence_contents (
    id UUID PRIMARY KEY,
    album_id UUID NOT NULL,
    elder_reaction VARCHAR(255),
    generated_at TIMESTAMP(6) NOT NULL,
    generated_date DATE NOT NULL
);

CREATE TABLE voice_alarms (
    id UUID PRIMARY KEY,
    active BOOLEAN NOT NULL,
    alarm_time TIME(0) NOT NULL,
    alarm_type VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    elder_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(255) NOT NULL,
    last_acknowledged_at TIMESTAMP(6),
    last_triggered_at TIMESTAMP(6),
    repeat_rule VARCHAR(255) NOT NULL,
    voice_key VARCHAR(255)
);

CREATE TABLE walk_routines (
    id UUID PRIMARY KEY,
    active BOOLEAN NOT NULL,
    afternoon_time TIME(0),
    created_at TIMESTAMP(6) NOT NULL,
    elder_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(255) NOT NULL,
    morning_time TIME(0),
    target_minutes INTEGER NOT NULL
);

CREATE TABLE walk_records (
    id UUID PRIMARY KEY,
    completed_at TIMESTAMP(6),
    duration_minutes INTEGER NOT NULL,
    elder_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(255) NOT NULL,
    routine_id UUID NOT NULL,
    started_at TIMESTAMP(6),
    status VARCHAR(255) NOT NULL,
    step_count INTEGER NOT NULL,
    walk_date DATE NOT NULL,
    weather_condition VARCHAR(255)
);

CREATE TABLE album_members (
    album_id UUID NOT NULL,
    member_id VARCHAR(255),
    CONSTRAINT fk_album_members_album FOREIGN KEY (album_id) REFERENCES albums(id)
);

CREATE TABLE photo_person_tags (
    photo_id UUID NOT NULL,
    member_id VARCHAR(255) NOT NULL,
    member_name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_photo_person_tags_photo FOREIGN KEY (photo_id) REFERENCES photos(id)
);

CREATE TABLE memory_post_likes (
    post_id UUID NOT NULL,
    member_id VARCHAR(255),
    CONSTRAINT fk_memory_post_likes_post FOREIGN KEY (post_id) REFERENCES memory_posts(id)
);

CREATE TABLE memory_post_photos (
    post_id UUID NOT NULL,
    photo_key VARCHAR(255),
    CONSTRAINT fk_memory_post_photos_post FOREIGN KEY (post_id) REFERENCES memory_posts(id)
);

CREATE TABLE ranking_entries (
    ranking_id UUID NOT NULL,
    awarded_stars INTEGER NOT NULL,
    badge_grade VARCHAR(255),
    entry_member_id VARCHAR(255) NOT NULL,
    entry_member_name VARCHAR(255) NOT NULL,
    entry_post_count INTEGER NOT NULL,
    entry_rank INTEGER NOT NULL,
    entry_reply_count INTEGER NOT NULL,
    entry_score INTEGER NOT NULL,
    CONSTRAINT fk_ranking_entries_ranking FOREIGN KEY (ranking_id) REFERENCES family_rankings(id)
);

CREATE TABLE member_star_summaries (
    ranking_id UUID NOT NULL,
    current_badge VARCHAR(255),
    star_member_id VARCHAR(255) NOT NULL,
    total_stars INTEGER NOT NULL,
    CONSTRAINT fk_star_summaries_ranking FOREIGN KEY (ranking_id) REFERENCES family_rankings(id)
);

CREATE TABLE reminiscence_questions (
    content_id UUID NOT NULL,
    question_text VARCHAR(500),
    question_sequence INTEGER,
    CONSTRAINT fk_reminiscence_questions_content FOREIGN KEY (content_id) REFERENCES reminiscence_contents(id)
);

CREATE TABLE reminiscence_quizzes (
    content_id UUID NOT NULL,
    quiz_answer VARCHAR(200),
    quiz_photo_id UUID,
    quiz_text VARCHAR(500),
    quiz_sequence INTEGER,
    CONSTRAINT fk_reminiscence_quizzes_content FOREIGN KEY (content_id) REFERENCES reminiscence_contents(id)
);

CREATE TABLE reminiscence_slides (
    content_id UUID NOT NULL,
    slide_caption VARCHAR(300),
    slide_photo_id UUID,
    slide_sequence INTEGER,
    CONSTRAINT fk_reminiscence_slides_content FOREIGN KEY (content_id) REFERENCES reminiscence_contents(id)
);

CREATE TABLE training_questions (
    session_id UUID NOT NULL,
    correct_answer VARCHAR(200) NOT NULL,
    question_difficulty INTEGER NOT NULL,
    question_prompt VARCHAR(500) NOT NULL,
    question_id VARCHAR(255) NOT NULL,
    question_type VARCHAR(255) NOT NULL,
    question_order INTEGER NOT NULL,
    CONSTRAINT pk_training_questions PRIMARY KEY (session_id, question_order),
    CONSTRAINT fk_training_questions_session FOREIGN KEY (session_id) REFERENCES cognitive_training_sessions(id)
);

CREATE TABLE training_question_attempts (
    session_id UUID NOT NULL,
    answered_at TIMESTAMP(6) NOT NULL,
    is_correct BOOLEAN NOT NULL,
    attempt_question_id VARCHAR(255) NOT NULL,
    response_seconds INTEGER NOT NULL,
    submitted_answer VARCHAR(200),
    attempt_order INTEGER NOT NULL,
    CONSTRAINT pk_training_question_attempts PRIMARY KEY (session_id, attempt_order),
    CONSTRAINT fk_training_attempts_session FOREIGN KEY (session_id) REFERENCES cognitive_training_sessions(id)
);
