# 개발 플랜 — #85 알림 스케줄러 전체 앨범 로딩 페이징 처리

> refactor · PR #83 리뷰 후속 · #84 위에 스택. `AlbumRepository.findAll()` 전량 적재 제거.

## 1. 이슈 분석

`AlbumRepository.findAll()`은 앨범 전체를 한 번에 메모리로 올린다. 사용처를 세어 보니 스케줄러 3곳만이 아니었다.

| 사용처 | 성격 | 문제 |
| --- | --- | --- |
| `ReminiscenceScheduler` (08:00) | 배치 | 전량 적재 후 앨범당 LLM 호출 |
| `EveningNotificationScheduler` (19:00) | 배치 | 전량 적재 |
| `DailyTrainingScheduler` (09:00) | 배치 | 전량 적재 + 앨범마다 `getPhotos()` 지연 로딩 |
| `AlbumPhotoOwnershipAdapter` | **요청 경로** | 인물 태깅 요청마다 전체 앨범 + 전체 사진 스캔 |
| `RepositoryPredownloadContentAdapter` | 배치 | 전량 적재 후 elderProfileId만 추출 |

스케줄러보다 `AlbumPhotoOwnershipAdapter`가 더 나쁘다. 사용자 요청 한 건마다 전 테이블을 훑는다. 같은 뿌리(전량 적재 API)의 문제이므로 이번에 함께 정리한다.

`ReminiscenceScheduler`에는 원래 "실제로는 활성 앨범만 처리하는 페이징 쿼리 필요" 주석이 있었으나 PR #82에서 주석만 지워지고 문제는 남았다.

## 2. 설계 판단

### 2-1. 포트에서 `findAll()`을 없앤다

호출부만 고치면 다음 사람이 다시 `findAll()`을 쓴다. 포트에서 제거해 재발 경로 자체를 막는다. 각 사용처가 실제로 필요한 만큼만 가져가는 메서드로 대체한다.

| 새 메서드 | 용도 |
| --- | --- |
| `findPage(page, size)` | id 오름차순 페이지 순회 |
| `findPageWithAtLeastPhotos(minPhotos, page, size)` | 사진 N장 이상 앨범만 |
| `existsPhotoInGroup(groupId, photoId)` | 소유권 검증 단건 |
| `findDistinctElderProfileIds()` | 선다운로드 대상 ID만 |

### 2-2. offset 페이징을 쓴다

keyset(`id > :lastId`) 페이징이 이론상 낫지만, PK가 UUID라 정렬 기준이 어차피 의미 없는 순서다. 야간 배치의 앨범 규모에서 offset 비용은 무시할 수 있고, H2(PostgreSQL 모드) 테스트 환경까지 이식성이 확실한 쪽을 택한다.

### 2-3. `AlbumBatchScanner`로 순회 로직을 한 곳에 모은다

페이지 루프를 스케줄러 3곳에 복붙하지 않는다. 배치 크기(100)도 한 곳에서 관리한다.

### 2-4. `EveningNotificationScheduler`에는 긴 읽기 트랜잭션을 붙이지 않는다

이슈 등록 시에는 `DailyTrainingScheduler`와의 "일관성"을 이유로 `@Transactional(readOnly = true)` 추가를 적었는데, 코드를 다시 보니 근거가 틀렸다.

`DailyTrainingScheduler`가 트랜잭션을 가진 이유는 일관성이 아니라 `album.getPhotos()` 지연 로딩 때문이다. `EveningNotificationScheduler`는 `getId()`·`getGroupId()`만 읽어 지연 로딩이 없다. 여기에 트랜잭션을 붙이면 전체 순회 동안 커넥션을 붙잡을 뿐 얻는 게 없다.

그래서 반대로 간다. 사진 수 필터를 `SIZE(a.photos) >= :min` 쿼리로 내려 `DailyTrainingScheduler`의 **지연 로딩을 없애고 트랜잭션도 제거**한다. 두 스케줄러 모두 트랜잭션 없이 페이지 단위로 도는 형태로 맞춰진다.

## 3. 변경 파일

- `m1/domain/repository/AlbumRepository` — `findAll()` 제거, 대체 메서드 4종
- `m1/infrastructure/persistence/{AlbumRepositoryAdapter, JpaAlbumRepository}` — 구현·쿼리
- `m1/application/service/AlbumBatchScanner` — 신규
- `m1/infrastructure/scheduler/ReminiscenceScheduler` — 스캐너 사용
- `m2/infrastructure/scheduler/EveningNotificationScheduler` — 스캐너 사용
- `m3/infrastructure/scheduler/DailyTrainingScheduler` — 사진 수 필터 쿼리 사용, 트랜잭션 제거
- `m1/infrastructure/persistence/AlbumPhotoOwnershipAdapter` — 단건 exists 쿼리
- `predownload/infrastructure/RepositoryPredownloadContentAdapter` — ID 프로젝션 쿼리

## 4. 검증

- `findAll()`이 포트에서 사라졌으므로 컴파일 자체가 회귀 가드
- 스캐너가 페이지를 나눠 요청하고 마지막 빈 페이지에서 멈추는지 단위 테스트
- 스케줄러 3종이 페이지 API만 호출하는지 (`findAll` 미호출) 검증
- `./gradlew test --rerun-tasks` green

### 4-1. 새 쿼리는 반드시 실제 DB로 검증한다

스케줄러 단위 테스트는 저장소를 목으로 두므로 JPQL이 맞는지 전혀 확인하지 못한다. `SIZE(a.photos)`, 조인 기반 `existsPhotoInGroup`, `DISTINCT` 프로젝션 모두 문법이 통과해도 의미가 틀릴 수 있다.

특히 `existsPhotoInGroup`은 **다른 그룹의 사진을 거부하는 보안 검증**이다. Java 필터링을 쿼리로 옮기면서 테스트 없이 넘어가면 안 된다. `AlbumRepositoryAdapterIntegrationTest`로 네 쿼리를 실제 H2에 대고 확인한다.

## 5. Out-of-scope

- `device_tokens.member_id` 타입 정합성 → #86
