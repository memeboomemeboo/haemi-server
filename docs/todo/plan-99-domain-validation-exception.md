# plan-99 · 도메인 검증 예외 타입 분리

이슈: #99 `refactor: IllegalArgumentException 일괄 400 매핑이 서버 오류를 가림`
브랜치: `refactor/domain-validation-exception/#99` (base: develop, 스택 최하단)

## 목표

`GlobalExceptionHandler`의 `IllegalArgumentException → 400` 일괄 매핑을 제거한다.
그 대신 **의도적인 사용자 검증**만 새 타입 `DomainValidationException`으로 옮겨 400을 유지하고,
JDK·라이브러리가 던지는 진짜 `IllegalArgumentException`은 다시 500으로 돌려보낸다.

성공 기준:
- 사용자에게 보이는 400 응답 메시지는 그대로 유지된다 (동작 변화 없음).
- 예기치 못한 `IllegalArgumentException`은 500 + "서버 오류가 발생했습니다."로 떨어지고 로그가 남는다.
- 내부 예외 메시지가 그대로 밖으로 나가는 경로가 사라진다.

## 설계

### 1. 새 예외 타입

```
com.memeboo2.haemi.common.exception.DomainValidationException  (extends RuntimeException)
```

`IllegalArgumentException`을 상속하지 **않는다**. 상속하면 기존 `catch (IllegalArgumentException)`
블록들이 도메인 검증 예외를 계속 삼켜서 분리 자체가 무의미해진다.

`common` 패키지는 지금 없다. m2·m3·m4가 공유해야 하므로 신설한다.

### 2. UUID 파싱 헬퍼

```
com.memeboo2.haemi.common.support.DomainIds.parseUuid(String value, String label)
```

이게 이번 작업의 **가장 큰 함정**이다. 현재 `UUID.fromString`이 잡히지 않은 채로 호출되는 곳이
m1·m2·m4·m5에 흩어져 있고, 지금은 일괄 핸들러 덕분에 400으로 나간다
(메시지도 `Invalid UUID string: ...`이 그대로 노출된다). 핸들러만 지우면 전부 500이 된다.

해당 지점을 헬퍼로 바꿔 `DomainValidationException`을 던지게 하고, 메시지도 한국어로 정리한다.

### 3. 핸들러

- `GlobalExceptionHandler`: `handleIllegalArgument` 제거 → `handleDomainValidation(DomainValidationException)` 400 추가.
- `M3ExceptionHandler`: bad-request 목록의 `IllegalArgumentException.class` → `DomainValidationException.class`.

## 이관 대상

### 도메인/애플리케이션 검증 (throw → DomainValidationException)

| 파일 | 라인 |
| --- | --- |
| m2 `notification/ElderNotificationPolicy` | 19 |
| m2 `notification/QuietHours` | 13 |
| m3 `service/DifficultyPolicyApplicationService` | 38 |
| m3 `hint/AccruedHint` | 58, 61 |
| m3 `training/CognitiveTrainingSession` | 205, 224, 231, 341, 345 |
| m3 `training/DifficultyPolicy` | 112, 116, 119, 122, 125 |
| m3 `ai/AlbumCognitiveQuestionGeneratorAdapter` | 27, 61 |
| m3 `tts/SsmlTrainingSpeechAdapter` | 16 |
| m4 `service/ActivityChangeLanguagePolicy` | 19 |
| m4 `service/DashboardApplicationService` | 304, 308, 387 |
| m4 `dashboard/AlertRecipientSetting` | 47, 50 |
| m4 `dashboard/CognitiveReport` | 167 |

### UUID 파싱 (헬퍼로 교체)

`AlbumId.of` · `PhotoId.of` · `ReminiscenceContentId.of` · `MemoryPostId.of` · `TrainingSessionId.of`,
m2 `GroupGoalApplicationService` 45·64, m2 `MemoryPostApplicationService` 98·212,
m4 `DashboardApplicationService` 59·395, m5 `CareApplicationService` 68·98·121·136.

### 손대지 않는 곳

- `ElderHealthCrypto:62` — 바로 아래 `catch (Exception)`에서 `IllegalStateException`으로 감싸는 내부 가드다.
- `JwtTokenProvider:93`, `OpenApiConfig:107` — 라이브러리 예외를 잡는 쪽이지 던지는 쪽이 아니다.
- `catch (IllegalArgumentException)` 블록들 — `UUID.fromString`이 여전히 IAE를 던지므로 그대로 유효하다.

## 순서

1. `DomainValidationException` + `DomainIds` 추가
2. 도메인/애플리케이션 검증 throw 이관 (m2 → m3 → m4)
3. UUID 파싱 지점 헬퍼로 교체
4. 핸들러 두 곳 정리 (일괄 매핑 제거)
5. 기존 테스트의 `isInstanceOf(IllegalArgumentException.class)` 9곳 갱신
6. `IllegalArgumentException`이 500으로 떨어지는 것을 확인하는 핸들러 테스트 추가
7. 전체 테스트

## 커밋 분할

1. `refactor: 도메인 검증 예외 타입과 UUID 파싱 헬퍼를 추가한다`
2. `refactor: m2·m3·m4 도메인 검증을 DomainValidationException으로 옮긴다`
3. `refactor: 사용자 입력 UUID 파싱을 도메인 검증 예외로 통일한다`
4. `refactor: IllegalArgumentException 일괄 400 매핑을 제거한다`
5. `test: 예외 매핑 변경에 맞춰 테스트를 갱신한다`
