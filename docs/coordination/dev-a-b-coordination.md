# Developer B ↔ Developer A 공동 조율 지점 (#51)

> 기준: Phase 1~2 스택(#39~#50) 실측 · 2026-08-05
> 목적: B가 구현한 seam·스케줄러·회귀 스위트를 A와의 계약 관점에서 확정·정리.

세 조율 지점 모두 B 쪽 구현은 **seam(확장 지점)** 형태로 완료되어 있고, A 쪽 실제 구현(주로 #36 어르신 상태 머신)이 붙으면 즉시 활성화된다.

---

## 1. 도메인 계약 — A의 `Elder`/`Person` 조회 API

B의 여러 차단 로직은 A가 소유한 어르신 상태·인물 노출 정보를 **읽기**로 소비한다. 아래 조회 계약을 확정한다(구현·전이는 A/#36 소유, B는 소비자).

### 1-1. 어르신 상태 조회
- 소비처(B seam):
  - `m2 ElderNotificationPolicy` — 상태 사유(`NotificationBlockReason`)에 사별/입원 추가 예정 (EX-F201-05)
  - `m5 VoiceAlarm.isDispatchable()` — 발송 직전 상태 결합 (EX-F501-06)
  - `m3` 세션 개시 가드 — 부적합 상태 개시 차단 (EX-F301-10), 세션 중 사별 즉시 종료 (EX-F301-11)
- 요청 계약(제안):
  ```java
  // A 제공, B 소비 (읽기 전용)
  interface ElderStatusQuery {
      ElderStatus statusOf(String elderId);           // ACTIVE/DECLINING/HOSPITALIZED/DECEASED/DORMANT
      boolean isDispatchable(String elderId);          // 알림·알람 발송 가능 상태 여부(ACTIVE/DECLINING)
  }
  ```
- 합의 필요: 상태 변경 이벤트(사별 등록) 발행 방식 — B의 진행 중 세션 즉시 종료(EX-F301-11)는 **도메인 이벤트 구독**을 전제로 한다(`ElderStatusChangedEvent`).

### 1-2. 인물 생존/노출 조회
- 소비처(B seam): `m3 AccruedHint.suppress()` — 숨김 인물·작고 가족 힌트 억제 (EX-F303-07/08)
- 요청 계약(제안):
  ```java
  interface PersonVisibilityQuery {
      boolean isVisibleForElder(String elderId, UUID personId);  // 숨김이면 false
      boolean isDeceased(UUID personId);                          // 작고 가족이면 true
  }
  ```
- 합의 필요: 노출 변경 시 이미 적립된 힌트 억제 트리거 — A의 상태 변경 → B `AccruedHint.suppress()` 호출 경로(이벤트 구독 vs 스케줄 재평가). 참고: `m1 PersonSafetyInvalidationListener`(EX-F004-04)가 유사 패턴.

---

## 2. 배치 스케줄러 순서 의존

하루 실행 흐름은 **엄격한 순서 의존**을 가진다. cron·타임존을 공동 고정한다.

| 시각 | 소유 | 작업 | 구현 |
|------|------|------|------|
| 08:00 | **A** | 오늘의 콘텐츠(회상 카드·세션) 생성 | (A) |
| 08:45 | **B** | 선다운로드(카드·사진·힌트 사전 전송) | `PredownloadScheduler` (`haemi.predownload.cron=0 45 8`) |
| 09:00 | **B** | 일일 훈련/알림 발송 | `DailyTrainingScheduler` (`0 0 9`) |

- 타임존: 전 구간 `Asia/Seoul` 고정.
- 의존 규칙: 08:45 B는 08:00 A 산출물을 읽는다(`RepositoryPredownloadContentAdapter`가 오늘 세션·힌트 조회). A 지연 시 B는 **빈 번들 skip**으로 안전 실패(장애 전파 없음).
- 합의 필요: A의 08:00 완료 신호(완료 마커/이벤트) 여부. 현재 B는 마커 없이 조회 시점 존재분만 조립(best-effort).

---

## 3. S1 회귀 스위트 · CI 게이트 공동 소유

- B 담당분 스위트: `regression/S1RegressionSuiteTest` (#50).
  - **충족 3건**(통과): EX-F302-07(레벨 비노출), EX-F303-07/08(힌트 억제 훅).
  - **#36 의존 7건**(`@Disabled`): EX-F005-01/06, F201-05, F301-10/11, F501-06/07 — A의 상태 머신 착수 후 스켈레톤을 실 구현으로 채움.
- CI 게이트: A·B **공동 소유**. 게이트 통과 조건 = 두 담당분 스위트 green + 해제된(`@Disabled` 제거) 케이스 100% 통과.
- 해제 조건: 위 1번 도메인 계약(ElderStatusQuery/PersonVisibilityQuery + 상태 변경 이벤트)이 A에서 구현·머지되면 B가 7건 스켈레톤을 순차 활성화.

---

## 참조
- 스택 PR: #39~#50 (base `feat/f301-recall-session/#39`로 순차 머지 후 develop 1회 머지)
- 관련 seam 위치: `ElderNotificationPolicy`, `VoiceAlarm.isDispatchable`, `AccruedHint.suppress`, `PredownloadScheduler`, `S1RegressionSuiteTest`
