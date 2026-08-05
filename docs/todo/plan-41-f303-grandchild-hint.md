# 개발 플랜 — #41 F3-03 손주 한마디 재설계 (사전 적립형 + L1/L2/L3 폴백)

> feat · M3 · #40 위에 스택.

## 1. 이슈 분석
현재: 실시간 소진형 손주 찬스 — `requestGrandchildChance`가 알림 이벤트 발송 후 가족이 30분 내 `provideHint` 응답, 미응답 시 EXPIRED→pass. 대기 발생.

목표(#41):
1. **사전 적립형** — 가족이 미리 힌트를 적립, 세션 중 즉시 제공(대기 없음).
2. **L1/L2/L3 3계층 폴백** — L1 인물·사진 특정 적립힌트 → L2 어르신 일반 적립힌트 → L3 시스템 기본 문구. 항상 하나는 제공.
3. **힌트 적립 4개 경로** — 메모 / 온보딩 / 주간 리마인더 / 반응 기반 유도 → 적립 소스(enum)로 모델링.

## 2. 설계 (핵심 슬라이스)
- 도메인: `AccrualSource`(MEMO/ONBOARDING/WEEKLY_REMINDER/REACTION), `HintTier`(L1/L2/L3), `AccruedHint` 애그리거트(elderId, albumId?, personName?, questionId?, author, text, active, createdAt).
- 영속성: `AccruedHintRepository` 포트 + JPA 어댑터 + `V10__m3_accrued_hints.sql`.
- 리졸버: `HintBankResolver`(순수) — elderId + questionId/personName 기준 L1 → 일반 L2 → 기본 L3. `active=false`는 제외(S1 훅).
- 응용: `accrueHint`, `serveGrandchildHint`(리졸브 후 세션에 즉시 적용).
- 세션: `CognitiveTrainingSession.serveAccruedHint(text, responder)` — 찬스 카운트(2/세션) 준수, 즉시 ANSWERED, 대기/이벤트 없음.
- 표현: `POST /api/v1/training/hints`(적립), `POST /sessions/{id}/hints/served`(즉시 제공). 기존 실시간 `chances`/`hints`는 Swagger deprecated 표기(유지).

## 3. 이월 (out of scope, 명시)
- **EX-F303-07 숨김 인물 / EX-F303-08 작고한 가족** — 인물 상태·가시성은 Developer A `Person`/상태(#36) 의존. 본 PR은 `AccruedHint.active` 억제 훅만 제공, 실제 상태 연동은 #36 완료 후 릴리스 게이트 #50에서.
- **주간 리마인더 스케줄러 발사** — 소스 enum(WEEKLY_REMINDER)까지만. 스케줄러 실제 발송은 후속.
- **실시간 소진형 완전 제거** — 안정성 위해 유지·deprecate, 제거는 후속 정리.

## 4~5. 검증
- 단위: 리졸버 L1/L2/L3 + inactive 제외, 세션 serveAccruedHint(찬스 준수/소진), 적립·제공 서비스.
- 통합: `./gradlew clean test` + Flyway V10.

## 6~9
- 커밋 분리: (a) 도메인+영속성+마이그레이션 (b) 리졸버+서비스+엔드포인트 (c) 테스트+플랜
- Draft PR, base develop, 스택 순서·이월 명시, Closes #41.
