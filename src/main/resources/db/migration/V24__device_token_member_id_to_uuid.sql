-- #86 device_tokens.member_id 타입 정합성: VARCHAR → UUID
--
-- 다른 테이블의 회원 식별자는 모두 UUID이고, 같은 테이블의 elder_id도 UUID다.
--
-- 문자열을 캐스팅해 살리지 않고 컬럼을 다시 만든다.
--   - device_tokens는 V22에서 처음 생긴 테이블이라 운영 데이터가 없다.
--   - 기기 토큰은 클라이언트가 앱 실행 때 다시 등록하는 일회성 데이터다.
--   - PostgreSQL의 ALTER COLUMN ... USING 절을 H2가 지원하지 않아,
--     캐스팅 방식은 테스트 환경에서 돌지 않는다.

DELETE FROM device_tokens;

DROP INDEX IF EXISTS idx_device_tokens_member;
ALTER TABLE device_tokens DROP COLUMN member_id;
ALTER TABLE device_tokens ADD COLUMN member_id UUID NOT NULL;
CREATE INDEX idx_device_tokens_member ON device_tokens (member_id);
