# 크로스 플랫폼 푸시 알림 설계 (FCM 단일 게이트웨이)

> 상태: **설계 v2(리뷰 반영)** · 대상: Android + iOS · 게이트웨이: Firebase Cloud Messaging(FCM) 단일
> 배경: 현재 발송은 `LogNotificationAdapter`/`LogPushNotificationAdapter` 로그 스텁뿐이며,
> 기기 등록 저장 모델과 FCM 의존성이 전무하다. **실발송 불가** 상태 해소가 목표.
>
> v2 변경(설계 리뷰 반영):
> - **[P0] 수신자 식별 모델 선행(Phase 0)** — 어르신 프로필→기기 소유 Member 명시 연결 +
>   `NotificationRecipientResolver`. "하드코딩 교체"는 별도 이슈가 아니라 FCM 전환의 선행 단계.
> - **[P1] 어댑터 빈 유일성** — Log 어댑터도 조건부 빈으로. 프로파일별 빈 1개 보장 context test.
> - **[P1] FID 우선 모델** — FCM이 registration token→Firebase Installation ID(FID)로 전환 중.
>   `DeviceToken`이 아니라 식별자 타입(FID/legacy)을 표현하는 `DeviceRegistration`, 기본값 FID.
> - **[P1] 자격증명 배포 연결** — compose에 secret 파일 마운트·환경변수 단계 명시.

## 1. 목표와 범위

**목표**: 도메인의 알림 발송 호출이 실제 사용자 단말(Android/iOS)로 전달되도록 만든다.

**In scope**
- **(Phase 0)** 수신자 식별 모델: 어르신 프로필 ↔ 기기 소유 Member 연결 + `NotificationRecipientResolver`
- 기기 등록 저장 모델(`member ↔ 등록식별자(FID/legacy) ↔ platform`) + 등록/해제 API
- FCM 발송 어댑터(Firebase Admin SDK, **FID 우선**) — Android/iOS 통합
- 프로파일 기반 어댑터 **정확히 1개** 보장(dev=로그, prod=FCM)
- 무효 등록 자동 정리(FCM Unregistered 응답 처리)
- 운영 자격증명 배포 경로(compose secret 마운트)

**Out of scope (후속)**: Web Push(platform enum 확장 여지만), 딥링크 라우팅, 알림 이력 저장,
장기 미사용 등록 정리 배치(모델·타임스탬프는 미리 넣어둠).

## 2. 왜 FCM 단일인가

- FCM 하나로 Android 직접, **iOS는 FCM이 내부적으로 APNs로 중계**. 서버는 플랫폼 무관하게
  "등록식별자 + title/body"만 다룬다.
- iOS는 **APNs 인증키(.p8)를 Firebase 콘솔에 1회 등록**하는 운영 작업만 필요, 서버 코드는 동일.
- 직접 APNs 연동 대비 인증서 갱신·연결관리·페이로드 분기 부담 없음.

## 3. 수신자 식별 모델 (Phase 0 · [P0] 선행)

### 3.1 문제
현재 알림 호출부는 **이질적인 문자열 식별자**를 `NotificationPort`에 넘긴다.
- `DailyTrainingScheduler` → `sendToMember(elderProfileId, …)` (어르신 프로필 ID)
- `CareApplicationService`(M5) → `sendToMember(alarm.getElderId(), …)`, `sendToGroup(familyMemberIds…)`
- `MemoryPostEventListener`(M2) → `sendToGroup(Set.of("elder-device"), …)` (**하드코딩**)

그리고 **`Elder`(m0)에는 Member 연결값이 없다.** 즉 어르신 프로필/그룹 식별자에서
"이 알림을 받을 실제 기기 소유 Member(UUID)"로 가는 경로가 없다. 이 상태로 FCM 어댑터가
`member_id UUID` 기준으로 등록을 조회하면 **대상 없음 또는 UUID 변환 예외로 발송 실패**한다.

### 3.2 해결: 명시적 연결 + Resolver
1. **어르신 프로필 → 기기 소유 Member 연결.** `Elder`에 어르신이 사용하는 단말의 Member 계정
   (ELDER 역할)을 연결한다. 마이그레이션으로 컬럼 추가:
   ```sql
   ALTER TABLE elders ADD COLUMN device_owner_member_id UUID;  -- nullable(미배정 허용)
   ```
   온보딩에서 어르신 단말 계정이 생성/연동될 때 채운다. 미배정이면 발송 대상 없음(정상 no-op).

