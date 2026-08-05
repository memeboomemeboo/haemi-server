# 개발 플랜 — #43 F1-03-A 그룹 협력 목표

> feat · M2 · #42(랭킹 폐기) 위에 스택. 폐기된 개인 순위·뱃지·스트릭의 **대체 기능**.
> 개인 경쟁 없이 가족이 하나의 공동 목표를 함께 채우는 협력형 목표 + 하이라이트 카드.

## 1. 이슈 분석
- #42에서 `FamilyRanking`(개인 순위·뱃지·스트릭) 수직 슬라이스를 전면 제거함. 그 자리를 **경쟁이 아닌 협력** 개념으로 대체한다.
- 핵심 요구: "그룹 협력 목표 (개인 순위·뱃지·스트릭 없음)" + "하이라이트 카드".
- 진행 신호는 이미 존재하는 도메인 이벤트(`MemoryPostPublishedEvent`, `ElderRepliedEvent`)로 확보 가능 → 스케줄러 불필요, 이벤트 구독으로 협력 진척을 누적한다.
- 하이라이트 카드는 기간 내 게시글 집계(개인 랭킹이 아니라 **그룹 전체**의 성취·가장 사랑받은 추억)만 사용.

## 2. 설계 (수직 슬라이스, 기존 M2 컨벤션 준수)
### 도메인 `m2/domain/model/goal/`
- `GroupGoal` (AbstractAggregateRoot, table `group_goals`, unique(album_id, period, period_start))
  - 필드: id, albumId, period(`GoalPeriod` WEEKLY/MONTHLY), periodStart/End, targetCount, currentProgress, status(`GoalStatus` IN_PROGRESS/ACHIEVED), achievedAt, createdAt
  - 참여자 집합 `group_goal_participants`(ElementCollection) — **순위 없이** 누가 함께했는지만 기록(협력감)
  - `start(...)` 팩토리, `recordProgress(amount, contributorId)` — 진척 누적·참여자 추가, 목표 달성 시 1회 `GroupGoalAchievedEvent` 발행(멱등)
- `GoalPeriod`, `GoalStatus` enum
- `GroupGoalAchievedEvent`(domain/event)
### 리포지토리
- `GroupGoalRepository`(domain) + `JpaGroupGoalRepository` + `GroupGoalRepositoryAdapter`
  - `save`, `findById`, `findActiveByAlbumId(albumId, at)`(기간 포함 & IN_PROGRESS), `findByAlbumIdAndPeriod`
### 애플리케이션
- `GroupGoalApplicationService`
  - `getCurrentGoal(query)` → `GroupGoalResult`
  - `recordContribution(cmd)` — 활성 목표 없으면 주간 목표 **지연 자동 생성**(스케줄러 회피) 후 진척
  - `getHighlightCard(query)` → `HighlightCardResult`(그룹 총 게시·어르신 답변·가장 사랑받은 추억 1건, 개인 랭킹 없음)
- command/query: `RecordContributionCommand`, `GetCurrentGoalQuery`, `GetHighlightCardQuery`
- dto: `GroupGoalResult`, `HighlightCardResult`
### 인프라 이벤트
- `GroupGoalEventListener` — `MemoryPostPublishedEvent`(+1, 작성자), `ElderRepliedEvent`(+1, 어르신) 구독 → `recordContribution`
  (기존 `MemoryPostEventListener`는 알림 전용, 건드리지 않음. 다중 리스너 허용)
### 프레젠테이션
- `GroupGoalController` `/api/v1/albums/{albumId}/group-goal` (현재 목표), `/highlight` (하이라이트 카드). Tag `M2-GroupGoal`
- `OpenApiConfig`에 `M2-GroupGoal` 태그 추가
### 마이그레이션
- `V12__m2_group_goal.sql`: `group_goals`, `group_goal_participants` 생성

## 3~5. 검증
- 단위: `GroupGoalTest`(진척 누적/달성 멱등/이벤트/참여자 중복 제거), `GroupGoalApplicationServiceTest`(자동생성·진척·하이라이트)
- 통합: `./gradlew clean test` 전량 green, `FlywayMigrationTest` 마이그레이션 12개 + `group_goals` 존재 검증

## 6~9
- 커밋 분리: (a) 도메인+리포지토리+마이그레이션 (b) 애플리케이션+이벤트+컨트롤러+OpenAPI (c) 테스트+플랜
- Draft PR, base develop, 스택 순서 명시(#39→#40→#41→#42→**#43**), Closes #43

## Out-of-scope (릴리스 게이트 #50 이월)
- S1 회귀 스위트(EX-*) — #50 (Developer B 담당분)
- 어르신 상태 관리·사별 처리(#36) 의존 로직 — 본 이슈는 #36에 의존하지 않음
