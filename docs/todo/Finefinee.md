# 기능 구현 현황 및 남은 작업

최종 갱신일: 2026-07-06

기준 문서: `해미_기능명세서_v2.0 (2).docx`

## 적용 결정

- 외부 AI 연동은 추후 작업으로 분리한다.
- F3-01 문제 생성은 현재 앨범 데이터를 사용하는 내부 문제 생성기로 제공한다.
- TTS는 서버에서 음성 파일을 생성하지 않고, 클라이언트가 응답 텍스트를 읽는 방식으로 처리한다.
- FCM/APNs와 이메일처럼 외부 서비스 인증정보가 필요한 기능은 추후 연동한다.

## 최신 구현 상태

| 기능 | 상태 | 현재 구현 | 남은 작업 |
| --- | --- | --- | --- |
| F3-01 일일 인지 훈련 세션 | 최고 | 사진 5장·어르신 프로필 검증, 앨범 기반 3~5문제 생성, 유형 연속 중복 방지, 당일 이어서 풀기, 생성 실패 fallback, 완료 칭찬·정답률 텍스트 제공 | 외부 AI 연결, 실제 푸시 알림 연결 |
| F3-02 난이도 적응형 알고리즘 | 최고 | 최근 3세션 이동평균, 극단적 점수 완충, 레벨별 기준표 관리, 연속 정답·오답·timeout 조정, 반복 오답 유형 우선 배치 | 전문가 분기 검토 기한 알림 자동화 |
| F3-03 손주 찬스 | 최고 | 세션당 2회 제한, 가족 전체 알림, 가족 구성원 응답 검증, 30분 만료 상태 갱신, 안내 메시지·문제 패스 API, 미사용 완료 뱃지 상태 제공 | 실제 푸시 알림 연결 |
| F4-01 주간·월간 인지 리포트 | 중간 | 주·월간 스케줄러, 7일 조건, 정답률 추이, 회상 참여, 최다 반응 사진 유형, 이전 기간 비교, PDF 차트·다운로드, 가족 그룹 알림, 열람 시각 기록 | 실제 이메일 발송, 기관-관리자 소속 관계와 기관 수신자 지정 |

## 주요 구현 근거

### F3-01 일일 인지 훈련

- 세션 시작과 사진·프로필 선행조건: [`TrainingApplicationService.java`](../../src/main/java/com/memeboo2/haemi/m3/application/service/TrainingApplicationService.java)
- 앨범 기반 문제 생성: [`AlbumCognitiveQuestionGeneratorAdapter.java`](../../src/main/java/com/memeboo2/haemi/m3/infrastructure/ai/AlbumCognitiveQuestionGeneratorAdapter.java)
- 클라이언트 읽기용 텍스트·SSML 응답: [`SsmlTrainingSpeechAdapter.java`](../../src/main/java/com/memeboo2/haemi/m3/infrastructure/tts/SsmlTrainingSpeechAdapter.java)
- 오전 09:00 알림 스케줄러: [`DailyTrainingScheduler.java`](../../src/main/java/com/memeboo2/haemi/m3/infrastructure/scheduler/DailyTrainingScheduler.java)

### F3-02 난이도 적응

- 이동평균·오답 패턴·레벨 조정: [`DifficultyProfile.java`](../../src/main/java/com/memeboo2/haemi/m3/domain/model/training/DifficultyProfile.java)
- 전문가 기준표와 다음 분기 검토일: [`DifficultyPolicy.java`](../../src/main/java/com/memeboo2/haemi/m3/domain/model/training/DifficultyPolicy.java)
- 관리자 기준표 API: [`DifficultyPolicyAdminController.java`](../../src/main/java/com/memeboo2/haemi/m3/presentation/DifficultyPolicyAdminController.java)

### F3-03 손주 찬스

- 30분 만료·문제 패스·미사용 완료 뱃지: [`CognitiveTrainingSession.java`](../../src/main/java/com/memeboo2/haemi/m3/domain/model/training/CognitiveTrainingSession.java)
- 조회 시 만료 갱신과 가족 응답자 검증: [`TrainingApplicationService.java`](../../src/main/java/com/memeboo2/haemi/m3/application/service/TrainingApplicationService.java)
- 문제 패스 API: [`TrainingController.java`](../../src/main/java/com/memeboo2/haemi/m3/presentation/TrainingController.java)
- 가족 그룹 알림 처리: [`TrainingEventListener.java`](../../src/main/java/com/memeboo2/haemi/m3/infrastructure/event/TrainingEventListener.java)

