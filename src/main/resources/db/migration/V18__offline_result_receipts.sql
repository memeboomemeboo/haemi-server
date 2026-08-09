-- #49 오프라인 세션 완주 결과 멱등 수신 영수증 (7일 보관)

CREATE TABLE offline_result_receipts (
    idempotency_key   VARCHAR(100) PRIMARY KEY,
    elder_id          VARCHAR(255) NOT NULL,
    session_id        UUID         NOT NULL,
    completed_at      TIMESTAMP    NOT NULL,
    responded_count   INT          NOT NULL,
    no_response_count INT          NOT NULL,
    received_at       TIMESTAMP    NOT NULL
);

CREATE INDEX idx_offline_receipts_received_at ON offline_result_receipts (received_at);
