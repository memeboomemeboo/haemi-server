ALTER TABLE training_question_attempts
    DROP COLUMN submitted_answer;

ALTER TABLE training_question_attempts
    ADD COLUMN vad_duration_ms INTEGER NOT NULL DEFAULT 0;
