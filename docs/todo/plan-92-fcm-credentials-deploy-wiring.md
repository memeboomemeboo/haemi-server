# 개발 플랜 — #92 운영 배포 FCM 자격증명 배선

> fix · develop 전체 검토 후속 · 스택 최하단(base = develop).

## 1. 이슈 분석

### 1-1. 어디서 끊겼나

자격증명이 앱까지 도달하는 경로는 네 단계다.

```
GitHub Secret → deploy-prod.yml(envs, save_env) → .env → compose.yaml(environment) → application.yaml
```

`GEMINI_API_KEY`는 네 단계가 모두 이어져 있다. `FIREBASE_CREDENTIALS`는 **양 끝만** 있다.

| 단계 | GEMINI_API_KEY | FIREBASE_CREDENTIALS |
| --- | --- | --- |
| `.env.example` 문서화 | ✅ | ✅ |
| `deploy-prod.yml` `env:` + `envs:` | ✅ | ❌ |
| `deploy-prod.yml` `save_env` | ✅ | ❌ |
| `compose.yaml` `environment:` | ✅ | ❌ |
| `application.yaml` 바인딩 | ✅ | ✅ |

가운데 세 단계가 통째로 빠져서, 앱은 `haemi.notification.fcm.credentials`를 빈 문자열로 읽는다.

### 1-2. 왜 조용히 실패하나

`FirebaseConfig.FcmConfiguredCondition`이 불성립 → `FirebaseMessaging` 빈 없음 → `PushSenderConfig`가 `LoggingPushSenderAdapter`를 고른다. 이 폴백은 **의도된 설계**다. 로컬·테스트에서 자격증명 없이 돌리기 위한 것이다.

문제는 운영에서도 같은 경로를 타면서 아무도 모른다는 점이다. 기동 시 INFO 한 줄이 전부고, 기기 토큰 등록도 테스트 발송도 200을 돌려준다.

## 2. 설계 판단

### 2-1. 폴백 자체는 유지한다

자격증명이 없다고 기동을 실패시키면 로컬 개발과 CI가 막힌다. 폴백은 그대로 두고 **배선만** 잇는다.

### 2-2. 운영 프로필에서만 시끄럽게 만든다

다만 운영에서 조용히 로그 폴백으로 도는 건 사고다. `prod` 프로필일 때는 폴백 선택을 `WARN`으로 올려, 로그만 봐도 "지금 푸시가 안 나가고 있다"가 드러나게 한다.

기동 실패까지 시키지 않는 이유는, 알림은 부가 기능이고 이것 때문에 서비스 전체가 안 뜨는 게 더 나쁘기 때문이다.

### 2-3. 서비스 계정 JSON은 `.env`에 넣지 않는다

처음에는 다른 값들처럼 `save_env FIREBASE_CREDENTIALS "$JSON"`으로 `.env`에 쓰려 했다. 2차 검증에서 이게 위험하다는 걸 확인했다.

`save_env`는 `printf '%s=%s\n'`으로 **한 줄**을 쓴다. 그런데 Firebase 서비스 계정 JSON은 GitHub Secret에 여러 줄(pretty-printed)로 저장되는 경우가 흔하다. 그러면 `.env`가 중간에 깨지고, 뒤따르는 `IMAGE_TAG`·`APP_PORT`·`OPENAPI_SERVER_URL`까지 오염돼 **배포 전체가 조용히 망가진다**.

`FirebaseConfig.openCredentials`는 JSON 원문과 파일 경로를 둘 다 받는다. 그래서 JSON은 `secrets/firebase-service-account.json`(0600)으로 떨어뜨리고 컨테이너에 읽기 전용으로 마운트한 뒤, `.env`에는 **경로만** 넣는다. 개행·따옴표·`#` 같은 문자를 신경 쓸 필요가 없어지고, 개인키가 다른 설정과 한 파일에 섞이지도 않는다.

### 2-4. 재발 가드는 설정 파일을 직접 검증한다

이 버그는 Java 코드가 아니라 배포 매니페스트에 있었다. 단위 테스트로는 절대 잡히지 않는다.
`compose.yaml`과 `deploy-prod.yml`을 실제로 파싱해, `application.yaml`이 요구하는 환경변수가 전달되는지 확인하는 테스트를 둔다.

## 3. 변경 파일

| 파일 | 변경 |
| --- | --- |
| `compose.yaml` | `FIREBASE_CREDENTIALS`·`FIREBASE_PROJECT_ID` 전달 |
| `.github/workflows/deploy-prod.yml` | Secret → `envs` → `save_env` 연결 |
| `notification/infrastructure/PushSenderConfig` | 운영 프로필 폴백 시 WARN |
| `DeploymentConfigurationTest` (신규) | 배선 회귀 가드 |

## 4. 검증

- 배선 가드 테스트가 현재 `compose.yaml`에서 **실패**하는지 먼저 확인 (가드가 실제로 동작하는지)
- 자격증명이 없는 테스트 환경에서 기존 폴백 동작이 그대로인지
- `./gradlew test --rerun-tasks` green

## 5. Out-of-scope

- 리포트 PDF 영속성 → #93
- 가입 시 역할 제한(기관 관리자 자가 가입) → 범위 밖, 별도 논의