2. **`NotificationRecipientResolver` 포트 도입.** 발송 대상을 **타입 있는 참조**로 표현하고
   실제 Member UUID 집합으로 변환한다.
   ```java
   sealed interface NotificationTarget {
       record Elder(String elderProfileId) implements NotificationTarget {}   // → device_owner_member_id
       record Member(UUID memberId) implements NotificationTarget {}          // 가족 개인
       record FamilyGroup(UUID groupId) implements NotificationTarget {}      // 그룹 활성 구성원 Member들
   }
   interface NotificationRecipientResolver {
       Set<UUID> resolve(NotificationTarget target);   // 없으면 빈 집합
   }
   ```

3. **호출부 이관.** 기존 stringly-typed 호출을 `NotificationTarget`으로 교체한다
   (`elderProfileId`→`Elder`, 하드코딩 `"elder-device"` 제거→`Elder`/`FamilyGroup`).
   `NotificationPort`는 최종적으로 `send(NotificationTarget, title, body)` 형태로 정리하고,
   어댑터가 resolver로 Member UUID를 얻은 뒤 등록식별자로 fan-out 한다.

> **범위 원칙**: Phase 0가 완료되기 전에는 FCM 어댑터를 prod에 붙이지 않는다. 수신자 모델 없이
> FCM만 켜면 곧바로 발송 실패가 나기 때문이다. "하드코딩 교체"는 이 Phase에 포함(별도 이슈 아님).

## 4. 기기 등록 저장 모델 ([P1] FID 우선)

### 4.1 배경 — registration token → FID
Firebase는 FCM을 **registration token에서 Firebase Installation ID(FID) 기반 등록으로 전환** 중이며,
Admin SDK Send API의 `token` 필드는 deprecated, `fid` 사용을 권장한다(둘 다 전환기 동안 co-support).
서버 역할은 "각 클라이언트의 **FID를 timestamp와 함께 보관**하고 활성 목록을 유지"하는 것.
→ 모델을 **식별자 타입을 표현**하도록 설계하고 **기본값을 FID**로 둔다.

### 4.2 테이블 (Flyway `V22__push_device_registrations.sql`)
```sql
-- 크로스 플랫폼 푸시 기기 등록 (FCM, FID 우선)
CREATE TABLE device_registrations (
    id              UUID         PRIMARY KEY,
    member_id       UUID         NOT NULL,
    identifier_type VARCHAR(20)  NOT NULL,          -- FID(기본) | LEGACY_TOKEN
    identifier      VARCHAR(512) NOT NULL,          -- FID 또는 registration token
    platform        VARCHAR(20)  NOT NULL,          -- ANDROID | IOS
    created_at      TIMESTAMP    NOT NULL,
    last_seen_at    TIMESTAMP    NOT NULL,
    CONSTRAINT uq_device_registrations_identifier UNIQUE (identifier_type, identifier)
);
CREATE INDEX idx_device_registrations_member ON device_registrations (member_id);
```
- **식별자 유일**: 같은 기기가 재등록하면 upsert로 `member_id`·`last_seen_at` 갱신(소유자 이전 대응).
- 행 존재 = 활성. 무효 등록은 발송 실패 시 삭제(§6). `last_seen_at`은 미사용 정리 배치 근거(후속).

### 4.3 엔티티 요지
```java
@Entity @Table(name = "device_registrations")
public class DeviceRegistration {
    @Id private UUID id;
    private UUID memberId;
    @Enumerated(EnumType.STRING) private RegistrationIdentifierType identifierType; // FID | LEGACY_TOKEN
    private String identifier;
    @Enumerated(EnumType.STRING) private DevicePlatform platform;                   // ANDROID | IOS
    private LocalDateTime createdAt;
    private LocalDateTime lastSeenAt;

    public static DeviceRegistration ofFid(UUID memberId, String fid, DevicePlatform p) { ... }
    public void touch(UUID memberId, LocalDateTime now) { ... }
}
```

## 5. API 스펙

베이스: `/api/v1/push` · 인증 필요(`@AuthenticationPrincipal AuthenticatedMember`) ·
**memberId는 body가 아닌 인증 주체로 고정**. ELDER·FAMILY 모두 자기 기기를 등록(§11.2).

| 메서드 | 경로 | 설명 | 요청 | 응답 |
|---|---|---|---|---|
| POST | `/api/v1/push/registrations` | 기기 등록(멱등 upsert) | `{ identifier, identifierType?, platform }` | 201 |
| DELETE | `/api/v1/push/registrations` | 로그아웃/해제 시 삭제 | `{ identifier }` | 200 |

```jsonc
// POST — identifierType 생략 시 FID 기본
{ "identifier": "c9f1e...FID...", "identifierType": "FID", "platform": "IOS" }
```
- 유효성: `identifier` NotBlank, `platform` enum, `identifierType` enum(기본 FID).
- 동일 identifier 재등록 → 기존 행 갱신(중복 생성 X).

