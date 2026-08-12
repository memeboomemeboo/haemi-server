# 해미 애플리케이션 실행 가이드

## 준비 사항

전체 컨테이너 실행에는 Docker Desktop과 Docker Compose가 필요합니다. 호스트에서
애플리케이션을 직접 실행하려면 Java 21도 필요합니다.

```shell
docker --version
docker compose version
java --version
```

## 환경변수 준비

환경변수 예시 파일을 복사합니다.

```shell
cp .env.example .env
```

`.env`에서 다음 값을 반드시 설정합니다.

- `POSTGRES_PASSWORD`: PostgreSQL 비밀번호
- `DB_PASSWORD`: 호스트에서 앱을 실행할 때 사용할 비밀번호. `POSTGRES_PASSWORD`와 동일하게 설정
- `JWT_SECRET`: JWT 서명용 secret
- `SPRING_PROFILES_ACTIVE`: `prod` 또는 `dev`

`prod`에서는 아래 외부 연동 값도 필수입니다. 값 하나라도 비어 있으면 앱이 기동하지
않습니다.

- FCM: `FIREBASE_CREDENTIALS`, `FIREBASE_PROJECT_ID`
- SMTP: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
- 공개 확인 링크 주소: `APP_PUBLIC_URL`

안전한 값을 생성할 때 다음 명령을 사용할 수 있습니다.

```shell
openssl rand -base64 24
openssl rand -base64 48
```

`POSTGRES_PASSWORD`와 `JWT_SECRET`이 비어 있으면 Compose는 실행 전에 실패합니다.

## 전체 앱 실행

애플리케이션과 PostgreSQL을 함께 실행하는 권장 방식입니다.

```shell
docker compose up --build -d
docker compose ps
```

애플리케이션 컨테이너는 PostgreSQL 헬스체크가 통과한 뒤 시작됩니다. 첫 실행에서는
Flyway가 데이터베이스 스키마를 생성합니다.

기본 접근 주소:

- 애플리케이션: `http://localhost:8080`
- 헬스체크: `http://localhost:8080/actuator/health`
- Swagger UI (`dev`만): `http://localhost:8080/swagger-ui.html`
- OpenAPI (`dev`만): `http://localhost:8080/v3/api-docs`

헬스체크가 다음과 같이 응답하면 앱과 데이터베이스 연결이 정상입니다.

```shell
curl http://localhost:8080/actuator/health
```

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

## 프로필 전환

Compose는 `.env`의 `SPRING_PROFILES_ACTIVE`를 사용하며 기본값은 `prod`입니다.

```dotenv
SPRING_PROFILES_ACTIVE=prod
```

- `prod`: SMTP·FCM·DB·JWT 환경변수 필수. 이메일 확인, 초대 수락, 기기 알림을 실제로 전달합니다.
- `dev`: Swagger와 OpenAPI 활성화

개발 프로필로 변경한 뒤 앱 컨테이너를 재생성합니다.

```dotenv
SPRING_PROFILES_ACTIVE=dev
```

```shell
docker compose up -d --build app
```

Gradle 또는 실행 JAR에서는 다음 플래그를 사용할 수 있습니다.

```shell
./gradlew bootRun --args='--spring.profiles.active=dev'
java -jar app.jar --spring.profiles.active=prod
```

## 로그와 상태 확인

```shell
docker compose ps
docker compose logs -f app
docker compose logs -f postgres
```

앱이 시작되지 않으면 다음 항목을 순서대로 확인합니다.

1. `.env`의 필수 값이 비어 있지 않은지 확인합니다.
2. PostgreSQL 컨테이너가 `healthy`인지 확인합니다.
3. 앱 로그에서 Flyway 또는 Hibernate 검증 오류를 확인합니다.
4. `8080` 또는 `5432` 포트가 이미 사용 중인지 확인합니다.

포트 충돌 시 `.env`의 `APP_PORT` 또는 `POSTGRES_PORT`를 변경합니다.

## 재시작과 업데이트

앱만 재시작:

```shell
docker compose restart app
```

코드 변경 후 이미지 재빌드:

```shell
docker compose up --build -d app
```

전체 서비스 중지:

```shell
docker compose down
```

`docker compose down`은 PostgreSQL과 업로드 볼륨을 유지합니다. 다음 명령은 모든
애플리케이션 및 PostgreSQL 데이터를 삭제하므로 초기화가 필요한 경우에만 사용합니다.

```shell
docker compose down -v
```

## 호스트에서 앱 실행

PostgreSQL만 Docker로 실행하고 애플리케이션은 Gradle로 실행할 수 있습니다.

```shell
docker compose up -d postgres
set -a
source .env
set +a
./gradlew bootRun --args='--spring.profiles.active=dev'
```

기본 PostgreSQL 연결 정보:

- 데이터베이스: `haemi`
- 사용자: `haemi`
- 포트: `5432`
- JDBC URL: `jdbc:postgresql://localhost:5432/haemi`

