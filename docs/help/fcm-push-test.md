# FCM 푸시 알림 실제 발송 테스트 (#80)

로컬에서 실제 FCM 발송을 끝까지 확인하는 절차예요. 클라이언트 앱이 없어도
`tools/fcm-token-tester` 페이지로 진짜 등록 토큰을 발급받아 테스트할 수 있어요.

## 1. Firebase 준비

Firebase 콘솔에서 두 가지를 받아요.

| 항목 | 위치 | 쓰는 곳 |
| --- | --- | --- |
| 서비스 계정 JSON | 프로젝트 설정 → 서비스 계정 → 새 비공개 키 생성 | 서버 |
| 웹 앱 설정 + VAPID 키 | 프로젝트 설정 → 일반 → 내 앱(웹) / 클라우드 메시징 → 웹 푸시 인증서 | 테스터 페이지 |

받은 서비스 계정 JSON은 프로젝트 루트에 두고 `.env`에 **경로만** 적어요.
(`*service-account*.json` 패턴은 `.gitignore`에 있어서 커밋되지 않아요.)

```bash
echo "FIREBASE_CREDENTIALS=$(pwd)/service-account.json" >> .env
```

## 2. 서버를 dev 프로필로 기동

테스트 발송 엔드포인트와 CORS 허용은 **dev 프로필에서만** 열려요.

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

기동 로그에서 자격증명이 붙었는지 바로 확인돼요.

- `FCM 자격증명을 불러왔습니다. 푸시 알림을 FCM으로 발송합니다.` → 실제 발송 준비 완료
- `FCM 자격증명이 없어 푸시 알림을 로그로만 남깁니다.` → `.env`가 안 먹은 상태

## 3. 계정 만들고 액세스 토큰 받기

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"email":"push-test@haemi.test","password":"Test1234!","name":"푸시테스트","role":"FAMILY"}'
```

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"push-test@haemi.test","password":"Test1234!"}'
```

응답의 `accessToken`을 복사해 두세요.

## 4. 테스터 페이지로 기기 토큰 발급

```bash
python3 -m http.server 8000 --directory tools/fcm-token-tester
```

<http://localhost:8000> 을 열고,

1. Firebase 웹 앱 설정 4개(`apiKey` · `projectId` · `messagingSenderId` · `appId`)와 VAPID 키를 입력해요.
2. **토큰 발급받기** → 브라우저 알림 권한을 허용하면 FCM 등록 토큰이 나와요.
3. 서버 주소와 3단계에서 받은 액세스 토큰을 넣고 **POST /api/v1/device-tokens** 를 눌러 등록해요.
4. **테스트 발송** 을 누르면 본인 기기로 알림이 갑니다.

입력값은 브라우저 localStorage에만 저장되고 서버로 전송되지 않아요.

## 5. 결과 해석

테스트 발송 응답이 실제로 무슨 일이 있었는지 알려줘요.

```json
{ "success": true, "data": { "successCount": 1, "failureCount": 0, "invalidTokens": [] },
  "message": "1대에 발송했어요." }
```

| 증상 | 원인 |
| --- | --- |
| `successCount: 0`, 안내 문구 | 토큰 미등록이거나 자격증명 미설정 (2단계 로그 확인) |
| `invalidTokens`에 토큰이 담김 | 만료·무효 토큰. 서버가 자동으로 지웠으니 페이지에서 다시 등록 |
| `failureCount`만 올라감 | FCM 일시 오류. 토큰은 보존되니 재시도 |
| 응답은 성공인데 알림이 안 보임 | 브라우저 알림 권한·집중 모드 확인. 백그라운드 알림은 탭을 다른 창으로 옮긴 뒤 다시 발송 |

curl로 직접 쏘려면:

```bash
curl -s -X POST http://localhost:8080/api/v1/device-tokens/test-send \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"해미 테스트","body":"발송 확인용 알림이에요"}'
```

## 운영 환경에서는

`test-send` 엔드포인트와 CORS 허용 빈은 dev 프로필에서만 등록돼요. 운영에서는
컨트롤러 자체가 없어 404이고, 이 사실은 `TestPushDisabledInProdIntegrationTest`가 검증해요.
운영 배포 시에는 `FIREBASE_CREDENTIALS`만 주입하면 됩니다.
