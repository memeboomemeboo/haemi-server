# 개발 플랜 — #86 device_tokens.member_id UUID 타입 정합성 정리

> chore · PR #83 리뷰 후속 · #85 위에 스택.

## 1. 이슈 분석

`device_tokens.member_id`가 `VARCHAR(255)`다. 다른 테이블의 회원 식별자는 모두 `UUID`이고, **같은 테이블의 `elder_id`조차 `UUID`**라 한 테이블 안에서도 타입이 갈린다.

### 1-1. 단순 타입 변경이 아니다

`member_id`를 `UUID`로 바꾸면 조회 파라미터도 `UUID`가 돼야 한다. 그런데 발송 경로로 흘러드는 수신자 ID가 전부 UUID인 게 아니다.

```java
notificationPort.sendToGroup(album.getAllMemberIds(), ...)   // 레거시 앨범 멤버 문자열
notificationPort.sendToGroup(recipients, ...)                // M4 기관 담당자 ID
```

`AlertRecipientSetting.institutionManagerMemberIds`는 API가 임의 문자열 `Set<String>`으로 받는다. 통합 테스트조차 `"manager-api-test"` 같은 값을 넣는다. 앨범 멤버 ID도 PR #82 이전에 만들어진 행은 임의 문자열일 수 있다.

즉 **경계에서 걸러내지 않으면** `UUID.fromString`이 던지는 예외가 `dispatchToMembers`의 `catch (Exception)`에 잡혀 **그 배치 전체 발송이 조용히 사라진다**. 수신자 한 명의 ID 형식 때문에 나머지 가족 전원이 알림을 못 받는다.

### 1-2. 마이그레이션 이식성 제약

마이그레이션은 운영(PostgreSQL)과 테스트(H2 PostgreSQL 모드) 양쪽에서 돌아야 한다. `FlywayMigrationTest`가 H2에 전 마이그레이션을 적용한다.

Postgres의 `ALTER COLUMN ... TYPE uuid USING member_id::uuid`는 H2가 `USING` 절을 지원하지 않아 쓸 수 없다.

## 2. 설계 판단

### 2-1. 포트는 UUID로, 변환은 발송 경계 한 곳에서

`DeviceTokenRepository`·`DeviceToken`·`DeviceTokenService`는 `UUID`로 통일한다. 컨트롤러는 이미 `AuthenticatedMember.memberId()`가 `UUID`라 `.toString()`이 사라진다.

반면 `NotificationPort.sendToGroup(Set<String>)`은 도메인 계약이라 그대로 둔다. 대신 `PushDispatchService`가 수신자 문자열을 UUID로 파싱하면서 **형식이 잘못된 ID만 건너뛰고 로그로 남긴다**. 나머지 수신자 발송은 그대로 진행한다.

### 2-2. 마이그레이션은 컬럼을 다시 만든다

`device_tokens`는 이번 릴리스 train(V22)에서 처음 생긴 테이블이라 운영 데이터가 없다. 기기 토큰은 클라이언트가 앱 실행 때 다시 등록하는 **일회성 데이터**이기도 하다.

그래서 문자열을 캐스팅해 살리는 대신, 남은 개발용 행을 비우고 컬럼을 다시 만든다. `USING` 절도, 비-UUID 행 처리용 DB별 정규식도 필요 없어 H2·Postgres 양쪽에서 같은 SQL이 돈다.

V22를 직접 고치는 방법도 있지만, 이미 develop을 받아 로컬 DB를 마이그레이션한 사람의 체크섬이 깨지므로 하지 않는다.

## 3. 변경 파일

- `db/migration/V24__device_token_member_id_to_uuid.sql` — 신규
- `notification/domain/DeviceToken` — `memberId` 타입, `isOwnedBy`
- `notification/domain/repository/DeviceTokenRepository` + 어댑터/JPA — 조회 파라미터
- `notification/application/DeviceTokenService` — 시그니처
- `notification/application/PushDispatchService` — 수신자 파싱·필터
- `notification/presentation/DeviceTokenController` — `.toString()` 제거
- 관련 테스트

## 4. 검증

- `FlywayMigrationTest` 마이그레이션 수 24로 갱신 + `member_id` 컬럼 타입 확인
- 비-UUID 수신자가 섞여도 **정상 수신자에게는 발송되는지** 단위 테스트 (이번 변경의 핵심 위험)
- 기기 토큰 등록·해지·조회 통합 테스트 그대로 통과
- `./gradlew test --rerun-tasks` green

## 5. Out-of-scope

- `AlertRecipientSetting.institutionManagerMemberIds`를 UUID로 강제하는 일. API 계약 변경이라 별도 이슈가 맞다. 이번에는 걸러내기만 한다.
