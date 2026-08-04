ALTER TABLE reminiscence_slides
    ADD COLUMN card_type VARCHAR(20) NOT NULL DEFAULT 'STORY_CARD';

ALTER TABLE reminiscence_slides
    ADD COLUMN safety_passed BOOLEAN NOT NULL DEFAULT TRUE;

-- v2의 질문/퀴즈 테이블은 과거 감사 기록 보존을 위해 유지하되, v3 코드에서는 더 이상 읽거나 생성하지 않는다.
