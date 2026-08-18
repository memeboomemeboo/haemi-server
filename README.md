# 해미 (Haemi) — 어르신 회상 케어 서버

> **해미**는 어르신의 생애 기억을 가족·기관과 함께 기록하고, 회상 훈련·건강 모니터링·오프라인 케어까지 아우르는 통합 케어 플랫폼의 백엔드 서버입니다.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)

---

## 목차

- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [모듈 구조](#모듈-구조)
- [빠른 시작](#빠른-시작)
- [환경 변수](#환경-변수)
- [API 문서](#api-문서)
- [개발 가이드](#개발-가이드)
- [배포](#배포)
- [문서](#문서)

---

## 주요 기능

| 모듈 | 설명 | 대표 API |
|------|------|----------|
| **M0 — 계정 & 어르신 프로필** | 회원가입/로그인, 2FA, 어르신 프로필·상태 관리, 가족 그룹, 기관 배정, 접근 모드(Mode A/B) | `/api/v1/auth`, `/api/v1/elders` |
| **M1 — 앨범 & 회상** | 앨범·사진 업로드, 기억 기록, 생애 회상 피드 | `/api/v1/albums`, `/api/v1/elders/{id}/memories` |
| **M2 — 커뮤니티 피드** | 그룹 피드, 그룹 목표, 메모리 포스트, 레퍼런스 데이터 | `/api/v1/albums/{id}/posts`, `/api/v1/reference` |
| **M3 — 회상 훈련** | 훈련 세션, 힌트 적립, 난이도 정책 관리 | `/api/v1/training/sessions`, `/api/v1/training/hints` |
| **M4 — 기관 포털** | 기관 대시보드, 포털 API, 인지 현황 모니터링 | `/api/v1/institution-portal`, `/api/v1/cognitive-dashboard` |
| **M5 — 케어 관리** | 케어 활동 기록 및 조회 | `/api/v1/care` |
| **알림** | FCM 푸시(조용시간·일일 한도 정책 포함), 이메일 발송 | `/api/v1/device-tokens` |
| **오프라인** | 오프라인 세션 결과 일괄 수신·처리 | `/api/v1/offline-results` |
| **이벤트 로그 / 사전 다운로드** | 도메인 이벤트 집계, 콘텐츠 번들 스케줄러 | `/api/v1/events` |

전체 엔드포인트는 [Swagger UI](#api-문서)에서 확인하세요.

---

## 기술 스택

| 영역 | 사용 기술 |
|------|-----------|
| 언어 / 프레임워크 | Java 21, Spring Boot 4.0 |
| 인증 | Spring Security, JWT (jjwt 0.12), TOTP 2FA |
| 영속성 | Spring Data JPA, PostgreSQL 16, Flyway |
| 외부 연동 | Firebase Admin SDK (FCM), Gemini API, SMTP |
| 문서 / 운영 | springdoc-openapi (Swagger UI), Spring Boot Actuator |
| 기타 | OpenPDF (리포트 PDF), Docker / Docker Compose |

---

## 모듈 구조

```
src/main/java/com/memeboo2/haemi/
├── auth/           # JWT 인증·인가
├── common/         # 공통 유틸, 예외, 응답 래퍼
├── config/         # Spring 설정 (CORS, OpenAPI 등)
├── eventlog/       # 도메인 이벤트 로깅
├── notification/   # FCM + 이메일
├── offline/        # 오프라인 결과 수신
├── predownload/    # 콘텐츠 사전 다운로드
├── m0/             # 계정 & 어르신 프로필
├── m1/             # 앨범 & 회상
├── m2/             # 커뮤니티 피드
├── m3/             # 회상 훈련
├── m4/             # 기관 포털
└── m5/             # 케어 관리
```

각 모듈은 `presentation` / `application` / `domain` / `infrastructure` 레이어로 분리됩니다.

---

## 빠른 시작

### 사전 요건

- Docker & Docker Compose
- (로컬 개발) JDK 21

### 1. 환경 변수 준비

```bash
cp .env.example .env
```

`.env`에서 최소한 아래 값을 채웁니다. 비어 있으면 Compose가 기동 전에 실패합니다.

```bash
# 32바이트 키
openssl rand -base64 32   # ELDER_HEALTH_ENCRYPTION_KEY
# 서명 키
openssl rand -base64 48   # JWT_SECRET
openssl rand -base64 24   # POSTGRES_PASSWORD
```

> `prod` 프로파일에서는 FCM(`FIREBASE_*`)·SMTP(`MAIL_*`)·`APP_PUBLIC_URL` 값도 필수입니다. 하나라도 비면 앱이 기동하지 않습니다.

### 2. Docker Compose로 실행

```bash
docker compose up -d
```

| 주소 | 용도 |
|------|------|
| `http://localhost:8080` | 애플리케이션 |
| `http://localhost:8080/actuator/health` | 헬스체크 |
| `http://localhost:8080/swagger-ui.html` | API 문서 |

애플리케이션 컨테이너는 PostgreSQL 헬스체크 통과 후 시작하며, 첫 기동 시 Flyway가 스키마를 생성합니다.

### 3. 로컬에서 Gradle로 실행

PostgreSQL만 컨테이너로 띄우고 앱은 호스트에서 실행하는 방식입니다.

```bash
docker compose up -d postgres
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

`dev` 프로파일은 외부 연동(FCM·SMTP) 값 없이도 기동하며, FCM은 발송 대신 로그 폴백으로 동작합니다.

자세한 실행 절차는 [docs/help/runapp.md](docs/help/runapp.md)를 참고하세요.

---

## 환경 변수

### 필수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `POSTGRES_PASSWORD` | — | PostgreSQL 비밀번호 (Compose 필수) |
| `JWT_SECRET` | — | JWT 서명 키 (HS256, 256bit 이상) |
| `ELDER_HEALTH_ENCRYPTION_KEY` | — | 건강 정보 암호화 키 (base64 32바이트) |

### prod 프로파일 필수

| 변수 | 설명 |
|------|------|
| `FIREBASE_CREDENTIALS` | 서비스 계정 JSON 원문 또는 파일 경로 (컨테이너 기준 `/run/secrets/...`) |
| `FIREBASE_PROJECT_ID` | Firebase 프로젝트 ID |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_FROM` | SMTP 설정 |
| `APP_PUBLIC_URL` | 이메일 확인 링크에 사용할 서비스 공개 URL |

### 데이터베이스

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/haemi` | JDBC URL (Compose가 자동 주입) |
| `DB_USERNAME` / `DB_PASSWORD` | `haemi` / — | DB 접속 정보 |
| `POSTGRES_DB` / `POSTGRES_USER` | `haemi` / `haemi` | Compose PostgreSQL 초기화 값 |
| `POSTGRES_PORT` | `5432` | 호스트 바인딩 포트 (`127.0.0.1`에만 노출) |
| `FLYWAY_BASELINE_ON_MIGRATE` | `false` | 기존 스키마에 Flyway를 얹을 때만 `true` |

### 선택

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `INSTITUTION_ADMIN_EMAILS` | (비어 있음) | 기관 관리자 가입 허용 이메일, 쉼표 구분. **비우면 아무도 가입 불가** |
| `GEMINI_API_KEY` | — | Gemini API 키. 없으면 시 초안·음성 전사가 503 반환 |
| `GEMINI_MODEL` | `gemini-3.6-flash` | 사용할 Gemini 모델 |
| `GEMINI_CONNECT_TIMEOUT` / `GEMINI_READ_TIMEOUT` | `5s` / `20s` | 업스트림 타임아웃 |
| `GEMINI_INLINE_AUDIO_MAX_BYTES` | `12MB` | 음성 전사 원본 최대 크기 |
| `GEMINI_MAX_CONCURRENT_AUDIO_REQUESTS` | `2` | 동시 음성 전사 수 (힙 보호) |
| `MULTIPART_MAX_FILE_SIZE` | `20MB` | 사진 1장 최대 크기 |
| `MULTIPART_MAX_REQUEST_SIZE` | `650MB` | 업로드 1회 최대 크기 (최대 30장) |
| `API_DOCS_ENABLED` | `true` | Swagger UI / `/v3/api-docs` 활성화 여부 |
| `OPENAPI_SERVER_URL` | — | Swagger UI에 표시할 서버 URL |
| `APP_PORT` | `8080` | 호스트 바인딩 포트 |
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring 프로파일 (`prod` / `dev`) |
| `HAEMI_REPORT_PDF_OUTPUT_DIR` | 임시 디렉터리 | 리포트 PDF 저장 경로 (Compose는 영속 볼륨 사용) |

---

## API 문서

서버 실행 후 Swagger UI에서 전체 API 명세를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

기본값(`API_DOCS_ENABLED=true`)에서는 문서가 **인증 없이 공개**되어 전체 API 표면이 노출됩니다. 클라이언트 개발 중에는 켜 두고, 정식 오픈 시점에 `API_DOCS_ENABLED=false`로 배포하면 Swagger UI와 `/v3/api-docs`가 모두 비활성화됩니다. 코드 변경 없이 재배포만 하면 됩니다.

---

## 개발 가이드

### 브랜치 전략

- `main` — 운영 릴리즈
- `develop` — 통합 브랜치
- `feat/<이슈번호>-<설명>` — 기능 개발

### DB 마이그레이션

Flyway를 사용하며, `ddl-auto`는 `validate`로 고정되어 있습니다. 스키마 변경은 반드시 마이그레이션 파일로 추가하세요.

```
src/main/resources/db/migration/V<버전>__<설명>.sql
```

### 테스트

```bash
# 전체 테스트 (H2 인메모리 DB 사용)
./gradlew test

# S1 안전 회귀 게이트
./gradlew test --tests com.memeboo2.haemi.regression.S1RegressionSuiteTest
```

두 명령 모두 CI에서 PR마다 실행됩니다.

---

## 배포

- **CI** — `main` / `develop` 대상 PR·push마다 전체 테스트와 S1 회귀 게이트를 실행합니다. (`.github/workflows/ci.yml`)
- **CD** — `main`으로의 PR이 머지되면 이미지를 빌드해 Docker Hub에 푸시하고 운영 서버에 배포합니다. (`.github/workflows/deploy-prod.yml`)

수동으로 이미지를 빌드할 때:

```bash
docker build -t finepinee/haemi:latest .
```

---

## 문서

| 경로 | 내용 |
|------|------|
| [docs/adr/](docs/adr) | 아키텍처 결정 기록 |
| [docs/help/](docs/help) | 실행 가이드, FCM 푸시 테스트 |
| [docs/todo/](docs/todo) | 이슈별 구현 계획 |
| [docs/coordination/](docs/coordination) | 개발자 간 작업 조율 문서 |
| [tools/fcm-token-tester/](tools/fcm-token-tester) | FCM 디바이스 토큰 테스트 도구 |

---

## 라이선스

내부 프로젝트입니다. 무단 배포를 금합니다.