## 6. 발송 흐름

```
send(NotificationTarget, title, body)
  └─ FcmNotificationAdapter
       1. Set<UUID> members = recipientResolver.resolve(target)   // §3
       2. members 비었으면 no-op + debug 로그(미배정/미설치)
       3. registrations = deviceRegistrationRepository.findByMemberIdIn(members)
       4. FID/legacy 별로 MulticastMessage 구성:
            - identifierType=FID       → message.fid(identifier)
            - identifierType=LEGACY    → message.token(identifier)   // 전환기 co-support
          FirebaseMessaging.sendEachForMulticast(...)  (500건/콜, 초과 시 청크)
       5. 응답 순회:
            - UNREGISTERED / INVALID_ARGUMENT → 해당 등록 삭제(자동 정리)
            - 기타 일시 오류 → 경고 로그(카운트만, 삼키지 않음)
       6. Micrometer 카운터: push.sent / push.failed / push.registration.pruned (platform·type 태그)
```
- **트랜잭션 경계**: 외부 I/O이므로 도메인 트랜잭션과 분리(비동기 권장, M2의 `@Async @EventListener`
  패턴과 정합). 발송 실패가 도메인 작업을 롤백하지 않는다.
- **부분 실패 격리**: `sendEachForMulticast`는 개별 결과를 반환 → 한 등록 실패가 타 수신자에 무영향.

## 7. FCM 어댑터 & 설정

### 7.1 의존성 (`build.gradle`)
```gradle
// FID 전송(Send API의 fid 필드)을 지원하는 버전 필요. 최신 릴리스로 채택하고 릴리스 노트로 확정.
implementation 'com.google.firebase:firebase-admin:9.10.0'   // 최소 하한(현행 최신으로 상향 검토)
```
> 주: 기존 초안의 `9.4.1`은 `fid` Send API 미지원이라 채택 불가. 도입 시 릴리스 노트에서
> `Message.fid(...)`/`MulticastMessage` FID 지원 버전을 확인해 **그 이상**으로 고정한다.

### 7.2 초기화 (`FirebaseConfig`)
- 서비스 계정 JSON을 **레포 외부 파일**로 두고 경로 주입:
  `haemi.push.fcm.credentials-path=${FCM_CREDENTIALS_PATH}` (미설정 시 표준
  `GOOGLE_APPLICATION_CREDENTIALS` fallback). JSON은 커밋 금지.
- `@Bean FirebaseMessaging`(앱당 1회 `FirebaseApp.initializeApp`).

### 7.3 프로파일 기반 어댑터 — 정확히 1개 보장 ([P1])
`NotificationPort`를 단일 생성자 인자로 주입받는 서비스가 많으므로 **빈이 정확히 하나**여야 한다.
FCM만 조건부로 추가하고 Log를 무조건 `@Component`로 두면 prod에서 빈 2개 →
`NoUniqueBeanDefinitionException`으로 **기동 실패**. 따라서 **양쪽 모두 조건부**로 둔다.
```java
@Component
@ConditionalOnProperty(name = "haemi.push.provider", havingValue = "fcm")
class FcmNotificationAdapter implements NotificationPort { ... }

@Component
@ConditionalOnProperty(name = "haemi.push.provider", havingValue = "log", matchIfMissing = true)
class LogNotificationAdapter implements NotificationPort { ... }   // 기본값 log
```
```yaml
# application.yaml (기본)         → provider 미지정 == log
# application-prod.yaml
haemi: { push: { provider: fcm, fcm: { credentials-path: ${FCM_CREDENTIALS_PATH} } } }
```
- **포트 일원화**: 중복 `m2 PushNotificationPort`/`LogPushNotificationAdapter`는 제거하고
  `NotificationPort`로 통합(별도 정리 커밋).

## 8. 테스트 전략
- `DeviceRegistrationService` 단위: 신규/동일 identifier upsert/소유자 이전/삭제, FID·legacy 혼재.
- `DeviceRegistrationController` 웹: 인증 주체 고정(body memberId 무시), 유효성 400.
- `NotificationRecipientResolver` 단위: Elder(연결 있음/없음), Member, FamilyGroup(활성 구성원만).
- `FcmNotificationAdapter` 단위: `FirebaseMessaging` 목킹 → fan-out, FID/legacy 분기,
  UNREGISTERED 시 등록 삭제, 부분 실패 격리(실제 FCM 호출 없음).
- **[P1] 빈 유일성 context test**: `default`(=log)와 `prod`(=fcm) 각각에서
  `ApplicationContext`의 `NotificationPort` 빈이 **정확히 1개**임을 검증(빈 개수 assert).
