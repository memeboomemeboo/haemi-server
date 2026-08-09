# 개발 플랜 — #46 F5-01 목소리 알람 P0

> feat · M5 · #45 위에 스택. 회상 알람 유형 추가 + 가족 음성 로테이션 + 발송 직전 상태 검증.

## 1. 이슈 분석
작업 항목: 목소리 알람 P0(회상 알람 유형) / 음성 로테이션 / 발송 직전 상태 검증.
기존 `VoiceAlarm`은 단일 `voiceKey`. 로테이션을 위해 음성 풀+인덱스를 도입한다.
사별/입원 차단(EX-F501-06)·작고 가족 음성 차단(EX-F501-07)은 어르신 상태 머신(#36) 의존 → out-of-scope(#50), 발송 직전 검증에 seam만 남긴다.

## 2. 설계 (기존 M5 컨벤션)
### 도메인
- `AlarmType`에 `REMINISCENCE`(회상 알람, P0) 추가. `CareApplicationService`의 두 switch(제목·확인문구)에 케이스 보강.
- `VoiceAlarm`:
  - `voiceKeys`(`@ElementCollection` 순서 풀) + `voiceRotationIndex`. 기존 `voiceKey` 컬럼은 **현재 재생 음성**으로 유지.
  - `create(...)`는 초기 음성 1개를 풀 0번으로 시작(기존 시그니처 유지).
  - `addVoice(key)` — 로테이션 풀에 음성 추가.
  - `rotateVoice()` — 다음 음성으로 진행(풀 ≤1이면 유지), 현재 `voiceKey` 갱신.
  - `isDispatchable()` — **발송 직전 상태 검증**(현재 active). 사별/입원·작고 가족 음성 차단은 여기 앞단에 #36 결과를 결합(주석 seam).
  - `usesTtsFallback()`는 현재 음성 기준.
### 애플리케이션/프레젠테이션
- `CareApplicationService.processVoiceAlarm`: `shouldTrigger` 후 `isDispatchable` 검증 → markTriggered → 현재 음성으로 발송 → `rotateVoice()`로 다음 발송 대비.
- `addAlarmVoice(command)` + `AddAlarmVoiceCommand` + `CareController` `POST /voice-alarms/{alarmId}/voices`(multipart) + `AddAlarmVoiceRequest`.
- `VoiceAlarmResult`에 `voiceCount`(풀 크기) 추가.
### 마이그레이션 `V17__m5_voice_alarm_rotation.sql`
- `voice_alarms.voice_rotation_index` 추가, `voice_alarm_voices`(alarm_id, voice_key, voice_order) 생성, 기존 `voice_key` → 풀 0번 백필.

## 3~5. 검증
- 단위: `VoiceAlarmTest`(로테이션 순환·풀1 유지·addVoice·TTS fallback·dispatchable), 기존 `CareDomainTest`/`CareApplicationServiceTest` 유지.
- 통합: `./gradlew clean test` green, `FlywayMigrationTest` 17개 + `voice_alarm_voices`·`voice_rotation_index` 검증.

## 6~9
- 커밋 분리: (a) 도메인(AlarmType·VoiceAlarm)+마이그레이션, (b) 서비스·컨트롤러·DTO 연동, (c) 테스트+플랜
- Draft PR, base = 첫 브랜치 `feat/f301-recall-session/#39`, 스택/머지 순서 명시, Closes #46

## Out-of-scope (릴리스 게이트 #50 이월)
- EX-F501-06 사별/입원 시 알람 발송 차단, EX-F501-07 작고한 가족 음성 알람 차단 — 어르신 상태 머신(#36) 의존. `isDispatchable` seam만 제공.
