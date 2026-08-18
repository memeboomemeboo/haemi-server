-- F0-01-E 어르신 참여 로그인 (성함·전화번호·초대 코드) + 평생 세션
-- 가족 초대(링크 토큰)와 어르신 초대(6자리 코드)를 같은 invitation 구조에서 kind로 구분한다.

ALTER TABLE invitations ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'FAMILY';
ALTER TABLE invitations ADD COLUMN code VARCHAR(6);
ALTER TABLE invitations ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE invitations ADD COLUMN held_at TIMESTAMP(6);

-- 어르신 초대에는 피초대 이메일과 관계가 없다.
ALTER TABLE invitations ALTER COLUMN relation DROP NOT NULL;

-- 코드는 미수락(pending) 범위에서만 유일하면 된다. 만료·수락된 코드는 재사용할 수 있으므로
-- 컬럼 자체에는 유일 제약을 걸지 않고, 발급 시 pending 충돌만 피한다(FamilyGroupApplicationService).
CREATE INDEX idx_invitations_code ON invitations (code);

-- 어르신 식별·중복 등록 감지용. OTP는 사용하지 않으므로 해시만 보관한다.
ALTER TABLE elders ADD COLUMN phone_hash VARCHAR(64);
CREATE INDEX idx_elders_phone_hash ON elders (phone_hash);

-- 평생 세션(rolling refresh). 어르신 화면에는 로그아웃 UI가 없고, 폐기는 owner 경로로만 한다.
CREATE TABLE elder_sessions (
    id UUID PRIMARY KEY,
    elder_id UUID NOT NULL,
    group_id UUID NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    issued_at TIMESTAMP(6) NOT NULL,
    last_refreshed_at TIMESTAMP(6) NOT NULL,
    rolling_expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6),
    revoked_reason VARCHAR(40),
    CONSTRAINT uk_elder_sessions_refresh_token UNIQUE (refresh_token_hash),
    CONSTRAINT fk_elder_sessions_elder FOREIGN KEY (elder_id) REFERENCES elders(id)
);

CREATE INDEX idx_elder_sessions_elder ON elder_sessions (elder_id, revoked_at);
-- 기기당 활성 세션은 1개다. 재합류 시 기존 세션을 폐기하고 새로 발급한다.
CREATE INDEX idx_elder_sessions_device ON elder_sessions (elder_id, device_id);