클라이언트는 `GET /api/v1/training/sessions/today` 응답의 다음 필드를 사용한다.

- `lastChanceStatus=EXPIRED`
- `grandchildChanceGuideMessage`
- `questionPassAvailable=true`

문제 패스는 `POST /api/v1/training/sessions/{sessionId}/pass`를 호출한다.

### F4-01 인지 리포트

- 기간 집계·이전 기간 비교·가족 알림·열람 기록: [`DashboardApplicationService.java`](../../src/main/java/com/memeboo2/haemi/m4/application/service/DashboardApplicationService.java)
- PDF 차트 생성: [`OpenPdfReportAdapter.java`](../../src/main/java/com/memeboo2/haemi/m4/infrastructure/pdf/OpenPdfReportAdapter.java)
- 생성·열람·다운로드 API: [`DashboardController.java`](../../src/main/java/com/memeboo2/haemi/m4/presentation/DashboardController.java)
- 주간·월간 자동 생성: [`CognitiveReportScheduler.java`](../../src/main/java/com/memeboo2/haemi/m4/infrastructure/scheduler/CognitiveReportScheduler.java)
- 스키마 변경: [`V4__complete_grandchild_chance_and_cognitive_reports.sql`](../../src/main/resources/db/migration/V4__complete_grandchild_chance_and_cognitive_reports.sql)

리포트 API:

- `POST /api/v1/cognitive-dashboard/reports`
- `POST /api/v1/cognitive-dashboard/reports/{reportId}/viewed`
- `GET /api/v1/cognitive-dashboard/reports/{reportId}/pdf`

`EMAIL` 또는 `IN_APP_AND_EMAIL`을 선택해도 실제 이메일 연동 전까지는 가족 앱 알림으로 대체한다.

## 아직 구현하지 않은 부분

### 외부 AI

- 외부 AI 모델 호출
- AI 장애 감지와 운영 관리자 알림 채널
- 외부 AI 응답 품질·비용·timeout 정책

현재는 앨범 사진과 메타데이터를 사용해 서버 내부에서 문제를 생성하며, 실패하면 최근 완료 세션 문제로 대체한다.

### 실제 푸시 알림

현재 [`LogNotificationAdapter.java`](../../src/main/java/com/memeboo2/haemi/m1/infrastructure/notification/LogNotificationAdapter.java)는 알림을 로그로 기록한다. 실제 FCM/APNs 연동에는 다음 작업이 필요하다.

- Firebase 프로젝트와 FCM API 설정
- 서비스 계정 또는 Google Application Default Credentials 설정
- 앱에서 발급한 FCM registration token 저장 API
- 회원·기기·플랫폼별 토큰 저장
- 로그아웃 시 토큰 해제 및 갱신 토큰 재등록
- 만료·실패 토큰 비활성화
- iOS APNs 인증 키와 Android/iOS Firebase 설정 파일 적용

회원 JWT와 FCM registration token은 서로 다른 값이다. 앱이 회원 인증 후 기기 토큰을 별도로 서버에 등록해야 한다.

### 이메일과 기관 수신자

- 이메일 발송 provider와 발송 실패 fallback 결과 기록
- 기관과 `INSTITUTION_ADMIN` 회원의 소속 관계 모델
- 어르신별 담당 기관 관리자 지정
- 주·월간 스케줄러의 기관 수신자 조회

기관 ID만으로는 실제 관리자 회원을 특정할 수 없으므로, 현재 코드에서 기관 ID를 회원 ID처럼 사용하지 않는다.

## 검증 결과

- `./gradlew test --rerun-tasks`
- 전체 113개 테스트 성공, 실패 0
- Flyway V1~V4 마이그레이션 적용 및 재실행 검증
- 리포트 생성 API에서 정답률 추이 7건, 회상 참여 14건, 최다 반응 사진 유형 반환 확인
- 리포트 열람 기록 API 성공 확인
- PDF 다운로드 HTTP 200 및 PDF 렌더링 확인