`POSTGRES_*` 값을 변경했다면 대응하는 `DB_*` 값도 함께 변경합니다.

## 테스트와 이미지 빌드

전체 테스트:

```shell
./gradlew test
```

실행 JAR 생성:

```shell
./gradlew bootJar
```

Docker 이미지 단독 빌드:

```shell
docker build -t haemi-server .
```

## 데이터베이스 스키마

- 기본 및 `prod` 프로필에서는 Flyway가 `db/migration`의 변경을 적용합니다.
- Hibernate는 스키마를 변경하지 않고 `validate`만 수행합니다.
- `test` 프로필에서는 Flyway를 비활성화하고 인메모리 H2를 사용합니다.
- 새 스키마 변경은 기존 migration을 수정하지 않고 다음 버전 파일로 추가합니다.

기존 Hibernate `update`로 생성된 데이터베이스를 유지해야 한다면 먼저 백업합니다.
첫 실행에만 `.env`의 `FLYWAY_BASELINE_ON_MIGRATE=true`를 사용하고 정상 기동 후 즉시
`false`로 되돌립니다. 신규 데이터베이스에서는 이 값을 변경하지 않습니다.

## 운영 전 확인

```shell
docker compose config --quiet
docker compose up --build -d
docker compose ps
curl --fail http://localhost:8080/actuator/health
```

운영에서는 `.env`를 저장소에 커밋하지 않고 배포 환경의 secret 관리 기능을 사용합니다.

## 이메일과 기기 연동 확인

### 이메일 확인

가입과 가족 초대 수락은 전화번호 OTP가 아니라 이메일 확인 링크를 사용합니다. 가입 API는
즉시 로그인 토큰을 발급하지 않으며, 받은 메일의 링크를 열어야 계정이 활성화됩니다.

SMTP 계정은 발신자 주소와 일치해야 합니다. 배포 전에는 테스트 이메일로 가입한 뒤 다음을
확인합니다.

1. 메일 본문의 링크가 `APP_PUBLIC_URL/api/v1/auth/email-verifications/confirm?token=...` 형식인지 확인합니다.
2. 링크를 한 번 열면 계정이 활성화되는지 확인합니다.
3. 같은 링크를 다시 열거나 24시간 뒤 열면 거부되는지 확인합니다.
4. 가족 초대는 72시간 내, 초대받은 이메일과 동일하고 확인 완료된 계정만 수락되는지 확인합니다.

### 클라이언트 계약 변경

- `POST /api/v1/auth/signup`은 `201` 대신 `202 Accepted`를 반환하며 즉시 로그인 토큰을 주지 않습니다.
- 가족 초대 요청 필드는 `phoneNumber`에서 `email`로 변경됐습니다.
- `POST /api/v1/training/sessions/{sessionId}/answers`는 `submittedAnswer`, `responseSeconds` 대신
  `voiceDetected`, `vadDurationMs`만 받습니다. `AttemptResult`도 답변 원문을 반환하지 않습니다.
- 실시간 손주 찬스·힌트·패스 엔드포인트는 폐기됐으며, `/{sessionId}/hints/served`를 사용합니다.

### Firebase 및 iOS

서버는 Firebase Admin SDK를 통해 FCM data payload를 발송합니다. iOS의 APNs 키·인증서는
Firebase Console에 등록하며 서버 환경변수로 APNs 자격증명을 넣지 않습니다. 배포 전 Firebase
프로젝트에 iOS 앱과 APNs 키를 연결하고, 실제 기기 토큰으로 회상 알림과 목소리 알람을 각각
수신·확인합니다.

사별 확정 뒤에는 예약을 취소하고 `LOCK_AND_OPEN_MEMORIAL` 기기 명령을 DB 아웃박스에 기록합니다.
단말 또는 MDM이 응답하지 않으면 최대 10회, 최대 60분 간격으로 재시도합니다. 단말은 잠금 확인 전
로컬 선다운로드 콘텐츠를 재생하면 안 됩니다.

### 기관 포털 권한

기관 담당자는 기관 관리자 계정의 TOTP 등록과 어르신 배정을 모두 충족해야 합니다. 기관 화면에는
배정된 어르신의 회상 집계만 제공하고, 가족 추억 본문과 memorial 활동 데이터는 제공하지 않습니다.
권한 거부와 내보내기 요청은 운영 감사 로그에서 점검합니다.

### 빈 운영 DB 초기화

이번 배포는 운영 시작 전 빈 PostgreSQL DB를 전제로 합니다. 기존 운영 DB를 재사용하지 말고,
배포 전에 데이터베이스와 Docker volume을 백업한 뒤 필요할 때만 `docker compose down -v`로
초기화합니다. 운영을 시작한 뒤에는 이메일 확인·초대·기기 명령 데이터를 초기화하지 않습니다.
