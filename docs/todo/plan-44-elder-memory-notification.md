# 개발 플랜 — #44 F2-01 어르신 추억 알림 수신

> feat · M2 · #43(그룹 협력 목표) 위에 스택. 어르신이 가족 추억글 알림을 받되 과도한 알림을 막는 수신 정책.

## 1. 이슈 분석
작업 항목: 알림 수신 / 일일 3회 한도 / 야간 21:00~08:00 차단 / 발송 직전 상태 검증.
현재 `MemoryPostApplicationService.handleElderNotification`은 한도(기본 5)만 체크하고 야간 차단·상태 검증이 없다. 로직이 서비스에 흩어져 테스트가 어렵다 → **순수 도메인 정책**으로 분리.

## 2. 설계 (기존 M2 컨벤션)
### 도메인 `m2/domain/model/notification/`
- `QuietHours` (value object) — 시작/종료 시(hour). `covers(LocalTime)`가 자정 넘김(21→08) 랩어라운드 처리.
- `NotificationBlockReason` enum — `NONE`, `QUIET_HOURS`, `DAILY_LIMIT_EXCEEDED`. (사별·입원 등 어르신 상태 사유는 #36 의존 → 아래 out-of-scope)
- `ElderNotificationPolicy` — 순수 결정 로직. 입력: 현재 시각, 오늘 발송 수, 일일 한도, QuietHours. 출력: `NotificationDecision(allowed, reason)`. 한도 초과 판정을 야간보다 우선.
- `NotificationDecision` (record).
### 애플리케이션
- `handleElderNotification` 리팩터: (a) 발송 직전 상태 검증 — 글 존재·`PUBLISHED`·미삭제 확인, (b) `ElderNotificationPolicy` 결정, (c) 차단 시 저녁 요약으로 이월(발송 안 함)·로그, (d) 허용 시 발송.
- 설정: `elder-daily-limit` 기본 **3**으로, `elder-quiet-hours-start=21`, `elder-quiet-hours-end=8` 추가(application.yaml). 정책 빈은 서비스에서 조립.
### 이벤트 연동
- `MemoryPostEventListener.onPostPublished`가 무조건 발송하던 것을 정책 경유(`handleElderNotification`)로 전환. `@Async` 유지.

## 3~5. 검증
- 단위: `ElderNotificationPolicyTest`(야간 경계 20:59 허용/21:00·07:59 차단/08:00 허용, 한도 미만 허용/이상 차단, 한도 우선순위), `QuietHours` 랩어라운드.
- 단위: `MemoryPostApplicationService` 알림 분기(야간·한도 초과 시 미발송, 허용 시 발송, 미게시글 미발송) — Mockito.
- 통합: `./gradlew clean test` 전량 green.

## 6~9
- 커밋 분리: (a) 도메인 정책, (b) 서비스·이벤트·설정 연동, (c) 테스트+플랜
- Draft PR, base develop, 스택 순서(#39→…→#43→**#44**) 명시, Closes #44

## Out-of-scope (릴리스 게이트 #50 이월)
- **EX-F201-05 사별/입원 시 추억 알림 차단** — 어르신 상태 머신(#36)의 `ElderStatus`(HOSPITALIZED/DECEASED) 전이 관리에 의존. 본 PR은 정책에 상태 사유 확장 seam(주석)만 남기고 실제 상태 조회·차단은 구현하지 않음.
- S1 회귀 스위트(EX-*) 자체 — #50.
