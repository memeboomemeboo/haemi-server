생성 실패 fallback	
중간	
[TrainingApplicationService.java (line 130)]	
캐시 대체는 구현됐지만 관리자 알림은 로그만 존재

09:00 자동 알림	
중간	
[DailyTrainingScheduler.java (line 23)] 
실제 FCM/APNs 대신 [LogNotificationAdapter.java (line 14)] 사용

문제·완료 TTS	
중간
[SsmlTrainingSpeechAdapter.java (line 8)]
한국어 SSML 제공까지 구현, 실제 음성 재생은 클라이언트 통합 필요

---

네. FCM 연동에는 크게 서버 인증정보와 기기 토큰이 필요합니다.
Firebase 프로젝트
Google/Firebase 계정으로 프로젝트 생성
FCM API 활성화

서버 인증정보
Firebase 서비스 계정 또는 Google Application Default Credentials 사용
서비스 계정 JSON은 Git에 커밋하지 않고 Secret 또는 GOOGLE_APPLICATION_CREDENTIALS로 주입합니다. Firebase Admin SDK 설정

기기 등록 토큰
앱이 FCM SDK에서 발급받는 토큰입니다.
회원 API의 JWT access token과는 별개입니다.
앱이 서버에 다음 형태로 등록해야 합니다.

POST /api/v1/devices/push-tokens
Authorization: Bearer {회원 JWT}

{
"token": "{FCM registration token}",
"platform": "ANDROID",
"deviceId": "device-uuid"
}
서버는 회원과 기기 토큰을 연결해 DB에 저장하고, 알림 전송 시 해당 토큰을 FCM에 전달합니다. FCM 단일 기기 전송
추가로 필요합니다.
Android: google-services.json
iOS: GoogleService-Info.plist와 Apple APNs 인증 키를 Firebase에 등록
로그아웃: 서버에서 해당 기기 토큰 삭제
토큰 갱신: 앱에서 새 토큰을 서버에 재등록
FCM이 토큰 만료를 반환하면 DB에서 비활성화
즉, 회원가입 API 토큰을 FCM에 제공하는 것이 아니라, 회원 인증 후 앱이 별도로 발급받은 FCM 기기 토큰을 해미 서버에 등록하는 구조입니다.