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
| F4-02 인지 상태 변화 조기 알림 | 최고 | 주 보호자·기관 담당자 수신자 설정, 7일 미참여, 정답률 20%p 하락·반응 시간 50% 증가의 3일 지속 조건, 주 1회 제한, 안내·의료 면책 문구 제공 | 실제 푸시·이메일 발송 연결 |
| F4-03 기관 관리자 포털 | 최고 | 기관별 기간·어르신 필터, 참여율·평균 정답률·주간 변화·기관 평균 비교, 데이터 없음 처리, 익명화 CSV/PDF export, 기관 관리자 역할 제한 | 기관별 관리자 소속 범위를 세분화할 경우 기관-관리자 관계 모델 추가 |
| F5-01 손주 목소리 알람 | 중간 | 반복 예약 스캔, 녹음 음성·클라이언트 TTS fallback 구분, 어르신 확인, 실제 가족 구성원 확인 알림, 10분 무응답 1회 알림 | FCM/APNs 기반 실제 기기 알람·음성 재생 |
| F5-02 하루 10분 산책 유도 | 중간 | 오전·오후 예약 스캔, 기본 10분, 날씨별 실내 활동 fallback, 수동 시작·완료·주간 달성률, 실제 가족 구성원 완료 알림 | 실제 날씨 API, 위치·활동 센서 추적, FCM/APNs 발송 |

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

### F4-02 인지 상태 변화 조기 알림

- 수신자 설정·3일 지속 조건·주 1회 제한: [`DashboardApplicationService.java`](../../src/main/java/com/memeboo2/haemi/m4/application/service/DashboardApplicationService.java)
- 주 보호자·기관 담당자 설정 모델: [`AlertRecipientSetting.java`](../../src/main/java/com/memeboo2/haemi/m4/domain/model/dashboard/AlertRecipientSetting.java)
- 수신자 설정·알림 탐지 API: [`DashboardController.java`](../../src/main/java/com/memeboo2/haemi/m4/presentation/DashboardController.java)

수신자 설정 없이 탐지를 실행하면 알림을 잘못된 대상으로 보내지 않고 `400`을 반환한다. 정답률·반응 시간 변화는 최근 3일 모두 훈련에 참여했고 기준을 초과한 경우에만 알린다.

### F4-03 기관 관리자 포털

- 기관 지표·주간 변화·참여율 집계: [`DashboardApplicationService.java`](../../src/main/java/com/memeboo2/haemi/m4/application/service/DashboardApplicationService.java)
- 익명화 CSV/PDF 생성: [`InstitutionDashboardExportAdapter.java`](../../src/main/java/com/memeboo2/haemi/m4/infrastructure/export/InstitutionDashboardExportAdapter.java)
- 기관 관리자 역할 제한: [`SecurityConfig.java`](../../src/main/java/com/memeboo2/haemi/auth/infrastructure/security/SecurityConfig.java)

기관 데이터가 없으면 빈 대시보드 대신 `404`를 반환한다. export 파일에는 원본 어르신 ID를 포함하지 않고 익명 식별자만 포함한다.

### F5-01 손주 목소리 알람

- 예약 발생·10분 무응답 상태: [`VoiceAlarm.java`](../../src/main/java/com/memeboo2/haemi/m5/domain/model/care/VoiceAlarm.java)
- 예약 스캔·어르신/가족 알림 처리: [`CareApplicationService.java`](../../src/main/java/com/memeboo2/haemi/m5/application/service/CareApplicationService.java)
- 분 단위 실행 스케줄러: [`CareReminderScheduler.java`](../../src/main/java/com/memeboo2/haemi/m5/infrastructure/scheduler/CareReminderScheduler.java)

가족 알림은 `groupId`를 회원 ID로 사용하지 않고 앨범의 실제 구성원 ID를 조회한다. 알람 확인은 해당 알람의 어르신 ID가 일치하고 알람이 실제 발생한 뒤에만 가능하다.

### F5-02 하루 10분 산책 유도

- 오전·오후 예약과 중복 방지: [`WalkRoutine.java`](../../src/main/java/com/memeboo2/haemi/m5/domain/model/care/WalkRoutine.java)
- 날씨 fallback·수동 시작/완료·가족 알림: [`CareApplicationService.java`](../../src/main/java/com/memeboo2/haemi/m5/application/service/CareApplicationService.java)
- 악천후 완료 차단: [`WalkRecord.java`](../../src/main/java/com/memeboo2/haemi/m5/domain/model/care/WalkRecord.java)

위치·활동 센서가 없는 사용자는 기존 수동 시작·완료 API를 사용한다. 외부 날씨 API가 연결되기 전에는 `StubWeatherAdapter`가 맑음으로 응답한다.

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

F5-01/F5-02의 서버 스케줄러와 10분 무응답 판정은 구현되어 있다. 현재 `NotificationPort` 구현이 로그 어댑터이므로 실제 스마트폰 알람 표시와 녹음 음성 재생은 위 기기 토큰 연동 이후 가능하다.

### 날씨·위치·활동 추적

- 어르신 위치 기반 날씨 provider와 장애 시 기본값 정책
- 모바일 위치 권한과 산책 경로 또는 시작 위치 연동
- HealthKit·Health Connect 등 활동 센서 걸음 수 연동
- 센서 미지원·권한 거부 사용자의 수동 완료 유지

현재는 외부 데이터 없이 예약 알림, 악천후 fallback 분기, 수동 산책 완료와 주간 달성률을 검증할 수 있다.

### 이메일과 기관 수신자

- 이메일 발송 provider와 발송 실패 fallback 결과 기록
- 기관과 `INSTITUTION_ADMIN` 회원의 소속 관계 모델
- 어르신별 담당 기관 관리자 지정
- 주·월간 스케줄러의 기관 수신자 조회

기관 ID만으로는 실제 관리자 회원을 특정할 수 없으므로, 현재 코드에서 기관 ID를 회원 ID처럼 사용하지 않는다.

## 검증 결과

- `./gradlew test`
- 전체 128개 테스트 성공, 실패 0
- Flyway V1~V6 마이그레이션 적용 및 재실행 검증
- 리포트 생성 API에서 정답률 추이 7건, 회상 참여 14건, 최다 반응 사진 유형 반환 확인
- 리포트 열람 기록 API 성공 확인
- PDF 다운로드 HTTP 200 및 PDF 렌더링 확인
- 보안 필터 체인에서 가족의 기관 포털 접근 `403`, 기관 관리자의 컨트롤러 진입 확인
- 수신자 설정 저장·조회 API와 산책 루틴 기본 10분 응답 확인
- 조기 알림 3일 지속·일시 변동 제외, 10분 무응답 1회, 악천후 완료 차단 회귀 테스트 통과
