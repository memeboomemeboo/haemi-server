# 크로스 플랫폼 푸시 알림 설계 (FCM 단일 게이트웨이)

> 상태: **설계(리뷰 대기)** · 대상: Android + iOS · 게이트웨이: Firebase Cloud Messaging(FCM) 단일
> 관련 배경: 현재 발송은 `LogNotificationAdapter`/`LogPushNotificationAdapter` 로그 스텁뿐이며,
> 기기 토큰 저장 모델과 FCM 의존성이 전무하다. 실발송 불가 상태를 해소하는 것이 목표.

## 1. 목표와 범위

**목표**: `NotificationPort.sendToMember/sendToGroup` 호출이 실제 사용자 단말(Android/iOS)로
푸시 알림을 전달하도록 만든다.

**In scope**
- 기기 토큰 등록/해제 API + 저장 모델(`member ↔ token ↔ platform`)
- FCM 발송 어댑터(Firebase Admin SDK) — Android/iOS 통합
- 프로파일 기반 어댑터 선택(dev=로그, prod=FCM)
- 무효 토큰 자동 정리(FCM Unregistered 응답 처리)

**Out of scope (후속)**
- Web Push (코드 변경 없이 platform enum에 `WEB` 추가로 확장 가능하도록 설계만 열어둠)
- 알림 클릭 딥링크 라우팅, 알림 센터/이력 저장, 사용자별 알림 채널 세분화
- 다국어 알림 템플릿(현재 문구는 호출부에서 생성)

## 2. 왜 FCM 단일인가

- FCM 하나로 Android는 직접, **iOS는 FCM이 내부적으로 APNs로 중계**한다. 서버는 플랫폼과
  무관하게 "토큰 + title/body"만 다루면 된다.
- iOS를 위해 필요한 것은 **Apple 개발자 계정의 APNs 인증키(.p8)를 Firebase 콘솔에 1회 등록**하는
  운영 작업뿐이며, 서버 코드/발송 경로는 Android와 동일하다.
- 직접 APNs 연동 대비 인증서 갱신·연결관리·페이로드 분기 부담이 없다.

## 3. 아키텍처 개요

기존 헥사고날 구조(도메인 포트 ↔ 인프라 어댑터)를 그대로 따른다. 신규 `push` 모듈을 만들고,
발송은 **기존 `m1.domain.port.NotificationPort`를 그대로 유지**하되 구현체만 교체/추가한다.

```
com.memeboo2.haemi.push
├── domain
│   ├── model/DeviceToken.java          # 엔티티 (member_id, token, platform, timestamps)
│   ├── model/DevicePlatform.java       # enum: ANDROID, IOS (WEB 확장 여지)
│   └── repository/DeviceTokenRepository.java   # 도메인 포트
├── application
│   └── DeviceTokenService.java         # 등록/해제/조회(멱등 upsert)
├── infrastructure
│   ├── persistence/JpaDeviceTokenRepository.java
│   ├── persistence/DeviceTokenRepositoryAdapter.java
│   ├── fcm/FirebaseConfig.java         # FirebaseApp 초기화 빈
│   └── fcm/FcmNotificationAdapter.java # NotificationPort 구현 (prod)
└── presentation
    ├── DeviceTokenController.java       # 등록/해제 API
    └── dto/RegisterDeviceTokenRequest.java
```

**포트 통합 결정**: 현재 알림 포트가 둘(`m1 NotificationPort`, `m2 PushNotificationPort`)로 중복이다.
`m2 PushNotificationPort`는 시그니처가 동일하므로 **`NotificationPort`로 일원화**하고
`LogPushNotificationAdapter`/`PushNotificationPort`는 제거한다(호출부는 `NotificationPort` 주입으로 변경).
→ 별도 정리 커밋으로 분리.

## 4. 데이터 모델

### 4.1 테이블 (Flyway `V22__push_device_tokens.sql`)

```sql
-- 크로스 플랫폼 푸시 기기 토큰 (FCM)
CREATE TABLE device_tokens (
    id           UUID         PRIMARY KEY,
    member_id    UUID         NOT NULL,
    token        VARCHAR(512) NOT NULL,
    platform     VARCHAR(20)  NOT NULL,          -- ANDROID | IOS
    created_at   TIMESTAMP    NOT NULL,
    last_seen_at TIMESTAMP    NOT NULL,
    CONSTRAINT uq_device_tokens_token UNIQUE (token)
);

-- memberId → 활성 토큰 다건 조회(발송 시 fan-out)
CREATE INDEX idx_device_tokens_member ON device_tokens (member_id);
```

