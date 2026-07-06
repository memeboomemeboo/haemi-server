CREATE TABLE difficulty_policies (
    difficulty_level INTEGER PRIMARY KEY,
    max_average_response_seconds DOUBLE PRECISION NOT NULL,
    increase_accuracy_threshold DOUBLE PRECISION NOT NULL,
    decrease_accuracy_threshold DOUBLE PRECISION NOT NULL,
    reviewed_at TIMESTAMP(6) NOT NULL,
    reviewed_by VARCHAR(100) NOT NULL,
    next_review_date DATE NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT ck_difficulty_policy_level CHECK (difficulty_level BETWEEN 1 AND 5),
    CONSTRAINT ck_difficulty_policy_response CHECK (max_average_response_seconds > 0),
    CONSTRAINT ck_difficulty_policy_accuracy CHECK (
        decrease_accuracy_threshold >= 0
        AND increase_accuracy_threshold <= 1
        AND decrease_accuracy_threshold < increase_accuracy_threshold
    )
);

CREATE TABLE difficulty_policy_question_types (
    difficulty_level INTEGER NOT NULL,
    question_type VARCHAR(255) NOT NULL,
    CONSTRAINT pk_difficulty_policy_question_types
        PRIMARY KEY (difficulty_level, question_type),
    CONSTRAINT fk_difficulty_policy_question_types_policy
        FOREIGN KEY (difficulty_level) REFERENCES difficulty_policies(difficulty_level)
);

CREATE TABLE difficulty_profile_accuracy_history (
    profile_id UUID NOT NULL,
    accuracy_rate DOUBLE PRECISION NOT NULL,
    history_order INTEGER NOT NULL,
    CONSTRAINT pk_difficulty_profile_accuracy_history
        PRIMARY KEY (profile_id, history_order),
    CONSTRAINT fk_difficulty_profile_accuracy_history_profile
        FOREIGN KEY (profile_id) REFERENCES difficulty_profiles(id),
    CONSTRAINT ck_difficulty_profile_accuracy_rate
        CHECK (accuracy_rate BETWEEN 0 AND 1)
);

CREATE TABLE difficulty_profile_wrong_patterns (
    profile_id UUID NOT NULL,
    pattern_key VARCHAR(255) NOT NULL,
    last_question_id VARCHAR(255) NOT NULL,
    question_type VARCHAR(255) NOT NULL,
    consecutive_wrong INTEGER NOT NULL,
    CONSTRAINT fk_difficulty_profile_wrong_patterns_profile
        FOREIGN KEY (profile_id) REFERENCES difficulty_profiles(id)
);

CREATE TABLE difficulty_level_changes (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    elder_id VARCHAR(255) NOT NULL,
    album_id UUID NOT NULL,
    previous_level INTEGER NOT NULL,
    current_level INTEGER NOT NULL,
    three_session_moving_average DOUBLE PRECISION NOT NULL,
    changed_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_difficulty_level_changes_session
        FOREIGN KEY (session_id) REFERENCES cognitive_training_sessions(id),
    CONSTRAINT ck_difficulty_level_changes_previous CHECK (previous_level BETWEEN 1 AND 5),
    CONSTRAINT ck_difficulty_level_changes_current CHECK (current_level BETWEEN 1 AND 5)
);

CREATE TABLE difficulty_level_change_wrong_questions (
    change_id UUID NOT NULL,
    question_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_difficulty_level_change_wrong_questions_change
        FOREIGN KEY (change_id) REFERENCES difficulty_level_changes(id)
);

INSERT INTO difficulty_policies (
    difficulty_level,
    max_average_response_seconds,
    increase_accuracy_threshold,
    decrease_accuracy_threshold,
    reviewed_at,
    reviewed_by,
    next_review_date,
    updated_at
) VALUES
    (1, 20.0, 0.8, 0.4, TIMESTAMP '2026-07-06 00:00:00', 'system-default', DATE '2026-10-06', TIMESTAMP '2026-07-06 00:00:00'),
    (2, 25.0, 0.8, 0.4, TIMESTAMP '2026-07-06 00:00:00', 'system-default', DATE '2026-10-06', TIMESTAMP '2026-07-06 00:00:00'),
    (3, 30.0, 0.8, 0.4, TIMESTAMP '2026-07-06 00:00:00', 'system-default', DATE '2026-10-06', TIMESTAMP '2026-07-06 00:00:00'),
    (4, 40.0, 0.8, 0.4, TIMESTAMP '2026-07-06 00:00:00', 'system-default', DATE '2026-10-06', TIMESTAMP '2026-07-06 00:00:00'),
    (5, 50.0, 0.8, 0.4, TIMESTAMP '2026-07-06 00:00:00', 'system-default', DATE '2026-10-06', TIMESTAMP '2026-07-06 00:00:00');

INSERT INTO difficulty_policy_question_types (difficulty_level, question_type) VALUES
    (1, 'FAMILY_PHOTO_PUZZLE'), (1, 'WORD_ASSOCIATION'), (1, 'SEQUENCE_MEMORY'),
    (1, 'PERSON_RECALL'), (1, 'PLACE_MATCH'), (1, 'COLOR_SHAPE'),
    (2, 'FAMILY_PHOTO_PUZZLE'), (2, 'WORD_ASSOCIATION'), (2, 'SEQUENCE_MEMORY'),
    (2, 'PERSON_RECALL'), (2, 'PLACE_MATCH'), (2, 'COLOR_SHAPE'),
    (3, 'FAMILY_PHOTO_PUZZLE'), (3, 'WORD_ASSOCIATION'), (3, 'SEQUENCE_MEMORY'),
    (3, 'PERSON_RECALL'), (3, 'PLACE_MATCH'), (3, 'COLOR_SHAPE'),
    (4, 'FAMILY_PHOTO_PUZZLE'), (4, 'WORD_ASSOCIATION'), (4, 'SEQUENCE_MEMORY'),
    (4, 'PERSON_RECALL'), (4, 'PLACE_MATCH'), (4, 'COLOR_SHAPE'),
    (5, 'FAMILY_PHOTO_PUZZLE'), (5, 'WORD_ASSOCIATION'), (5, 'SEQUENCE_MEMORY'),
    (5, 'PERSON_RECALL'), (5, 'PLACE_MATCH'), (5, 'COLOR_SHAPE');
