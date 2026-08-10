# 개발 플랜 — #93 리포트 PDF 영속성

> fix · develop 전체 검토 후속 · #92 위에 스택.

## 1. 이슈 분석

### 1-1. 파일은 사라지고 경로만 남는다

`OpenPdfReportAdapter`의 출력 경로는 이렇게 정해진다.

```java
@Value("${haemi.report.pdf.output-dir:reports}") Path outputDir
```

그런데 `haemi.report.pdf.output-dir`를 설정한 곳이 `application.yaml`에도 `application-prod.yaml`에도 `compose.yaml`에도 **없다**. 기본값 `reports`가 그대로 쓰이고, 이건 컨테이너 작업 디렉터리 기준 **상대 경로**다.

`compose.yaml`의 볼륨은 사진용 `haemi-upload-data:/data/haemi/photos` 하나뿐이다. `reports/`는 컨테이너 레이어에 쌓이므로 `docker compose up -d`로 컨테이너가 새로 뜰 때마다 사라진다.

반면 `pdfKey`(절대 경로 문자열)는 `cognitive_reports` 행에 그대로 남는다. 파일은 없는데 DB는 있다고 말하는 상태가 된다.

### 1-2. 그래서 다운로드가 500이 된다

```java
Resource resource = new FileSystemResource(report.getPdfKey());
return ResponseEntity.ok().body(resource);
```

`FileSystemResource`는 생성 시점에 존재 여부를 확인하지 않는다. 응답 본문을 쓰는 단계에서 `FileNotFoundException`이 터지고, 이미 200 헤더가 나간 뒤라 클라이언트는 깨진 응답을 받는다. 서버 로그에는 `GlobalExceptionHandler`의 "예상치 못한 오류"만 남아 원인을 짐작하기 어렵다.

## 2. 설계 판단

### 2-1. 사진과 같은 방식으로 볼륨에 둔다

이미 `HAEMI_STORAGE_UPLOAD_PATH: /data/haemi/photos` + `haemi-upload-data` 볼륨이라는 선례가 있다. 리포트도 같은 모양으로 맞춘다 — 별도 볼륨 `haemi-report-data:/data/haemi/reports`.

사진 볼륨에 얹지 않는 이유는 보존 주기와 성격이 달라서다. 사진은 사용자 원본이고 리포트는 재생성 가능한 파생물이다. 나중에 정리 정책을 따로 두려면 분리돼 있어야 한다.

### 2-2. 기본값은 상대 경로를 벗어난다

`reports` 같은 상대 경로 기본값은 "설정 안 하면 조용히 잘못된 곳에 쓴다"는 함정이다. 다만 로컬 개발과 테스트가 막히면 안 되므로, 사진 설정이 쓰는 `${java.io.tmpdir}` 관례를 따라 `application.yaml`에 명시적 기본값을 둔다.

### 2-3. 파일이 없으면 500이 아니라 404

볼륨을 붙여도 **이미 만들어진 과거 행은 파일이 없다.** 이 경우 응답이 200으로 시작했다가 깨지는 대신, 원인을 알 수 있는 404를 줘야 한다.

리포트 메타데이터는 DB에 남아 있으므로 재생성이 가능하다. 안내 문구로 그 경로를 알려준다.

## 3. 변경 파일

| 파일 | 변경 |
| --- | --- |
| `application.yaml` | `haemi.report.pdf.output-dir` 명시 기본값 |
| `compose.yaml` | `HAEMI_REPORT_PDF_OUTPUT_DIR` + 리포트 볼륨 |
| `m4/presentation/DashboardController` | 파일 부재 시 404 |
| `m4/domain/model/dashboard/ReportFileNotFoundException` | 신규 |
| `m4/presentation/exception/M4ExceptionHandler` | 404 매핑 |
| `DeploymentConfigurationTest` | 리포트 경로 배선 가드 |

## 4. 검증

- 파일이 없는 리포트를 내려받으면 404와 안내 문구가 나오는지 통합 테스트
- 정상 생성된 리포트는 그대로 내려받히는지
- 배선 가드가 `compose.yaml`의 리포트 볼륨·환경변수를 확인
- `./gradlew test --rerun-tasks` green

## 5. Out-of-scope

- 오래된 PDF 정리(retention) 정책 — 볼륨이 생긴 뒤에 별도로 논의
- 오브젝트 스토리지(S3 등) 이전 — 지금 규모에서는 과하다
