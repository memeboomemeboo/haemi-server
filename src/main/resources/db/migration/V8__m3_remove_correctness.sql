-- #38 M3 정오답 개념 삭제 및 폐기 카드 정리
-- 정오답(correct/wrong) 신호를 발화(응답) 신호로 대체하고, 폐기 카드 유형을 제거한다.

-- 1) 폐기 카드 유형(퍼즐/단어연결/순서기억) 데이터 정리 (pre-release, SNAPSHOT 기준)
DELETE FROM training_question_attempts
 WHERE session_id IN (
     SELECT session_id FROM training_questions
      WHERE question_type IN ('FAMILY_PHOTO_PUZZLE', 'WORD_ASSOCIATION', 'SEQUENCE_MEMORY')
 );
DELETE FROM training_questions
 WHERE session_id IN (
     SELECT session_id FROM training_questions
      WHERE question_type IN ('FAMILY_PHOTO_PUZZLE', 'WORD_ASSOCIATION', 'SEQUENCE_MEMORY')
 );
DELETE FROM difficulty_policy_question_types
 WHERE question_type IN ('FAMILY_PHOTO_PUZZLE', 'WORD_ASSOCIATION', 'SEQUENCE_MEMORY');

-- 2) 정답 개념 제거
ALTER TABLE training_questions DROP COLUMN correct_answer;
ALTER TABLE training_question_attempts RENAME COLUMN is_correct TO responded;

-- 3) 난이도 프로파일: 정오답 연속 카운트 → 응답 연속 카운트
ALTER TABLE difficulty_profiles RENAME COLUMN consecutive_correct TO consecutive_responded;
ALTER TABLE difficulty_profiles RENAME COLUMN consecutive_wrong TO consecutive_no_response;

-- 4) 정답률 이력 → 응답률 이력
ALTER TABLE difficulty_profile_accuracy_history RENAME TO difficulty_profile_response_history;
ALTER TABLE difficulty_profile_response_history RENAME COLUMN accuracy_rate TO response_rate;

-- 5) 오답 패턴 추적 폐기
DROP TABLE difficulty_profile_wrong_patterns;
