# 개발 플랜 — #38 M3 정오답 개념 삭제 및 폐기 카드 정리

> refactor · M3 회상 세션 개편 선행 작업 · 동작 유지가 아닌 "정오답 개념 제거"이므로 시맨틱 변경 동반

## 1. 이슈 분석 요약

정오답(`correctAnswer` / `isCorrect` / `WrongAnswerPattern`) 개념이 M3 적응형 난이도 엔진의 **입력 신호**로 깊게 결합되어 있음:

- `TrainingQuestion.correctAnswer` + `isCorrect(submitted)` + `getPatternKey()`
- `QuestionAttempt.correct` (DB `is_correct`)
- `QuestionPerformance.correct`
- `CognitiveTrainingSession`: `getAccuracyRate` / `getCorrectCount` / `getWrongCount`, 완료 이벤트 `accuracyRate`
- `DifficultyProfile`: `wrongAnswerPatterns`, `consecutiveCorrect/Wrong`, accuracy 이동평균으로 레벨 상·하향
- `DifficultyAdjustment.repeatedWrongQuestionIds`
- DTO: `AnswerResult.correct`, `TrainingSessionResult.accuracyRate` + attempt.correct
- 카드 유형: `FAMILY_PHOTO_PUZZLE`, `WORD_ASSOCIATION`, `SEQUENCE_MEMORY` (폐기 대상) / `PERSON_RECALL`, `PLACE_MATCH`, `COLOR_SHAPE` (회상 존치)
- DB: V1(training_questions.correct_answer, training_question_attempts.is_correct, difficulty_profiles.consecutive_correct/wrong), V3(difficulty_profile_wrong_patterns, seed된 policy question types)

## 2. 설계 결정 — 정오답 → 발화(응답) 신호 대체

회상 세션은 정답 판정을 하지 않음. 난이도 엔진 입력을 **"어르신이 응답(발화)했는가"** 로 대체:

- `correct` → `responded` (submittedAnswer 비어있지 않고 timeout 아님)
- accuracy 이동평균 → **response rate 이동평균** (컬럼 재사용, 의미 변경)
- `consecutiveCorrect/Wrong` → `consecutiveResponded/consecutiveNoResponse`
- `WrongAnswerPattern` / `repeatedWrongQuestionIds` / `patternKey` **완전 삭제** → 유형 추천은 정책 유형만 사용
- 발화율(비율) 1순위 개인화 정교화는 #40(F3-02)에서. 본 이슈는 컴파일·테스트 가능한 응답 기반 엔진까지.

## 3. 변경 파일

**도메인**
- `TrainingQuestion`: correctAnswer/isCorrect/getPatternKey 제거, 팩토리 시그니처 축소
- `QuestionAttempt`: correct → responded, `of(...)` 갱신
- `QuestionPerformance`: correct → responded
- `WrongAnswerPattern`: **삭제**
- `CognitiveTrainingSession`: accuracy→responseRate, correct/wrong count→responded/noResponse, 이벤트 필드
- `DifficultyProfile`: wrongAnswerPatterns 제거, consecutive* 개명, recommendQuestionTypes 단순화
- `DifficultyAdjustment`: repeatedWrongQuestionIds 제거
- `QuestionType`: PUZZLE/WORD_ASSOCIATION/SEQUENCE_MEMORY 제거
- `TrainingSessionCompletedEvent`: accuracyRate → responseRate

**응용/표현**
- `TrainingApplicationService`: message 로직·이벤트 매핑 갱신
- `AnswerResult`: correct → responded
- `TrainingSessionResult`: accuracyRate→responseRate, attempt.correct→responded
- 생성기 `AlbumCognitiveQuestionGeneratorAdapter`: switch에서 폐기 유형 제거, correctAnswer 인자 제거

**인프라/DB**
- `V8__m3_remove_correctness.sql`: correct_answer/is_correct 컬럼 처리(응답 컬럼), wrong_patterns 테이블 DROP, consecutive_* 개명, 폐기 유형 seed 제거

**테스트**
- M3 도메인/서비스/정책 테스트 전면 갱신 (정오답 → 발화 시맨틱)

## 4. 자체 검증

- `./gradlew compileJava`
- `./gradlew test --tests "com.memeboo2.haemi.m3.*"` → 전체 `./gradlew test`
- 잔존 정오답 심볼 grep 0건 확인

## 5. PR

- 브랜치 `refactor/m3-remove-correctness/#38` → develop 대상 PR, Closes #38
