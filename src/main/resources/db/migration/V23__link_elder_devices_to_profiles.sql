-- F5-01: 계정 소유자와 어르신 본인 휴대전화를 분리한다.
-- Mode B에서는 보호자 계정이 어르신 폰에서 대행 실행할 수 있으므로, member_id만으로는
-- 회상/알람 푸시 수신 대상을 정확히 지정할 수 없다.
ALTER TABLE elders ADD COLUMN member_id UUID;
CREATE UNIQUE INDEX ux_elders_member_id ON elders(member_id);

ALTER TABLE device_tokens ADD COLUMN elder_id UUID;
CREATE INDEX idx_device_tokens_elder_id ON device_tokens(elder_id);
