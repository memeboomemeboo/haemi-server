-- 퀴즈 더미 데이터 시드
-- 1) 회상 퀴즈(reminiscence_quizzes): 앨범 + 회상 콘텐츠 + 퀴즈
-- 2) 인지훈련 퀴즈(training_questions): 인지훈련 세션 + 문제

-- ---------------------------------------------------------------------------
-- 공통 더미 앨범
-- ---------------------------------------------------------------------------
INSERT INTO albums (id, created_at, elder_profile_id, group_id, owner_member_id)
VALUES ('11111111-1111-1111-1111-111111111111',
        TIMESTAMP '2026-07-01 09:00:00',
        'elder-dummy-001',
        'group-dummy-001',
        'member-dummy-owner');

-- ---------------------------------------------------------------------------
-- 1) 회상 퀴즈
-- ---------------------------------------------------------------------------
INSERT INTO reminiscence_contents (id, album_id, elder_reaction, generated_at, generated_date)
VALUES ('22222222-2222-2222-2222-222222222222',
        '11111111-1111-1111-1111-111111111111',
        NULL,
        TIMESTAMP '2026-07-01 09:05:00',
        DATE '2026-07-01');

INSERT INTO reminiscence_quizzes (content_id, quiz_answer, quiz_photo_id, quiz_text, quiz_sequence)
VALUES
    ('22222222-2222-2222-2222-222222222222', '바다',   NULL, '이 사진 속 가족이 함께 놀러 간 곳은 어디일까요?', 1),
    ('22222222-2222-2222-2222-222222222222', '손녀',   NULL, '옆에서 환하게 웃고 있는 사람은 누구일까요?',       2),
    ('22222222-2222-2222-2222-222222222222', '여름',   NULL, '이 사진을 찍은 계절은 언제일까요?',               3),
    ('22222222-2222-2222-2222-222222222222', '수박',   NULL, '식탁 위에 놓여 있던 과일은 무엇이었을까요?',       4),
    ('22222222-2222-2222-2222-222222222222', '생신',   NULL, '이날 가족이 함께 축하한 특별한 날은 무엇일까요?', 5);

-- ---------------------------------------------------------------------------
-- 2) 인지훈련 퀴즈
-- ---------------------------------------------------------------------------
INSERT INTO cognitive_training_sessions (
    id, album_id, chance_used_count, completed_at, current_question_index,
    difficulty_level, elder_id, last_hint_responder, last_hint_text,
    session_date, start_mode, started_at, status,
    last_chance_status, last_chance_question_id, last_chance_requested_at,
    chance_unused_completion_badge_awarded)
VALUES ('33333333-3333-3333-3333-333333333333',
        '11111111-1111-1111-1111-111111111111',
        0, NULL, 0,
        1, 'elder-dummy-001', NULL, NULL,
        DATE '2026-07-01', 'AUTO', TIMESTAMP '2026-07-01 10:00:00', 'IN_PROGRESS',
        'NONE', NULL, NULL,
        FALSE);

INSERT INTO training_questions (
    session_id, correct_answer, question_difficulty, question_prompt,
    question_id, question_type, question_order, question_photo_id)
VALUES
    ('33333333-3333-3333-3333-333333333333', '사과',   1, '화면에 보이는 과일의 이름은 무엇일까요?',           'q-dummy-001', 'WORD_ASSOCIATION',    1, NULL),
    ('33333333-3333-3333-3333-333333333333', '손자',   1, '사진 속 인물은 누구일까요?',                       'q-dummy-002', 'PERSON_RECALL',       2, NULL),
    ('33333333-3333-3333-3333-333333333333', '공원',   2, '이 장소는 어디일까요?',                             'q-dummy-003', 'PLACE_MATCH',         3, NULL),
    ('33333333-3333-3333-3333-333333333333', '빨강',   2, '가장 위에 있는 도형의 색깔은 무엇일까요?',           'q-dummy-004', 'COLOR_SHAPE',         4, NULL),
    ('33333333-3333-3333-3333-333333333333', '1-2-3', 3, '방금 본 순서대로 숫자를 나열해 주세요.',            'q-dummy-005', 'SEQUENCE_MEMORY',     5, NULL);
