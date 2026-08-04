# 개발 플랜 — #37 F0-06 이벤트 로깅 및 지표 파이프라인

> feat · M0 · 횡단 인프라 · #36 위에 스택. 이벤트 11종 수집·집계 파이프라인. (소급 불가, Phase 1 필수)

## 1. 이슈 분석
작업: 이벤트 11종 스키마 / 온디바이스 VAD 수신(내용 미저장, 발생·길이만) / Ingest API(배치·로컬 큐 재전송) / Event Store + 06:00 일일 집계 / 동의 철회 시 즉시 중단 + 기존분 가명 처리.
Ingest 멱등·배치·로컬 큐 재전송은 #49 오프라인 큐 패턴 재사용. 프라이버시: VAD는 duration만, content 필드 없음.

## 2. 설계 (신규 횡단 패키지 `eventlog`, 헥사고날)
### 도메인 `eventlog/domain`
- `EventType`(11종): SESSION_START, SESSION_COMPLETE, SESSION_ABANDON, HINT_SERVED, HINT_USED, NOTIFICATION_SENT, NOTIFICATION_ACK, ALARM_TRIGGERED, VAD_DETECTED, CONSENT_CHANGED, SYSTEM_ERROR.
- `LoggedEvent`(entity, `logged_events`): idempotency_key(PK), elder_id(nullable), event_type, occurred_at, duration_ms(nullable·VAD 전용), detail(짧은 요약, 원문 없음), pseudonymized, received_at. `pseudonymize()`(elder_id 제거).
- `EventEnvelope`(record) 수신 이벤트, `EventIngestOutcome`(ACCEPTED/DUPLICATE/REJECTED_NO_CONSENT).
- `EventCollectionConsent`(entity, `event_collection_consent`): elder_id(PK), active, withdrawn_at. `withdraw()`.
- repository: `LoggedEventRepository`(save, existsByIdempotencyKey, pseudonymizeByElderId, countByTypeBetween), `EventCollectionConsentRepository`.
### 애플리케이션 `eventlog/application`
- `EventLoggingService`: `ingest`(동의 확인→멱등 dedup→저장), `ingestBatch`→`BatchEventResult`, `withdrawConsent`(즉시 중단 + 기존분 가명처리), `aggregateDaily(date)`→`DailyEventSummary`(타입별 카운트).
### 인프라
- JPA 리포지토리·어댑터, `EventAggregationScheduler`(06:00 cron).
### 프레젠테이션
- `EventIngestController` `POST /api/v1/events`(배치), `POST /api/v1/events/consent/{elderId}/withdraw`.
### 마이그레이션 `V20__event_logging.sql`
- `logged_events`(+received_at 인덱스, occurred_at·event_type 집계 인덱스), `event_collection_consent`.
### 설정
- `haemi.eventlog.aggregate-cron:0 0 6 * * *`, time-zone Asia/Seoul.

## 3~5. 검증
- 단위: `LoggedEventTest`(VAD duration·content 부재·가명처리), `EventLoggingServiceTest`(멱등 ACCEPTED/DUPLICATE·무동의 REJECTED·배치 집계·철회 시 가명처리·일일 집계 카운트).
- 통합: `./gradlew clean test` green, `FlywayMigrationTest` 20 + 테이블·컬럼.

## 6~9
- 커밋 분리: (a) 도메인·리포지토리·마이그레이션, (b) 애플리케이션·인프라·컨트롤러·설정, (c) 테스트+플랜
- Draft PR, base = 첫 브랜치 `feat/f301-recall-session/#39`, 스택/머지 순서 명시, Closes #37

## Out-of-scope
- 집계 결과의 M4 대시보드 실제 연동 — 요약 산출까지(후속).
- 이벤트 원문/음성 저장 — 정책상 미수집(VAD는 duration만).
