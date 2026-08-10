-- #80 FCM 푸시 알림 발송 연동: 기기 토큰 저장소

CREATE TABLE device_tokens (
    token         VARCHAR(255) PRIMARY KEY,
    member_id     VARCHAR(255) NOT NULL,
    platform      VARCHAR(10)  NOT NULL,
    registered_at TIMESTAMP    NOT NULL,
    last_used_at  TIMESTAMP    NOT NULL
);

-- 수신자(memberId)로 발송 대상 토큰을 찾는 조회 경로
CREATE INDEX idx_device_tokens_member ON device_tokens (member_id);
