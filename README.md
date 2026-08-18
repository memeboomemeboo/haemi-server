# 해미 (Haemi) — 어르신 회상 케어 서버

> **해미**는 어르신의 생애 기억을 가족·기관과 함께 기록하고, 회상 훈련·건강 모니터링·오프라인 케어까지 아우르는 통합 케어 플랫폼의 백엔드 서버입니다.

---

## 목차

- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [모듈 구조](#모듈-구조)
- [빠른 시작](#빠른-시작)
- [환경 변수](#환경-변수)
- [API 문서](#api-문서)
- [개발 가이드](#개발-가이드)

---

## 주요 기능

| 모듈 | 설명 |
|------|------|
| **M0 — 계정 & 어르신 프로필** | 회원가입/로그인, 어르신 프로필·상태 관리, 가족 그룹, 기관 배정, 접근 모드(Mode A/B) |
| **M1 — 앨범 & 회상** | 앨범·사진 업로드, 기억 기록, 생애 회상 피드 |
| **M2 — 커뮤니티 피드** | 그룹 피드, 목표 관리, 메모리 포스트, 레퍼런스 데이터 |
| **M3 — 회상 훈련** | 훈련 세션, 힌트 적립, 난이도 정책 관리 |
| **M4 — 기관 포털** | 기관 대시보드, 포털 API, 어르신 현황 모니터링 |
| **M5 — 케어 관리** | 케어 활동 기록 및 조회 |
| **알림** | FCM 푸시 알림, 이메일 발송 |
| **오프라인** | 오프라인 세션 결과 일괄 수신·처리 |
| **사전 다운로드** | 콘텐츠 번들 스케줄러 |

---

## 기술 스택

- **Java 21** / **Spring Boot 4.0**
- **Spring Security** + **JWT** (jjwt 0.12)
- **Spring Data JPA** + **PostgreSQL 16**
- **Flyway** — DB 마이그레이션
- **Firebase Admin SDK** — FCM 푸시
- **Gemini API** — AI 기능
- **OpenPDF** — 리포트 PDF 생성
- **springdoc-openapi** — Swagger UI
- **Docker / Docker Compose** — 컨테이너 배포

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
- (로컬 개발) JDK 21, Gradle

### Docker Compose로 실행

```bash
# 필수 환경 변수 설정 (.env 파일 또는 export)
cp .env.example .env   # 예시 파일이 있는 경우
# .env 편집 후

docker compose up -d
```

서버가 기본적으로 `http://localhost:8080` 에서 실행됩니다.

### 로컬에서 Gradle로 실행

```bash
# PostgreSQL이 로컬 5432 포트에 실행 중이어야 합니다
./gradlew bootRun
```

---

## 환경 변수

| 변수 | 필수 | 기본값 | 설명 |
|------|:----:|--------|------|
| `POSTGRES_PASSWORD` | ✅ | — | PostgreSQL 비밀번호 |
| `JWT_SECRET` | ✅ | — | JWT 서명 키 |
| `ELDER_HEALTH_ENCRYPTION_KEY` | ✅ | — | 건강 정보 암호화 키 |
| `INSTITUTION_ADMIN_EMAILS` | | (비어 있으면 가입 불가) | 기관 관리자로 가입 가능한 이메일 목록 (쉼표 구분) |
| `FIREBASE_CREDENTIALS` | | — | FCM 서비스 계정 JSON 파일 경로 |
| `FIREBASE_PROJECT_ID` | | — | Firebase 프로젝트 ID |
| `GEMINI_API_KEY` | | — | Gemini API 키 |
| `GEMINI_MODEL` | | `gemini-3.6-flash` | 사용할 Gemini 모델 |
| `MAIL_HOST` / `MAIL_PORT` | | — | 메일 서버 설정 |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | | — | 메일 인증 정보 |
| `MAIL_FROM` | | — | 발신 이메일 주소 |
| `APP_PUBLIC_URL` | | — | 서비스 공개 URL (이메일 링크 등에 사용) |
| `API_DOCS_ENABLED` | | `true` | Swagger UI 공개 여부 |
| `OPENAPI_SERVER_URL` | | — | Swagger UI에 표시할 서버 URL |
| `APP_PORT` | | `8080` | 호스트 바인딩 포트 |
| `SPRING_PROFILES_ACTIVE` | | `prod` | Spring 프로파일 |

---

## API 문서

서버 실행 후 Swagger UI에서 전체 API 명세를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui.html
```

`API_DOCS_ENABLED=false` 로 설정하면 비인증 접근이 차단됩니다.

---

## 개발 가이드

### 브랜치 전략

- `main` — 운영 릴리즈
- `develop` — 통합 브랜치
- `feat/<issue-번호>-<설명>` — 기능 개발

### DB 마이그레이션

Flyway를 사용합니다. 새 마이그레이션 파일은 아래 경로에 추가합니다.

```
src/main/resources/db/migration/V<버전>__<설명>.sql
```

### 테스트 실행

```bash
./gradlew test
```

테스트는 H2 인메모리 DB를 사용합니다.

### 도커 이미지 빌드

```bash
docker build -t finepinee/haemi:latest .
```

---

## 라이선스

내부 프로젝트입니다. 무단 배포를 금합니다.
