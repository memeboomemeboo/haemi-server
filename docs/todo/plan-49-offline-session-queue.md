# 개발 플랜 — #49 오프라인 세션 완주 및 결과 로컬 큐

> feat · 횡단 인프라 · #48 위에 스택. 오프라인 완주 세션 결과를 멱등 수신·7일 보관.

## 1. 이슈 분석
- 오프라인 세션 완주: 단말이 네트워크 없이 세션을 완주하고 결과를 로컬 큐에 보관.
- 결과 로컬 큐(7일 보관, 멱등 재전송): 온라인 복귀 시 큐를 재전송 → 서버는 **멱등**하게 수신(중복 무시), 수신 이력을 **7일 보관** 후 만료.
- 서버 책임 = 멱등 수신 프레임 + 수신 영수증(완주 기록) 영속 + 7일 보관 정리. 실제 도메인 반영(난이도 프로필 등)은 apply 포트 seam.

## 2. 설계 (신규 횡단 패키지 `offline`, 헥사고날)
### 도메인 `offline/domain`
- `OfflineSessionResult`(record): idempotencyKey, elderId, sessionId, completedAt, respondedCount, noResponseCount.
- `OfflineResultReceipt`(entity, table `offline_result_receipts`): idempotency_key(PK) + 결과 요약 + receivedAt. `record(result, receivedAt)`.
- `IngestOutcome`(enum): ACCEPTED, DUPLICATE.
- port `OfflineResultApplyPort`: `apply(OfflineSessionResult)` — 다운스트림 반영 seam(로그 어댑터, 운영 시 m3 반영로 교체).
- repository `OfflineResultReceiptRepository`: save, existsByIdempotencyKey, deleteReceivedBefore.
### 애플리케이션 `offline/application`
- `OfflineResultIngestService`:
  - `ingest(result)`: 키 존재 시 DUPLICATE(무동작) / 아니면 apply + 영수증 저장 → ACCEPTED (멱등).
  - `ingestBatch(list)` → `BatchIngestResult(acceptedCount, duplicateCount)`.
  - `purgeExpired(now)`: `receivedAt < now - 7일` 삭제.
### 인프라 `offline/infrastructure`
- JPA 리포지토리 + 어댑터, `LogOfflineResultApplyAdapter`, `OfflineResultRetentionScheduler`(일 1회 정리).
### 프레젠테이션
- `OfflineResultController` `POST /api/v1/offline-results`(배치) → BatchIngestResult. 요청 DTO 리스트.
### 마이그레이션 `V18__offline_result_receipts.sql`
- `offline_result_receipts`(idempotency_key PK, elder_id, session_id, completed_at, responded/no_response_count, received_at) + received_at 인덱스.
### 설정
- `haemi.offline.retention-days:7`, `haemi.offline.cleanup-cron:0 0 4 * * *`.

## 3~5. 검증
- 단위: `OfflineResultReceiptTest`(팩토리), `OfflineResultIngestServiceTest`(최초 ACCEPTED+apply·저장, 중복 DUPLICATE+apply 미호출, 배치 집계, purge 7일 컷오프).
- 통합: `./gradlew clean test` green, `FlywayMigrationTest` 18 + 테이블 검증.

## 6~9
- 커밋 분리: (a) 도메인·리포지토리·마이그레이션, (b) 애플리케이션·인프라·컨트롤러·설정, (c) 테스트+플랜
- Draft PR, base = 첫 브랜치 `feat/f301-recall-session/#39`, 스택/머지 순서 명시, Closes #49

## Out-of-scope
- 오프라인 결과의 m3 난이도 프로필/세션 실제 반영 — apply 포트 seam(운영 교체).
- 동시 동일키 경합의 PK 충돌 처리는 순차 재전송(실사용) 범위 밖 최적화로 이월.
- S1 회귀(EX-*)·#36 의존 — #50.
