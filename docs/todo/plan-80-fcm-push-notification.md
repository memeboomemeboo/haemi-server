# 개발 플랜 — #80 FCM 푸시 알림 발송 연동

> feat · 공통(notification) · develop 기준 신규 브랜치. 로그 스텁으로만 존재하던 알림 발송을 실제 FCM 발송으로 교체한다.

## 1. 이슈 분석

현재 알림 경로는 두 포트로 갈라져 있고 구현체는 로그 스텁뿐이다.

| 포트 | 사용처 |
| --- | --- |
| `m1.domain.port.NotificationPort` | M1 사진/앨범/회상, M2 추억 알림·저녁 스케줄러·이벤트 리스너, M3 훈련 스케줄러·리스너, M4 대시보드, M5 케어 |
| `m2.domain.port.PushNotificationPort` | M2 전용 (현재 주입처 없음, 스텁만 존재) |

빠진 조각은 세 가지다.
1. **보낼 주소가 없다** — 기기 토큰 테이블·등록 API 부재.
2. **보낼 수단이 없다** — Firebase Admin SDK 의존성·자격증명 설정 부재.
3. **보내면 위험하다** — 발송은 `@Transactional` 서비스 안에서 동기 호출된다. 외부 HTTP 호출이 트랜잭션을 물고 있으면 커넥션 점유·타임아웃 전파가 생긴다.

## 2. 설계

포트는 M1/M2 모듈에 남기되(호출부 변경 없음), 실제 발송은 공통 `notification` 모듈로 모은다.

```
notification/
  domain/
    DeviceToken.java            엔티티 (token PK, memberId, platform, timestamps)
    DevicePlatform.java         ANDROID / IOS / WEB
    PushMessage.java            title, body, data
    PushSendResult.java         성공 수 + 무효 토큰 목록
    port/PushSenderPort.java    실제 전송 경계
    repository/DeviceTokenRepository.java
  application/
    DeviceTokenService.java     등록(업서트)/해지/조회
    PushDispatchService.java    memberId → 토큰 조회 → 전송 → 무효 토큰 정리
  infrastructure/
    FirebaseConfig.java         자격증명이 있을 때만 FirebaseApp 초기화
    FcmPushSenderAdapter.java   FirebaseMessaging 멀티캐스트
    LoggingPushSenderAdapter.java  @ConditionalOnMissingBean 폴백
    NotificationAsyncConfig.java   전용 executor (@EnableAsync)
    persistence/                Jpa + Adapter
  presentation/
    DeviceTokenController.java  POST/DELETE /api/v1/device-tokens
```

### 핵심 결정

- **폴백 우선.** `FcmPushSenderAdapter`는 `FirebaseMessaging` 빈이 있을 때만 등록되고, 없으면 `LoggingPushSenderAdapter`가 뜬다. 자격증명 없는 로컬/테스트/CI는 기존 로그 동작 그대로 — 기동 실패 없음.
- **토큰 소유자는 인증 주체.** 요청 본문의 memberId를 믿지 않고 `@AuthenticationPrincipal`에서 가져온다. 같은 토큰이 다른 계정으로 재등록되면 소유자를 이전한다(기기 재로그인 시나리오).
- **무효 토큰 정리.** FCM 응답의 `UNREGISTERED` / `INVALID_ARGUMENT` / `SENDER_ID_MISMATCH`만 삭제한다. `UNAVAILABLE`·`INTERNAL` 같은 일시 오류로는 지우지 않는다.
- **비동기 오프로드.** 포트 어댑터(`FcmNotificationAdapter`, `FcmPushNotificationAdapter`)의 발송 메서드에 `@Async("notificationExecutor")`를 걸어 호출자 트랜잭션에서 떼어낸다. 큐가 차면 `CallerRunsPolicy`로 흘려보낸다.
- **실패 격리.** `PushDispatchService`는 전송 예외를 삼키고 로그만 남긴다. 알림 실패로 사진 업로드가 실패하면 안 된다.
- **500개 청크.** FCM 멀티캐스트 상한에 맞춰 토큰을 500개씩 나눠 보낸다.

### 마이그레이션 `V22__device_tokens.sql`

```sql
CREATE TABLE device_tokens (
    token        VARCHAR(255) PRIMARY KEY,
    member_id    VARCHAR(255) NOT NULL,
    platform     VARCHAR(10)  NOT NULL,
    registered_at TIMESTAMP   NOT NULL,
    last_used_at  TIMESTAMP   NOT NULL
);
CREATE INDEX idx_device_tokens_member ON device_tokens (member_id);
```

### 설정

```yaml
haemi:
  notification:
    fcm:
      credentials: ${FIREBASE_CREDENTIALS:}       # 서비스 계정 JSON 원문 또는 파일 경로
      project-id: ${FIREBASE_PROJECT_ID:}
```

`build.gradle`에 `com.google.firebase:firebase-admin` 추가, `.env.example`에 선택 항목으로 기재.

## 3. 개발 순서 / 커밋 분리

1. `feat: 기기 토큰 도메인·저장소·등록 API 추가` — 엔티티/리포지토리/서비스/컨트롤러 + V22 마이그레이션
2. `feat: FCM 발송 어댑터와 무효 토큰 정리 연동` — Firebase 설정·sender·dispatch·비동기 설정 + 포트 어댑터 교체 + build.gradle/설정
3. `test: FCM 푸시 알림 단위·통합 테스트 추가` — 테스트 + 플랜 문서

## 4~5. 검증

- **단위**: `DeviceTokenTest`(업서트·소유자 이전·사용 시각 갱신), `DeviceTokenServiceTest`, `PushDispatchServiceTest`(토큰 없음 → 미발송, 무효 토큰 삭제, 일시 오류는 보존, 전송 예외 삼킴), `FcmNotificationAdapterTest`(그룹 발송 위임).
- **통합**: `DeviceTokenApiIntegrationTest`(등록/중복 등록/해지/미인증 401/타인 토큰 해지 차단), `FlywayMigrationTest` 22개로 상향 + `device_tokens` 테이블 검증, `./gradlew clean test` green.

## Out-of-scope

- `NotificationPreference`(ALL/IMPORTANT_ONLY/NONE) 반영 — 포트 시그니처에 중요도 인자가 없어 전체 호출부 변경이 필요하다. 별도 이슈로 분리.
- APNs 직접 연동 — iOS는 FCM 경유로 처리한다.
- 발송 이력 저장/재시도 큐.
