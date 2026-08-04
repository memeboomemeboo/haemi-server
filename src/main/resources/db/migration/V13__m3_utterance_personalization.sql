-- #40 F3-02 발화 기반 개인화 엔진
-- 응답 스트릭(연속 카운트) → 상향 2주기 카운터로 전환.

-- 상향 기준 연속 충족 세션 수 (2주기 도달 시 상향)
ALTER TABLE difficulty_profiles RENAME COLUMN consecutive_responded TO increase_eligible_sessions;

-- 하향은 즉시 적용하므로 무응답 연속 카운트 불필요
ALTER TABLE difficulty_profiles DROP COLUMN consecutive_no_response;
