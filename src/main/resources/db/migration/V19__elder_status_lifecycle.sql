-- #36 F0-05 어르신 상태 관리 및 사별 처리 — 사별 생명주기 컬럼 추가
-- (status 컬럼은 기존 존재, MEMORIAL은 VARCHAR 값 추가라 스키마 변경 불필요)

ALTER TABLE elders ADD COLUMN bereavement_requested_at TIMESTAMP;
ALTER TABLE elders ADD COLUMN bereaved_at TIMESTAMP;
ALTER TABLE elders ADD COLUMN silent_until TIMESTAMP;
