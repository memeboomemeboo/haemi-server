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

- `prod`: Swagger와 OpenAPI 비활성화, DB/JWT 환경변수 필수
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
