# 개발 플랜 — #42 M2 랭킹 폐기

> refactor · M2 · #41 위에 스택. 개인 순위·뱃지·스트릭 개념 전면 제거(대체 기능 #43 별도).

## 1. 이슈 분석
M2 랭킹은 도메인~스케줄러~컨트롤러 수직 슬라이스. 외부 결합점은 `MemoryPostEventListener`(뱃지 알림), `OpenApiConfig`(M2-Ranking 태그), 마이그레이션 3테이블뿐. m3의 `GrandchildChanceUnusedBadgeAwardedEvent`는 별개 완료 이벤트로 범위 외.

## 2. 변경
- 삭제(Java): `domain/model/ranking/*`(BadgeGrade·FamilyRanking·MemberStarSummary·RankEntry·RankingPeriod), `BadgeAwardedEvent`, `FamilyRankingRepository`+adapter+jpa, `RankingScheduler`, `RankingApplicationService`, `ComputeRankingCommand`, `RankingResult`, `GetRankingQuery`, `RankingController`, `FamilyRankingTest`.
- 수정: `MemoryPostEventListener`(onBadgeAwarded 제거), `OpenApiConfig`(M2-Ranking 태그 제거, M3 태그 문구 정리).
- 마이그레이션 `V11__m2_drop_ranking.sql`: ranking_entries → member_star_summaries → family_rankings DROP.
- 테스트: FlywayMigrationTest 마이그레이션 수·테이블 부재 검증.

## 3~5. 검증
- `./gradlew clean test` 전량 green (173), 잔존 랭킹 심볼 0.

## 6~9
- 커밋 분리: (a) Java 삭제+리스너/OpenAPI 정리 (b) 마이그레이션+테스트+플랜
- Draft PR, base develop, 스택 순서 명시, Closes #42.
