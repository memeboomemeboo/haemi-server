# 개발 플랜 — #45 F2-02 어르신 답변 (음성·이모지)

> feat · M2 · #44 위에 스택. 어르신 답변을 **음성 우선 + 마음 이모지 6종**으로 재설계, 텍스트 직접 입력 제거.

## 1. 이슈 분석
작업 항목: 음성 60% 우선 / 마음 이모지 6종 / 텍스트 직접 입력 삭제 / 즉시 전송.
현재 `ReplyType {POEM, IMAGE, SHORT_TEXT}`는 텍스트·시 직접 입력을 포함 → 음성/이모지 2모드로 축소.
사용처는 m2 post 모듈 + 테스트로 한정. `generatePoemDraft`(F2-03 AI 시 초안)는 `ReplyType`을 참조하지 않으므로 본 이슈 범위 밖(그대로 둠).

## 2. 설계 (기존 M2 컨벤션)
### 도메인
- `ReplyType` → `{ VOICE, EMOJI }` (POEM/IMAGE/SHORT_TEXT 제거).
- `HeartEmoji` (신규 enum) — 마음 이모지 6종(❤️🤗😊🥹🙏🥰, 각 code+label). `isValidCode`/`fromCode`.
- `ElderReply.of` 검증 재작성: VOICE(전사 텍스트 비어있지 않음, ≤300), EMOJI(6종 코드만 허용).
- `InvalidHeartEmojiException`(신규) — 400 매핑.
### 애플리케이션
- `ReplyToPostCommand` → `{ postId, elderId, replyType, heartEmojiCode, voiceInputStream, voiceContentType }` (textContent·imageKeyOrEmoji 제거).
- `replyToPost` 재작성: VOICE는 음성 STT 필수, EMOJI는 6종 코드. **즉시 전송**(초안 단계 없음, 기존과 동일하게 저장 즉시 반영). 음성이 1순위 경로.
### 프레젠테이션
- `ReplyToPostRequest` → `{ elderId, replyType(VOICE|EMOJI), heartEmojiCode }`. 컨트롤러 multipart(voice 파트) 유지, poem-draft 엔드포인트는 미변경.
- OpenAPI 문구 F2-02·음성/이모지로 갱신, `M2ExceptionHandler`에 신규 예외 400 등록.

## 3~5. 검증
- 단위: `HeartEmojiTest`(6종·유효성), `MemoryPostTest`/`ElderReply` 검증(VOICE/EMOJI 허용, 잘못된 이모지 거부, 텍스트 유형 부재), 기존 테스트 갱신(SHORT_TEXT→VOICE/EMOJI).
- 통합: `./gradlew clean test` 전량 green.
- 마이그레이션 불필요(reply_type은 @Enumerated STRING, 스키마 불변).

## 6~9
- 커밋 분리: (a) 도메인(ReplyType·HeartEmoji·ElderReply·예외), (b) 애플리케이션·프레젠테이션 연동, (c) 테스트 갱신+플랜
- Draft PR, **base = 첫 브랜치 feat/f301-recall-session/#39**(스택 머지 구조), 본문에 스택 순서·머지 순서 명시, Closes #45

## Out-of-scope
- F2-03 AI 시 초안(`generatePoemDraft`) 재정비 — 별도 이슈. 본 PR은 답변 입력 모드에 한정.
- S1 회귀(EX-*)·어르신 상태 머신(#36) 의존 항목 — #50 이월.
