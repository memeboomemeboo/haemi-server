# 개발 플랜 — #47 F5-02 산책 보류 처리 (WalkRoutine 비활성)

> chore · M5 · #46 위에 스택. 산책 기능을 **보류(hold)** — 폐기가 아니라 도달·동작만 비활성화.

## 1. 이슈 분석
"WalkRoutine 관련 기능 비활성 (산책 보류)". 라벨 chore·용어 "보류/비활성"은 삭제가 아님(#42 랭킹 "폐기"와 대비).
→ 도메인·영속성·테이블·서비스 로직·테스트는 **보존**하고, 외부에서 도달·동작하는 표면만 끈다.

## 2. 변경 (경계 비활성화)
- `CareController`: 산책 4개 엔드포인트 제거(`/walk-routines` 생성, `/walk-routines/{id}/start`, `/walk-records/{id}/complete`, `/walk-records/weekly-summary`). @Tag 문구에 산책 보류 명시.
- `CareApplicationService.processDueReminders`: 산책 루틴 처리 라인 제거(스케줄러가 더는 산책 알림을 보내지 않음). 스케줄러 전용 private `processWalkRoutine` 제거.
- `OpenApiConfig`: M5-Care 태그 설명에서 산책을 보류로 표기.
- **보존(hold)**: `WalkRoutine`/`WalkRecord`/`WalkStatus`/`WeatherCondition`/`WeatherPort`/StubWeatherAdapter, walk 리포지토리, walk 서비스 메서드(create/start/complete/weeklySummary), `walk_routines`/`walk_records` 테이블. 재개 시 컨트롤러·스케줄러만 재연결하면 됨.
  - `weatherPort`는 `startWalk`에서 계속 사용하므로 유지.

## 3~5. 검증
- 마이그레이션 불필요(테이블 보존).
- 기존 walk 서비스/도메인 테스트(`CareApplicationServiceTest`/`CareDomainTest`) 그대로 green — 서비스 로직 미변경.
- `./gradlew clean test` 전량 green.

## 6~9
- 커밋 분리: (a) 컨트롤러·스케줄러 산책 비활성 + OpenAPI, (b) 플랜
- Draft PR, base = 첫 브랜치 `feat/f301-recall-session/#39`, 스택/머지 순서 명시, Closes #47

## Out-of-scope
- 산책 도메인/테이블 삭제 — 보류이므로 하지 않음(재개 대비).
- S1 회귀(EX-*)·어르신 상태 머신(#36) 의존 항목 — #50.
