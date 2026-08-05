# 개발 플랜 — #35 F0-03 접근 모드 설정 (Mode A/B)

> feat · M0 · #37 위에 스택. 진단 기반 Mode A/B 판정·게이팅·14일 재평가·대행 기록·데이터 승계.

## 1. 이슈 분석
기존 `ElderAccessMode`(UNSET/A/B)·`Elder.changeAccessMode`만 존재. 판정·게이팅·주기 재평가·대행 기록·승계 미구현.
- Mode A = 자립 사용(전체 기능), Mode B = 보조 사용(안전 하위집합).
- 재평가는 **제안까지만**(임의 변경 금지) — 적용은 명시적 가족 행동.
- 데이터 승계 = 모드 변경 시 프로필·생애정보 등 유지(재온보딩 없음).

## 2. 설계 (기존 M0 컨벤션)
### 도메인 `m0/domain/model/access`
- `AccessModeAssessment`: 진단 5문항 점수(각 0~2) → 합산 임계값으로 A/B 추천(순수).
- `ModeFeature`(enum) + `AccessModeGate`: 모드별 기능 게이팅(A=전체, B=하위집합) 순수 판정.
- `EntryPath`(enum) SELF/CAREGIVER — 대행 실행 경로.
- `AccessModeRecommendation`(entity, `access_mode_recommendations`): elderId, recommendedMode, source(INITIAL/PERIODIC_REVIEW), status(PROPOSED/APPLIED/DISMISSED), entryPath, operatorId, createdAt, appliedAt. `apply(entryPath, operatorId, now)`, `dismiss()`.
- repository `AccessModeRecommendationRepository`.
### 애플리케이션 `m0/application/service`
- `AccessModeApplicationService`:
  - `assess(actorId, elderId, answers)` → 추천 산출·PROPOSED 저장.
  - `applyRecommendation(actorId, elderId, recommendationId, entryPath, operatorId)` → `Elder.changeAccessMode`(데이터 승계, 리셋 없음) + APPLIED·대행 기록.
  - `proposePeriodicReview(elderId, now)` → 14일 경과 PROPOSED 재평가 제안(모드 변경 없음).
  - 조회: 최신 추천/게이팅.
### 인프라/프레젠테이션
- `AccessModeReviewScheduler`(일 1회, 마지막 적용 14일 경과 어르신에 재평가 제안).
- `AccessModeController`(진단 제출·추천 적용·게이팅 조회) + 요청 DTO.
### 마이그레이션 `V21__access_mode_recommendation.sql`
- `access_mode_recommendations` 테이블.
### 설정
- `haemi.access-mode.review-interval-days:14`, 스케줄 cron.

## 3~5. 검증
- 단위: `AccessModeAssessmentTest`(점수→A/B 경계), `AccessModeGateTest`(A 전체·B 하위집합), `AccessModeRecommendationTest`(apply/dismiss·대행 기록), `AccessModeApplicationServiceTest`(제안·적용 데이터 승계·재평가 제안만).
- 통합: `./gradlew clean test` green, `FlywayMigrationTest` 21 + 테이블.

## 6~9
- 커밋 분리: (a) 도메인·리포지토리·마이그레이션, (b) 애플리케이션·스케줄러·컨트롤러·설정, (c) 테스트+플랜
- Draft PR, base = 첫 브랜치 `feat/f301-recall-session/#39`, 스택/머지 순서 명시, Closes #35

## Out-of-scope
- 모드별 게이팅의 각 기능 엔드포인트 실제 적용 — `AccessModeGate` 판정 제공까지(소비측 후속).
- A의 Person/상태 조회 계약 의존분 — #51 문서/계약에 위임.
