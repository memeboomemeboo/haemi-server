# 개발 플랜 — #40 F3-02 발화 기반 개인화 엔진

> feat · M3 · #38(정오답 삭제)·#39(회상 세션) 위에 스택.

## 1. 이슈 분석

작업 항목:
1. **발화율 1순위 개인화** — 난이도 조정의 1순위 신호를 세션 발화율(responseRate)로.
2. **하향 즉시 / 상향 2주기** — 발화율 낮으면(또는 timeout) 그 세션에서 즉시 하향, 높으면 2세션 연속 충족 시에만 상향.
3. **레벨 어르신·가족 비노출** — 세션 응답 DTO에서 난이도 레벨 노출 제거 (EX-F302-07).

현재(#38 이후) 엔진: 응답 스트릭(연속 3회) + 3세션 이동평균 + 극단 점수 버퍼. → 세션 단위 발화율 임계 비교로 재정의.

## 2. 설계

**DifficultyProfile.applySession(performances, responseRate, avgResponseSeconds, policy)**
- 하향 즉시: `timeout || rate <= policy.decreaseThreshold` → `level=max(1, level-1)`, increaseEligible=0
- 상향 2주기: `rate >= policy.increaseThreshold && avg <= policy.maxAvg` → increaseEligible++; `>=2`면 `level=min(5, level+1)`, 카운터 리셋
- 중간대: increaseEligible=0 (연속성 끊김)
- `extremeScoreBuffered` 계약 재사용 → "상향 1주기 충족·대기" 의미로 세팅

**필드 변경**: `consecutiveResponded/NoResponse` → `increaseEligibleSessions` 단일 카운터.
- V9 마이그레이션: `consecutive_responded`→`increase_eligible_sessions` 이름변경, `consecutive_no_response` DROP.

**레벨 비노출**: `TrainingSessionResult.difficultyLevel` 및 `QuestionResult.difficultyLevel` 제거. 관리자 DTO/정책은 유지.

**이벤트 계약 유지**: `DifficultyLevelChangedEvent`(threeSessionMovingAverage/repeatedWrongQuestionIds) 불변 → m4 영향 없음.

## 3. 변경 파일
- 도메인: `DifficultyProfile`(로직/필드), `DifficultyAdjustment`(extremeScoreBuffered 의미 재사용)
- DTO: `TrainingSessionResult`(레벨 필드 제거)
- 마이그레이션: `V9__m3_utterance_personalization.sql`
- 테스트: 난이도 프로파일 단위 테스트 재작성 + FlywayMigrationTest + 레벨 비노출 확인

## 4~5. 검증
- 단위: 하향 즉시 / 상향 2주기 / 중간대 유지 / 레벨 비노출
- 통합: `./gradlew test` (Flyway 마이그레이션 포함) 전량 green

## 6~9
- 커밋 분리: (a) 엔진 로직+마이그레이션 (b) 레벨 비노출 DTO (c) 테스트
- Draft PR, base develop, 스택 순서 명시, Closes #40. S1 EX-F302-07은 본 변경으로 충족.
