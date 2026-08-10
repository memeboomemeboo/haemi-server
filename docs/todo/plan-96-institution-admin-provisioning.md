# 개발 플랜 — #96 기관 관리자 계정 발급·2FA 잠금 해소

> fix · develop 전체 검토 후속 · #93 위에 스택.

## 1. 이슈 분석

### 1-0. 먼저, 처음 판단이 틀렸다

검토 초기에는 "누구나 `role: INSTITUTION_ADMIN`으로 가입해 `/api/v1/admin/**`에 접근할 수 있다"고 봤다. `totpEnabled`가 옵트인이라는 것만 보고 로그인 분기를 놓친 결과였다.

실제로는 `Member.requiresTotp()`가 INSTITUTION_ADMIN이면 무조건 true라, 2FA를 켜지 않은 관리자는 **토큰을 받지 못한다.** 권한 상승은 성립하지 않는다.

### 1-1. 진짜 문제 — 관리자 계정을 만들 수 없다

닭과 달걀이다.

| 하려는 것 | 필요한 것 | 결과 |
| --- | --- | --- |
| 로그인 | 2FA 활성화 | `TotpRequiredException` |
| 2FA 활성화 (`/totp/setup`) | 인증 토큰 | permitAll 아님 → 401 |

부트스트랩 경로도 없다. 마이그레이션에 관리자 행을 심는 곳이 없고, 관리자 발급 API도 없다.

결과적으로 `INSTITUTION_ADMIN` 역할 전체가 죽어 있다. `/api/v1/admin/training/difficulty-policies`는 도달 가능한 사용자가 없다.

### 1-2. 곁가지 — 공개 가입이 역할을 그대로 받는다

권한 상승으로 이어지지는 않지만, 아무나 기관 이메일로 잠긴 계정을 선점해 정당한 가입을 막을 수 있다(이메일 선점). 공개 엔드포인트가 권한 역할을 검증 없이 받는 것 자체가 좋지 않다.

## 2. 설계 판단

### 2-1. 잠금은 "자격증명 기반 등록"으로 푼다

토큰에 등록 전용 스코프를 넣는 방법도 있지만, `TokenPort`·JWT 필터·`SecurityConfig`를 모두 건드려야 한다. 인증 코어를 넓게 고치는 건 이 이슈가 감당할 위험이 아니다.

대신 자격증명으로 직접 여는 등록 경로를 둔다. `/login`이 이미 공개 엔드포인트로 이메일·비밀번호를 받으므로 새로운 노출 등급이 생기지 않는다.

```
POST /api/v1/auth/totp/enrollment         {email, password}              → {secret, qrUri}
POST /api/v1/auth/totp/enrollment/verify  {email, password, secret, code} → 활성화
```

기존 인증된 흐름(`/totp/setup` → `/totp/verify`)이 클라이언트가 secret을 들고 있다가 되돌려주는 모양이라, 같은 형태를 따른다.

**적용 대상을 좁힌다.** `requiresTotp() && !totpEnabled`인 계정에만 허용한다. 즉 잠긴 관리자 전용이다. 이미 2FA를 켠 계정이나 일반 사용자는 이 경로로 들어올 수 없어, 2FA 재설정 우회로가 되지 않는다.

활성화 후 토큰은 발급하지 않는다. 정상 `/login`으로 코드와 함께 들어오면 된다. 등록 경로가 토큰까지 뱉으면 사실상 2FA를 우회하는 두 번째 로그인이 된다.

### 2-2. 가입은 막되, 운영이 통제하는 발급 경로를 남긴다

INSTITUTION_ADMIN을 공개 가입에서 그냥 거부하면 관리자를 영원히 만들 수 없다 — 2-1로 잠금을 풀어도 소용이 없다.

그래서 **허용 목록**을 둔다. `haemi.security.institution-admin-emails`에 등록된 이메일만 INSTITUTION_ADMIN으로 가입할 수 있다. 기본값은 비어 있어 아무도 가입할 수 없다.

이 방식을 고른 이유:
- 관리자 발급 API를 새로 만들지 않아도 된다 (그건 그 자체로 보호해야 할 표면이 늘어난다)
- 운영이 환경변수로 통제한다. 배포 권한이 있는 사람만 관리자를 늘릴 수 있다
- 기존 배포 설정(`.env` + compose)과 같은 방식이라 새 개념이 없다

FAMILY·ELDER는 지금처럼 자유롭게 가입한다. ELDER는 계정만으로 아무것도 못 하고, 그룹 소유자가 `linkMember`로 연결해야 피드를 본다.

## 3. 변경 파일

| 파일 | 변경 |
| --- | --- |
| `auth/application/service/AuthApplicationService` | 가입 역할 제한, 자격증명 기반 2FA 등록 |
| `auth/application/command/*` | 등록 커맨드 |
| `auth/domain/model/InstitutionAdminSignUpNotAllowedException` | 신규 |
| `auth/presentation/AuthController` | 등록 엔드포인트 2종 |
| `auth/presentation/dto/request/*` | 요청 DTO |
| `auth/infrastructure/security/SecurityConfig` | 등록 경로 permitAll |
| `auth/presentation/exception/*` | 예외 매핑 |
| `application.yaml`, `.env.example`, `compose.yaml` | 허용 목록 설정 |

## 4. 검증

- 허용 목록에 없는 이메일로 INSTITUTION_ADMIN 가입 → 거부
- 허용 목록에 있는 이메일 → 가입 성공, 다만 2FA 전에는 로그인 불가
- 등록 경로로 2FA를 켠 뒤 정상 로그인 성공 (**잠금이 실제로 풀리는지 끝까지 확인**)
- 이미 2FA를 켠 계정·일반 사용자는 등록 경로 거부 (우회로가 아님을 확인)
- 잘못된 비밀번호로 등록 시도 → 거부
- `./gradlew test --rerun-tasks` green

## 5. Out-of-scope

- 기관 관리자 초대·관리 API — 지금 필요 수요가 없고 표면만 늘린다
- 로그인·등록 경로 브루트포스 방어(레이트 리밋) — 기존 `/login`도 없는 상태라 별도 이슈가 맞다
