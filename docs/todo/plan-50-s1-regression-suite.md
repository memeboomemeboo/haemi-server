# 개발 플랜 — #50 S1 회귀 스위트 (Developer B 담당분)

> test · 릴리스 게이트 · #49 위에 스택. 그동안 이월한 EX-* 회귀 케이스를 단일 스위트로 집약.

## 1. 이슈 분석
Developer B 담당 S1 회귀 10건을 게이트 스위트로 작성. 다수가 **어르신 상태 머신(#36)** — 상태(DECEASED/HOSPITALIZED) 조회·전이를 소비하는 로직 — 에 의존한다. `Elder.changeStatus`로 상태 설정은 가능하나 이를 **소비하는 차단 로직이 아직 없음**(#36).
→ 정직한 게이트: **지금 충족되는 케이스는 실제 테스트로 고정**, **#36 의존 케이스는 `@Disabled` 스켈레톤**으로 명시(추적 사유 포함). #36 완료 시 스켈레톤을 실 구현으로 채운다.

## 2. 구성 `regression/S1RegressionSuiteTest`
### 실제 통과 (Developer B 소유 메커니즘/이미 충족)
- **EX-F302-07** 개인화 레벨 UI 노출 금지 — `TrainingSessionResult`(중첩 포함) 레코드 컴포넌트에 난이도/레벨 필드 부재를 리플렉션으로 검증.
- **EX-F303-07** 숨김 인물 힌트 재생 차단 — `AccruedHint.suppress()` 억제 훅 → `active=false` → 제공 제외.
- **EX-F303-08** 작고한 가족 힌트/음성 재생 차단 — 동일 억제 훅으로 제공 제외.

### #36 의존 → `@Disabled("#36 어르신 상태 머신 선행 · 릴리스 게이트 #50")` 스켈레톤
- EX-F005-01 사별 후 알림 발송 차단
- EX-F005-06 사별 시 기기 잠금 실패 복구
- EX-F201-05 사별/입원 시 추억 알림 차단 (`ElderNotificationPolicy` 상태 사유 seam 대기)
- EX-F301-10 부적합 상태 세션 개시 차단
- EX-F301-11 세션 중 사별 등록 시 즉시 종료
- EX-F501-06 사별/입원 시 알람 발송 차단 (`VoiceAlarm.isDispatchable` seam 대기)
- EX-F501-07 작고한 가족 음성 알람 차단

각 `@Disabled` 테스트에 케이스가 소비할 seam과 기대 동작을 주석으로 남겨 #36 착수 시 바로 구현 가능하게 한다.

## 3~5. 검증
- `./gradlew clean test` green — 실 테스트 통과, 스켈레톤은 skipped로 집계.
- 마이그레이션 없음.

## 6~9
- 커밋 분리: (a) 회귀 스위트, (b) 플랜
- Draft PR, base = 첫 브랜치 `feat/f301-recall-session/#39`, 스택/머지 순서 명시, Closes #50
- 본문에 CI 게이트 공동 소유(Developer A) 및 #36 의존 잔여 명시

## Out-of-scope
- #36 어르신 상태 머신 구현 자체(Developer A/별도). 본 PR은 담당분 스위트 골격 + 충족 케이스 고정.
