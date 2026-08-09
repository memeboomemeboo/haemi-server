# 개발 플랜 — #51 Developer A 공동 조율 지점 정리

> chore · 조율 문서 · #50 위에 스택. B↔A 공동 조율 3지점을 단일 문서로 정리.

## 1. 이슈 분석
3개 조율 항목(도메인 계약 / 배치 스케줄러 / S1 회귀 게이트)은 모두 이번 스택에서 B가 구현한 seam·스케줄러·스위트와 매핑된다. 산출물 = **조율 계약 문서**(코드 변경 없음).

## 2. 산출물
- `docs/coordination/dev-a-b-coordination.md`:
  1. **도메인 계약**: B의 seam(ElderNotificationPolicy 상태 사유, VoiceAlarm.isDispatchable, AccruedHint.suppress, 세션 개시 가드)이 소비할 **A 제공 조회 API 계약** 확정 — 어르신 상태(ElderStatus) 조회, 인물(Person) 생존/노출 조회. 예상 포트 시그니처 포함.
  2. **배치 스케줄러**: 08:00 A(콘텐츠) → 08:45 B(선다운로드, `PredownloadScheduler`) → 09:00 B(알림, `DailyTrainingScheduler`) 순서 의존과 cron·타임존 정리.
  3. **S1 회귀 게이트**: #50 스위트 현황(충족 3 / #36 의존 7), CI 게이트 공동 소유·해제 조건.

## 3~5. 검증
- 문서 전용, 코드/마이그레이션 없음. `./gradlew clean test` green(변경 없음) 확인.

## 6~9
- 커밋 분리: (a) 조율 문서 + 플랜(단일 chore)
- Draft PR, base = 첫 브랜치 `feat/f301-recall-session/#39`, 스택/머지 순서 명시, Closes #51

## Out-of-scope
- A의 실제 API 구현(#36 등) — 본 문서는 계약 제안·정리. 확정은 공동 리뷰.
