# 개발 플랜 — #39 F3-01 회상 세션 구현

> feat · M3 회상 세션. #38(정오답 삭제) 위에 구축.

## 1. 이슈 분석

작업 항목 3종:
1. **발화 감지 흐름** — 어르신의 발화(음성 응답)를 세션에 기록. 현재 `answer` 엔드포인트가 발화 제출 역할이나 "정오답" 용어가 남아있음.
2. **4초 힌트 노출 / 7초 자동 재생** — 회상 세션 타이밍 설정을 세션 응답에 노출(클라이언트가 힌트/자동재생 시점 판단).
3. **60초 무응답 허용** — 무응답 상태로 다음 문제 진행을 정식 허용(손주 찬스 만료 게이팅과 무관).

S1 회귀(EX-F301-10 부적합 상태 세션 개시 차단 / EX-F301-11 세션 중 사별 등록 즉시 종료)는 **어르신 상태 머신(#36)에 의존** → 상태 조회 API 부재로 이번 스코프 제외, 릴리스 게이트 #50에서 #36 완료 후 처리.

## 2. 설계

- 도메인 상수: `HINT_DELAY_SECONDS=4`, `AUTO_PLAY_DELAY_SECONDS=7`, `NO_RESPONSE_ALLOWANCE_SECONDS=60`.
- `CognitiveTrainingSession.recordNoResponse(questionId)`: 현재 문제를 무응답(responded=false, 60초)으로 기록하고 진행. 손주 찬스 게이팅 없음. 완료 시 기존 complete 흐름 재사용.
- 세션 응답 DTO에 `RecallTiming`(hintDelaySeconds/autoPlayDelaySeconds/noResponseAllowanceSeconds) 노출.
- 서비스 `recordNoResponse(command)` + 완료 시 난이도 조정.
- 컨트롤러 `POST /{sessionId}/no-response`, Swagger 문구를 회상/발화 기준으로 정리.

## 3. 변경 파일
- 도메인: `CognitiveTrainingSession`(상수 + recordNoResponse)
- 응용: `RecordNoResponseCommand`, `TrainingApplicationService`
- DTO: `TrainingSessionResult`(RecallTiming 추가)
- 표현: `TrainingController`(엔드포인트 + 문구)
- 테스트: 세션 도메인/서비스 + 컨트롤러 흐름

## 4. 자체 검증
- `./gradlew test`
- 무응답 진행/타이밍 노출 단위 테스트 추가

## 5. PR
- 브랜치 `feat/f301-recall-session/#39` → develop, Closes #39. S1 항목 out-of-scope 명시.