- 회귀: 발송 호출부는 Phase 0에서 `NotificationTarget`으로 이관되므로 그 커밋 범위에서 검증.

## 9. 롤아웃 단계
0. **[선행] 수신자 모델**: `elders.device_owner_member_id` 마이그레이션 + `NotificationRecipientResolver`
   + 호출부 `NotificationTarget` 이관(하드코딩 제거). *FCM 무관, 로그 어댑터로 동작 검증.*
1. **등록 저장 모델 + API**: `device_registrations`(FID 우선) + 등록/해제 API + 서비스/저장소.
2. **포트 일원화**: `m2 PushNotificationPort` 제거, `NotificationPort` 통합 + 빈 유일성 조건/테스트.
3. **FCM 어댑터 + 설정**: 의존성(≥9.10.0)·`FirebaseConfig`·`FcmNotificationAdapter`·프로파일 스위치.
4. **자격증명 배포 연결([P1])**: 아래 §9.1.
5. **클라이언트 연동**: 앱이 로그인 후 FID 등록/로그아웃 시 해제 호출.
6. **운영 전환**: prod `provider=fcm`.

### 9.1 운영 자격증명 주입 (compose)
현재 `compose.yaml`에는 `FCM_CREDENTIALS_PATH`·secret 볼륨이 없다. 다음을 롤아웃에 포함한다.
- 배포 파이프라인에서 **GitHub Secret의 서비스 계정 JSON**을 배포 서버에 **제한 권한(0600)** 파일로 생성.
- 컨테이너에 **read-only 마운트** + 환경변수 주입:
```yaml
# compose.yaml (app 서비스에 추가)
environment:
  FCM_CREDENTIALS_PATH: /run/secrets/fcm/service-account.json
volumes:
  - /opt/haemi/secrets/fcm-service-account.json:/run/secrets/fcm/service-account.json:ro
```
- 배포 스크립트가 서버 설정을 EC2에 동기화하는 기존 흐름(커밋 `86e3166` 참고)에 secret 파일
  생성·권한 설정 단계를 추가한다.

## 10. 브랜치/PR 계획 (스택 PR 컨벤션)
신규 이슈(예: `feat/f0xx-push-notification/#NN`)로 분리, #58과 독립. 커밋은 §9 단계에 맞춰:
- `feat: 알림 수신자 식별 모델·RecipientResolver, 하드코딩 수신자 제거 (Phase 0)`
- `feat: 기기 등록 저장 모델(FID 우선)·등록/해제 API`
- `refactor: 알림 포트 NotificationPort 일원화·빈 유일성 조건`
- `feat: FCM 발송 어댑터·프로파일 스위치`
- `chore: FCM 자격증명 배포 마운트`

## 11. 결정 사항
1. **시크릿 주입** — 레포 외부 파일 + `FCM_CREDENTIALS_PATH`(표준 fallback), compose read-only 마운트(§9.1).
   별도 Secret Manager 도입은 하지 않음.
2. **단말 정책** — ELDER·FAMILY 모두 등록, `NotificationTarget`→resolver→Member UUID로 통일 발송.
   어르신은 `elders.device_owner_member_id`로 연결.
3. **관측** — Micrometer 카운터(push.sent/failed/registration.pruned, platform·type 태그).
4. **APNs 환경** — Firebase 콘솔 APNs 설정으로 처리, 서버 코드 무관.
5. **등록 식별자** — **FID 기본**, legacy token은 전환기 co-support로 병행 수용.

### 11.5 외부 입력 필요
- 이슈 번호 발급 → 브랜치명 정합.
- Firebase 프로젝트/서비스 계정 JSON, iOS APNs 키(.p8) 콘솔 등록, firebase-admin 최신 버전 확정.
- 어르신 단말 계정 온보딩 흐름(`device_owner_member_id` 채우는 시점) 확정.

---

### 부록 A. 현재 상태(설계 근거)
- 발송 구현체: `LogNotificationAdapter`(m1, 무조건 @Component), `LogPushNotificationAdapter`(m2) — 로그만.
- `Elder`에 Member 연결 없음. 호출부는 `elderProfileId`·`"elder-device"` 등 이질적 식별자 전달.
- FCM/APNs 의존성 없음, 기기 등록 저장 없음 → **실발송 불가**.
- `compose.yaml`에 FCM 자격증명 주입 경로 없음.

### 부록 B. 참고
- Firebase — Best practices for FCM registration management (FID 전환·서버는 FID 보관 권장)
- Firebase — Send messages with the Firebase Admin SDK
