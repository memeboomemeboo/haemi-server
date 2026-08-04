-- #37 F0-06 이벤트 로깅 및 지표 파이프라인

CREATE TABLE logged_events (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    elder_id        VARCHAR(255),
    event_type      VARCHAR(30)  NOT NULL,
    occurred_at     TIMESTAMP    NOT NULL,
    duration_ms     INT,
    detail          VARCHAR(200),
    pseudonymized   BOOLEAN      NOT NULL DEFAULT FALSE,
    received_at     TIMESTAMP    NOT NULL
);

-- 06:00 일일 집계용 (기간·타입)
CREATE INDEX idx_logged_events_occurred_type ON logged_events (occurred_at, event_type);
-- 동의 철회 시 가명 처리용
CREATE INDEX idx_logged_events_elder ON logged_events (elder_id);

CREATE TABLE event_collection_consent (
    elder_id     VARCHAR(255) PRIMARY KEY,
    active       BOOLEAN      NOT NULL,
    withdrawn_at TIMESTAMP
);
