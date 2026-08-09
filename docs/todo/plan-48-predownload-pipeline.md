# 개발 플랜 — #48 선다운로드 파이프라인 구축

> feat · 횡단 인프라 · #47 위에 스택. 08:45 카드·사진·힌트 사전 전송 → 힌트 0초 재생.

## 1. 이슈 분석
배치 순서 의존: 08:00 A(콘텐츠 생성) → **08:45 B(선다운로드)** → 09:00 B(알림).
08:00에 생성된 오늘의 훈련 세션(카드)·앨범 사진·적립 힌트를 08:45에 어르신 단말로 미리 전송(prefetch)해, 09:00 세션에서 힌트가 지연 없이 재생되게 한다.
영속화 대상이 아님(일시적 파이프라인) → 마이그레이션 불필요.

## 2. 설계 (신규 횡단 패키지 `predownload`, 헥사고날)
### 도메인 `predownload/domain`
- `PredownloadBundle`(record): elderId, date, cardKeys, photoKeys, hintKeys, assembledAt. `totalAssets()`, `isEmpty()`.
- port `PredownloadContentPort`: `List<String> eligibleElderIds(LocalDate)`, `PredownloadBundle assemble(elderId, date)`.
- port `PredownloadDispatchPort`: `void dispatch(PredownloadBundle)`.
### 애플리케이션 `predownload/application`
- `PredownloadService.runDailyPredownload(date)`: 적격 어르신마다 조립 → 빈 번들 skip → dispatch → 집계. `PredownloadSummary`(date, elderCount, dispatchedCount, totalAssets) 반환.
### 인프라 `predownload/infrastructure`
- `PredownloadScheduler` `@Scheduled(cron 08:45, zone Asia/Seoul)` → 서비스 호출.
- `LogPredownloadDispatchAdapter`(운영 시 CDN/디바이스 캐시 prefetch로 교체).
- `RepositoryPredownloadContentAdapter`: `AlbumRepository`(적격 어르신·사진), `TrainingSessionRepository`(오늘 세션 카드/사진), `AccruedHintRepository`(힌트)로 번들 조립.
### 설정
- `haemi.predownload.cron:0 45 8 * * *`, `haemi.predownload.time-zone:Asia/Seoul`.

## 3~5. 검증
- 단위: `PredownloadBundleTest`(totalAssets·isEmpty), `PredownloadServiceTest`(적격 어르신 조립·빈 번들 skip·비어있지 않으면 dispatch·집계) — 포트 목.
- 통합: `./gradlew clean test` 전량 green.

## 6~9
- 커밋 분리: (a) 도메인·포트·애플리케이션, (b) 인프라(스케줄러·어댑터)·설정, (c) 테스트+플랜
- Draft PR, base = 첫 브랜치 `feat/f301-recall-session/#39`, 스택/머지 순서·배치 순서 명시, Closes #48

## Out-of-scope
- 실제 CDN/디바이스 prefetch 전송 구현 — 로그 어댑터 seam 제공(운영 교체).
- S1 회귀(EX-*)·어르신 상태 머신(#36) 의존 항목 — #50. (힌트 억제는 `AccruedHint.active` 기존 훅에 위임)
