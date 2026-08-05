# 개발 플랜 — #36 F0-05 어르신 상태 관리 및 사별 처리

> feat · M0 · #51 위에 스택. 상태 전이 머신 + 사별 생명주기 + 발송 상태검증(dispatchable) 계약.
> 이번 스택에서 계속 이월한 EX-* 및 #50 스켈레톤의 선행 열쇠.

## 1. 이슈 분석
현재 `Elder.changeStatus`는 규칙 없는 단순 세터, `ElderStatus`에 `MEMORIAL` 부재. 사별 처리·복구·무음기간·발송검증 미구현.
작업: 상태 전이 머신 / 사별 처리(2단계·잡취소·기기잠금·7일 무음) / memorial 보관함 / 발송 파이프라인 최종 상태 검증(EX-F005-01) / 48h 오등록 복구. 기기잠금 실패 복구(EX-F005-06).

## 2. 설계 (기존 M0 컨벤션)
### 도메인
- `ElderStatus` + `MEMORIAL`, 전이 행렬 `canTransitionTo(target)`.
- `Elder` 확장: 필드 `bereavementRequestedAt`, `bereavedAt`, `silentUntil`.
  - `transitionTo(target)` 일반 전이(검증), `requestBereavement(now)`(1단계), `confirmBereavement(now, silentDays)`(2단계→DECEASED·무음기간), `recoverFromBereavement(now, windowHours)`(48h 오등록 복구→ACTIVE), `enshrineMemorial(now)`(무음 경과→MEMORIAL).
  - 질의: `isDispatchable(now)` = (ACTIVE|DECLINING) && 무음기간 아님 (= 발송 최종 검증, EX-F005-01), `isInSilentPeriod(now)`, `isMemorialArchiveOnly()`.
- 이벤트: `ElderBereavedEvent`, `ElderBereavementRecoveredEvent` (서비스가 `ApplicationEventPublisher`로 발행).
- 포트: `DeviceLockPort`(기기 원격 잠금), `ScheduledJobCancelPort`(스케줄 잡 취소) + 로그 스텁 어댑터.
- 계약: `ElderStatusQuery`(statusOf/isDispatchable) — #51 조율 계약 구현체, 다운스트림 seam이 소비.
### 애플리케이션/프레젠테이션
- `ElderStatusApplicationService`: 상태 변경·사별 요청/확정/복구·memorial. 사별 확정 시 이벤트 발행.
- `ElderBereavementListener`: onBereaved → 잡 취소 + 기기 잠금(**실패 시 복구/재시도 큐, EX-F005-06**).
- `ElderStatusController` + `ElderStatusResult`.
### 마이그레이션 `V19__elder_status_lifecycle.sql`
- `elders`에 `bereavement_requested_at`, `bereaved_at`, `silent_until` 컬럼 추가.
### 설정
- `haemi.elder.bereavement.silent-days:7`, `recovery-window-hours:48`.

## 3~5. 검증
- 단위: `ElderStateMachineTest`(유효/무효 전이, 2단계 사별, 사별 후 isDispatchable=false, 무음기간 발송차단, 48h 복구 창, 무음경과 memorial), `ElderBereavementListenerTest`(기기잠금 실패 graceful 복구), `ElderStatusApplicationServiceTest`.
- **#50 활성화**: `S1RegressionSuiteTest`의 EX-F005-01을 `@Disabled` 해제 → `Elder.isDispatchable` 실검증으로 전환.
- 통합: `./gradlew clean test` green, `FlywayMigrationTest` 19 + 컬럼 검증.

## 6~9
- 커밋 분리: (a) 도메인(상태머신·이벤트·포트)+마이그레이션, (b) 애플리케이션·리스너·컨트롤러·설정, (c) 테스트+#50 활성화+플랜
- Draft PR, base = 첫 브랜치 `feat/f301-recall-session/#39`, 스택/머지 순서 명시, Closes #36

## Out-of-scope
- memorial 기억 보관함의 실제 열람·다운로드 게이팅(m1 memory 접근 제어) — `isMemorialArchiveOnly()` 상태 seam 제공, 강제는 후속.
- m2/m5 발송 파이프라인의 `ElderStatusQuery` 실호출 배선(EX-F201-05/F501-06 완전 활성) — 계약·질의 제공까지. 소비측 배선은 후속(해당 #50 케이스는 계약 준비 완료로 갱신).