- **token 유일**: 같은 기기가 재로그인/재설치로 토큰을 재발급하면 upsert로 `member_id`·`last_seen_at`
  갱신(기기 소유자 이전 대응). FCM 토큰은 최대 ~256자이나 여유롭게 512.
- 별도 `active` 컬럼 없이 **행 존재 = 활성**. 무효 토큰은 발송 실패 시 삭제(§6).
- 앱 실행/로그인 시마다 `last_seen_at` 갱신 → 장기 미사용 토큰 정리 배치의 근거(후속).

### 4.2 엔티티 요지

```java
@Entity @Table(name = "device_tokens")
public class DeviceToken {
    @Id private UUID id;
    private UUID memberId;
    private String token;
    @Enumerated(EnumType.STRING) private DevicePlatform platform;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeenAt;

    public static DeviceToken register(UUID memberId, String token, DevicePlatform platform) { ... }
    public void touch(UUID memberId, DevicePlatform platform, LocalDateTime now) { ... } // upsert 갱신
}
```

## 5. API 스펙

베이스: `/api/v1/push` · 인증 필요(`@AuthenticationPrincipal AuthenticatedMember`) ·
**memberId는 요청 body가 아닌 인증 주체로 고정**(적립 인가와 동일 원칙). ELDER·FAMILY 모두
자기 기기 토큰을 등록한다(§11.2).

| 메서드 | 경로 | 설명 | 요청 | 응답 |
|---|---|---|---|---|
| POST | `/api/v1/push/tokens` | 기기 토큰 등록(멱등 upsert) | `{ token, platform }` | 201, `ApiResponse<Void>` |
| DELETE | `/api/v1/push/tokens` | 로그아웃/알림 해제 시 토큰 삭제 | `{ token }` | 200, `ApiResponse<Void>` |

```jsonc
// POST /api/v1/push/tokens
{ "token": "fMEP0...:APA91b...", "platform": "IOS" }
```

- 동일 `token` 재등록 → 기존 행의 `member_id`/`platform`/`last_seen_at` 갱신(중복 생성 X).
- 유효성: `token` NotBlank, `platform` enum(ANDROID/IOS).

## 6. 발송 흐름

```
NotificationPort.sendToMember(memberId, title, body)
  └─ FcmNotificationAdapter
       1. deviceTokenRepository.findByMemberId(UUID)  → List<DeviceToken>
       2. 토큰 없음 → no-op + debug 로그(정상: 미설치 사용자)
       3. FirebaseMessaging.sendEachForMulticast(MulticastMessage[title,body,tokens])
       4. 응답 순회:
          - 성공 → last_seen 유지
          - 실패코드 UNREGISTERED / INVALID_ARGUMENT(토큰 무효) → 해당 token 삭제(자동 정리)
          - 기타 일시 오류 → 경고 로그(다음 발송에서 재시도, 여기서는 삼키지 않고 카운트만)
       5. Micrometer 카운터 기록: push.sent / push.failed / push.token.pruned (platform 태그)
```

- `sendToGroup(Set<memberId>)`: 각 memberId 토큰을 모아 **한 번의 multicast**로 발송(최대 500토큰/콜,
  초과 시 청크 분할).
- **트랜잭션 경계**: 발송은 외부 I/O이므로 호출 도메인 트랜잭션과 분리(비동기 권장). 기존 M2가
  `@Async @EventListener`로 발송하는 패턴과 정합. 발송 실패가 도메인 작업을 롤백시키지 않도록 한다.
- **부분 실패 격리**: 한 토큰 실패가 다른 수신자 발송을 막지 않는다(`sendEachForMulticast`는 개별 결과 반환).

## 7. FCM 어댑터 & 설정

### 7.1 의존성 (`build.gradle`)
```gradle
implementation 'com.google.firebase:firebase-admin:9.4.1'
```

### 7.2 초기화 (`FirebaseConfig`)
- 서비스 계정 JSON을 **환경변수/파일 경로**로 주입(`GOOGLE_APPLICATION_CREDENTIALS` 또는
  `haemi.push.fcm.credentials-path`). 자격증명은 **레포에 커밋 금지**(시크릿 관리).
- `@Bean FirebaseMessaging` 등록. 앱당 1회 `FirebaseApp.initializeApp`.

### 7.3 프로파일 기반 어댑터 선택
```yaml
# application.yaml (공통 기본)
haemi:
  push:
    provider: log            # dev 기본
# application-prod.yaml
haemi:
  push:
    provider: fcm
    fcm:
      credentials-path: ${FCM_CREDENTIALS_PATH}
```
- `@ConditionalOnProperty(name="haemi.push.provider", havingValue="fcm")` → `FcmNotificationAdapter`
- 그 외 → 기존 `LogNotificationAdapter`(dev/test는 로그로 계속 확인)
- 이렇게 하면 **테스트/개발은 지금처럼 로그**, 운영만 실발송.

## 8. 테스트 전략

- `DeviceTokenService` 단위 테스트: 신규 등록 / 동일 토큰 upsert / 소유자 이전 / 삭제.
- `DeviceTokenController` 웹 테스트: 인증 주체 고정(body memberId 무시), 유효성 400.
- `FcmNotificationAdapter` 단위 테스트: `FirebaseMessaging` 목킹 → 토큰 fan-out, UNREGISTERED 응답 시
  토큰 삭제, 부분 실패 격리. **실제 FCM 호출 없음**.
- 회귀: 기존 발송 호출부(M2 이벤트 리스너, M5 알람)는 포트 시그니처 불변이므로 영향 없음.

## 9. 롤아웃 단계

1. **스키마 + 토큰 API**: `device_tokens` 마이그레이션 + 등록/해제 API + 서비스/저장소. (실발송 무관, 안전)
2. **포트 일원화**: `m2 PushNotificationPort` 제거, `NotificationPort`로 통합.
3. **FCM 어댑터 + 설정**: 의존성·`FirebaseConfig`·`FcmNotificationAdapter`·프로파일 스위치.
   운영 시크릿(서비스계정 JSON, APNs .p8 콘솔 등록)은 배포 파이프라인에서 주입.
4. **클라이언트 연동**: 앱이 로그인 후 토큰 등록 호출 / 로그아웃 시 삭제 호출.
5. **운영 전환**: prod 프로파일 `provider=fcm`.

## 10. 브랜치/PR 계획 (스택 PR 컨벤션)

신규 이슈(예: `feat/f0xx-push-notification/#NN`)로 분리. #58과 독립.
커밋은 §9 단계에 맞춰 분할:
- `feat: 기기 토큰 저장 모델·등록/해제 API (Fx-xx)`
- `refactor: 알림 포트 NotificationPort 일원화`
- `feat: FCM 발송 어댑터·프로파일 스위치 (Fx-xx)`

## 11. 결정 사항

아래는 설계 판단으로 **확정**한다. 외부 입력이 필요한 항목만 §11.5에 남긴다.

1. **운영 시크릿 주입** — 서비스 계정 JSON을 **레포 외부 파일**로 두고 경로를
   환경변수로 주입한다: `haemi.push.fcm.credentials-path=${FCM_CREDENTIALS_PATH}`
   (미설정 시 표준 `GOOGLE_APPLICATION_CREDENTIALS` fallback). JSON은 커밋 금지, 배포
   파이프라인/시크릿 스토어에서 파일로 마운트. 별도 Secret Manager 도입은 하지 않는다(과설계 회피).
2. **단말 정책** — **ELDER·FAMILY 모두 자기 기기 토큰을 등록**하고, 발송은 `memberId → 토큰`으로
   역할과 무관하게 동일하게 처리한다. 어르신 알림(알람·추억글)은 어르신의 `memberId`로,
   가족 알림(확인·무응답)은 가족 `memberId`로 보낸다.
   → 이에 따라 **M2 `"elder-device"` / M5·M2의 하드코딩 수신자를 실제 어르신 `memberId`로 교체**하는
   작업이 선행 정리로 필요하다(별도 이슈, §10과 연결).
3. **발송 관측** — actuator가 이미 의존성에 있으므로 **Micrometer 카운터**를 추가한다:
   `push.sent`, `push.failed`, `push.token.pruned`(플랫폼 태그). 구조화 로그와 병행.
4. **APNs 환경** — sandbox/production 구분은 **Firebase 콘솔 APNs 설정으로 처리**하며 서버 코드는
   무관하다. 코드 변경 없음(문서화만).

### 11.5 여전히 외부 입력 필요
- **이슈 번호/브랜치명** — 신규 이슈 발급 후 `feat/f0xx-push-notification/#NN`으로 확정.
- **FCM 프로젝트/서비스 계정** — Firebase 프로젝트 생성 및 서비스 계정 JSON, iOS용 APNs 키(.p8)
  콘솔 등록은 운영 준비 작업(코드 외).

---

### 부록 A. 현재 상태 요약(설계 근거)
- 발송 구현체: `LogNotificationAdapter`(m1), `LogPushNotificationAdapter`(m2) — 로그만.
- FCM/APNs 의존성: 없음. 기기 토큰 저장: 없음. → **실발송 불가**가 확인된 상태.
- `NotificationPort` 인터페이스는 이미 분리되어 있어 어댑터 교체 지점은 깔끔(병목은 토큰 저장 모델).
