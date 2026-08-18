# 기능명세서 v3.0.3 대비 서버 갭 분석

대상 명세: 해미 기능명세서 v3.0.3 (통합판) — 어르신 참여 로그인 반영
기준 코드: `develop` (510599a)

현재 코드는 v3.0 본문(M0~M5)을 상당 부분 구현한 상태다. 아래는 **v3.0.1 / v3.0.2 / v3.0.3 개정분**과, 본문 대비 확인된 실제 구현 공백을 정리한 것이다.

---

## A. v3.0.3 개정 — 어르신 참여 로그인 (최우선, P0, 신규)

현재 `Invitation`(`m0/domain/model/Invitation.java`)은 가족 초대 전용이다. `kind`, 6자리 코드, `phone_hash`, 어르신 평생 세션이 모두 없다.

### A-1. `invitation` 스키마 확장 — 필수
- `kind` 컬럼 추가 (`FAMILY` / `ELDER`). 신규 마이그레이션 `V33__add_elder_invitation_and_session.sql`.
- `kind=ELDER`일 때는 `token`(96자 Base64) 대신 **6자리 숫자 `code`** 를 발급. 현재 `token`은 `nullable=false, unique`이므로 컬럼 분리 또는 nullable 완화 필요.
- `kind=ELDER` 초대는 `invitee_email_hash`, `relation`이 의미 없음 → nullable 처리 필요(현재 둘 다 `nullable=false`).
- 만료 72h는 이미 일치(변경 불필요).
- 6자리 코드는 전역 유일이 아니어도 되나, **미만료 pending 범위에서 유일**해야 하며 브루트포스 방어(시도 횟수 제한/잠금)가 필요하다. 현재 초대 수락 경로에 rate limit 없음.

### A-2. `elder_session` 테이블 및 평생 세션 발급 — 필수 (신규)
- 필드: `session_token_id(PK)`, `elder_id`, `group_id`, `device_id`, `issued_at`, `last_refreshed_at`, `rolling_expires_at`.
- access token 단기(1h) + refresh token 장기(180일) + **rolling 갱신**. 현재 `auth`의 refresh 토큰 정책과 별도 정책이 필요하다(어르신은 만료 연장형).
- 기기 바인딩: refresh token은 `device_id`에 바인딩. 기존 `ElderDeviceAccessValidator`, `device_commands`(V27), V23 `link_elder_devices_to_profiles` 자산을 재사용할 것.
- **principal 설계 변경**: 현재 인증 주체는 `Member`(`AuthenticatedMember`) 뿐이다. 어르신 세션 principal은 `elder_id` 기반이어야 하며, `MemberRole.ELDER` 계정 방식(ADR-0001에서 이미 지양)과 혼용하지 않도록 별도 principal 타입 + `SecurityFilterChain` 분기가 필요하다.
- **권한 최소화**: 어르신 세션은 `자기 콘텐츠 열람 + 세션 결과 전송`만 허용. 리포트(`/api/v1/cognitive-dashboard/**`), 업로드(`/photos`, `/posts`), 기관 포털은 전면 차단. 현재 이 경계에 대한 인가 정책이 없음 → 신규 인가 규칙 필요.

### A-3. API 추가 — 필수
| 메서드 | 엔드포인트 | 상태 |
| --- | --- | --- |
| POST | `/api/v1/groups/{id}/invitations` | **수정** — `kind` 파라미터 수용, elder면 6자리 코드 반환 |
| POST | `/api/v1/invitations/{code}/accept-elder` | **신규** — 성함·전화번호·코드 검증 → elder 연결 + 평생 세션 발급 |
| POST | (어르신) 토큰 silent refresh | **신규 또는 기존 `/auth/refresh` 분기** — 어르신 세션은 rolling 연장 |

`FamilyGroupController`의 기존 `/invitations/{token}/accept`는 가족 전용으로 유지.

### A-4. 검증 로직 — 필수
- 성함 대조: 입력 성함 vs `elder.name`. 불일치 시 **차단이 아니라 합류 보류 + owner 확인 요청**(EX-F001E-02, S3). 보류 상태를 표현할 필드/상태값이 현재 없음.
- 전화번호: `phone_hash`(OTP 미사용) 저장. 현재 `Elder`에 `phone_hash` 필드 없음 → 추가 필요. 해시 방식은 기존 `invitee_email_hash`와 동일 규약 사용 권장.
- 번호 중복(다른 그룹 소속) 감지 → 그룹 선택/확인 응답(EX-F001E-04).
- 어르신 폰 미보유 → 가족 폰 임시 세션(EX-F001E-05 / EX-F003-04 연계). **미구현.**

### A-5. F0-05 연계 — S1
- 어르신 상태 변경 시 서버가 해당 `elder_session`을 **무효화**해야 기기 원격 잠금(EX-F005-06)이 실효를 갖는다. 현재 `device_commands` 아웃박스만 있고 세션 무효화 경로가 없다.
- 어르신 화면에는 로그아웃 UI가 없으므로, 세션 종료·초기화는 **owner 경로 API로만** 제공해야 한다(신규).

### A-6. 예외 처리 신규 (EX-F001-08~10, EX-F001E-01~05)
전부 미구현. 문구까지 명세에 고정되어 있으므로 메시지 상수화 필요.

---

## B. v3.0.2 개정 — MVP 알림 범위 축소 (Phase 2 이관)

### B-1. 약 복용 알람 → Phase 2 이관 — 필수
- `m5/domain/model/care/AlarmType.MEDICATION` 이 살아 있고, `CareApplicationService:229` "약 드실 시간이에요", `:170` "약을 드셨어요." 응답 통지까지 구현되어 있다.
- 조치: MVP 빌드에서 `MEDICATION` 생성·발송 경로 차단(피처 플래그 또는 enum 제거 + 기존 데이터 마이그레이션). 규제 사유(의료기기 프레임 근접, §10.5)이므로 **문구까지 제거**해야 안전하다.

### B-2. 운영성/품질 알림 → Phase 2 — 확인 필요
- 그룹 합류 축하 푸시: 현재 코드에 해당 푸시 없음(앱 내 표시만 필요) → **추가 구현 금지** 상태 유지. F0-01 흐름도 6단계는 앱 내 표시로만 구현.
- 발화 결측 시 마이크 권한 개선 안내 푸시: 미구현 상태 유지.

### B-3. MVP 유지 대상(예외 안내) — 확인 필요
- 어르신 알림 권한 없음(EX-F201-01), Mode B 동반자 부재(EX-F003-03) 안내는 **유지**. 현재 구현 여부 재확인 대상.

### B-4. F5-02 산책 — 보류인데 코드가 남아 있음
- `CareApplicationService`에 `createWalkRoutine` / `startWalk` / 날씨 포트가 살아 있으나 `CareController`에는 산책 엔드포인트가 없다. **도달 불가 코드 + 외부 날씨 의존**이 남아 있는 상태.
- 조치: 보류 결정에 맞춰 제거하거나, 명시적으로 비활성 표시. 대안(F5-01 `ETC` 유형 음성 알람)은 이미 가능.

---

## C. v3.0.1 개정 — F4-04 회상 참여 지표 엔진 (신규, P1)

현재 코드에 REI 엔진이 **전혀 없다**. F4-01 서술문(`DashboardApplicationService.buildReminiscenceSummary`)과 F4-02 알림 문구(`detectEarlyAlerts`)가 각자 데이터를 해석하고 있어, 명세가 지적한 "문구 불일치" 위험이 그대로 존재한다.

### C-1. `ReminiscenceEngagementIndex` 내부 엔진 신설 — 필수
- 5차원 밴드 판정: 참여일수 / 발화 발생률(1순위) / dwell / 무응답률 / 가족 주간 활성.
- 개인 기준선 = **직전 4주 이동평균**. 현재 F4-02는 "직전 7일 vs 최근 7일"만 본다 → 기준선 산출 로직 신규.
- 밴드 확정 조건: **3세션 이동평균 + 3일 이상 지속**. 현재 F4-02는 `lastThreeDays.allMatch`로 유사하나 이동평균은 아님.
- 발화 결측 시 **발화 차원 제외 후 잔여 4차원 판정**(EX-F404-03 / EX-F402-08). 현재는 `previousVoiceRate > 0` 가드로만 우회 → 명시적 결측 플래그 필요.
- 신규 배치 금지: F4-01(주간 월 09:00 / 월간 1일 09:00), F4-02(자정 배치) **생성 직전 파생 계산**.

### C-2. 조언 결정표 구현 — 필수
band → 가족 조언 템플릿 / 시스템 조정(F3-02 레벨·힌트 선제공 시점) / 에스컬레이션(기관 통지) 매핑. 현재 F4-02 알림은 조언 문구만 있고 **시스템 조정 연동과 에스컬레이션 규칙이 없다**.
- "3개 차원 이상 동시 주의 + 3일 지속 → 기관 담당자 통지" 룰 신규.

### C-3. 어휘 검사기 공유 — 부분 구현
- `ActivityChangeLanguagePolicy`가 이미 있고 F4-01/F4-02 양쪽에서 호출된다(양호).
- 보강 필요: F4-04 금지 슬롯("줄었다/나빠졌다", 숫자 등급, 미래 예측, 발화 "내용" 주장)이 현재 금칙어 목록에 없다. 특히 현재 `buildActivityMessage`가 **"함께한 날이 N일 줄었어요"** 를 생성하는데, 이는 F4-04 금지 슬롯("줄었다")에 정면 저촉된다 → **수정 필요(S1급 설계 위반)**.
- 밴드·수치의 UI/PDF/알림 노출 금지(EX-F404-02)를 린트/테스트로 강제 필요.

### C-4. 슬롯-데이터 검증(EX-F404-05) — 미구현
서술문 생성 후 슬롯값과 집계값 일치 검증, 불일치 시 재생성 → 실패 시 최소 서술 폴백.

---

## D. F4-01 회상 리포트 — 데이터 정의 정합화 (v3.0.1)

### D-1. 1면 인물·장소 순위 정의 — 수정 필요
- 명세: 인물 태그(F0-04) · 장소(F1-04 `PhotoMemo.place`)를 **`card_dwell` 응시 / `voice_activity.detected` 발화 감지 가중으로 집계한 반응 빈도**.
- 현재: `CognitiveDailyMetric.topMemoryTopic` 단일 문자열을 **클라이언트가 `RecordReminiscenceMetricCommand`로 올린 값**을 그대로 상위 3개 집계. 인물/장소 분리 없음, 응시·발화 가중 없음, 서버 집계 아님.
- 조치: 인물 TOP3 / 장소 TOP3를 분리 저장하고, 서버 측에서 태그 × (dwell, VAD) 가중 집계로 산출.

### D-2. 3면(어르신의 목소리) 음성 저장 동의 종속 — 미구현
- 명세: F2-02 **음성 저장 동의를 받은 경우에만** 3면 구성. 미동의 시 **항상 생략**(빈 면 금지) + 설정 화면 동의 안내(EX-F401-07).
- 현재: `voiceResponseCount`를 조건 없이 리포트에 포함. 동의 모델 자체가 없다 — `EventCollectionConsent`(이벤트 수집 동의)만 존재하고 **음성 내용 저장 동의는 별도 동의 항목으로 분리되어야 한다(§10.4: 서비스 이용 / 민감정보 / 음성 내용 저장 / 연구 활용 4종 분리 동의)**.
- 조치: 동의 항목 테이블 신설 + 3면 구성 게이트 + `EX-F401-07` 처리.

### D-3. 5면 서술 — F4-04 위임
`buildActivityMessage` / `buildReminiscenceSummary`를 F4-04 템플릿·결정표 산출물로 대체.

### D-4. 잔여 레거시 필드
`CognitiveReport`에 `averageAccuracyRate`, `accuracyTrend`, `accuracy_change_from_previous`, `most_reacted_photo_type` 등 v2.0 정답률 계열 필드가 남아 있다. 표시 금지 항목이므로 DTO에는 빠져 있으나, **저장·PDF 렌더 경로에서 유출되지 않는지 확인 후 제거 권장.**

---

## E. F0-06 이벤트 스키마 불일치 (본문 대비)

`eventlog/domain/EventType`의 11종이 명세 부록 B의 11종과 **이름·구성이 다르다.**

| 명세 | 코드 | 상태 |
| --- | --- | --- |
| `session_start` | `SESSION_START` | 일치 |
| `card_shown` | — | **누락** |
| `card_dwell` | — | **누락** (F4-01/F4-04 dwell 집계의 원천) |
| `voice_activity` | `VAD_DETECTED` | 이름 불일치, `duration_ms`·`card_id` 필드 확인 필요 |
| `card_response` | — | **누락** (F3-02 개인화 엔진 입력) |
| `hint_used` | `HINT_USED` | 일치 (`HINT_SERVED`는 명세 외 추가) |
| `session_end` | `SESSION_COMPLETE` / `SESSION_ABANDON` | 이름 불일치, `completed`·`exit_index`·`duration_ms` 필드로 통합 필요 |
| `family_contribution` | — | **누락** (가족 주간 활성률 = L1 지표) |
| `notification_sent` / `_opened` | `NOTIFICATION_SENT` / `NOTIFICATION_ACK` | `_opened` 명칭 정합화 |
| `ui_error` | — | **누락** (조작 실패율 = L1 지표, Stage 1 중단 기준) |
| `system_error` | `SYSTEM_ERROR` | 일치 |

- 누락 이벤트는 **소급 생성 불가**(§12.2 Phase 1 필수 항목 1번). 최우선 보강 대상.
- `schema_v` 버전 필드 관리(F0-06 ⑥) 구현 여부 확인 필요.
- `ts`(발생 시각) / `received_at`(수신 시각) 분리 저장 확인 필요.
- 이벤트 누락률 1% 초과 알림(EX-F006-04) 미구현으로 보임.

> 참고: 명세 §12.2는 "이벤트 로깅 9종"이라 쓰고 부록 B는 11종을 나열한다. **명세 내부 불일치**이므로 기획 확인 필요.

---

## F. S1 회귀 테스트 게이트 (부록 A)

`src/test/java/com/memeboo2/haemi/regression/S1RegressionSuiteTest.java`는 20건 중 **18건**을 덮고 있다.

- **누락 1: `EX-F106-04`** — 타임라인 숨김 인물 노출(렌더 직전 검증)
- **누락 2: `EX-F402-07`** — 사별 후 활동 변화 안내 발송 차단

추가로 v3.0.3/3.0.1 신설분에 대한 S1 케이스 확장이 필요하다.
- `EX-F404-02` 지표·밴드 UI/PDF 노출 → 빌드 차단(린트 또는 테스트)
- `EX-F404-04` 조언 금지 표현 유입 → 어휘 검사 통과
- `EX-F005-06` 확장: 상태 변경 시 `elder_session` 무효화 확인

---

## G. 기타 확인·정리 대상

1. **동의 4종 분리(§10.4)** — 서비스 이용 / 민감정보 / 음성 내용 저장 / 연구 활용. 현재는 `event_collection_consent` 단일. 동의 모델 재설계 필요. (D-2와 동일 작업)
2. **보존 정책(§10.4)** — 이벤트 24개월, 사별 후 12개월 보존 후 재문의. 파기 배치 구현 여부 확인 필요.
3. **기관 계정 elder_id 가명 처리**(F0-06) — 기관 포털 집계 시 가명화 적용 여부 확인. 감사 로그(`institution_portal_audit_logs`, V31)는 구현됨.
4. **성능 목표(§10.2)** — 콘텐츠 생성 배치 어르신 1,000명 30분, API p95 500ms. 현재 배치 페이징(plan-85)은 있으나 성능 회귀 기준선 없음.
5. **API 경로 규약** — 명세는 `/v1/...`, 코드는 `/api/v1/...`. 실질 문제는 아니나 명세-코드 정합 표기 정리 권장.
6. **F0-01 흐름도 6단계** — 그룹 합류 알림은 "앱 내 표시"로만 (푸시 금지, B-2 참조).

---

## 우선순위 제안

| 순위 | 항목 | 근거 |
| --- | --- | --- |
| P0 | A. 어르신 참여 로그인 전체(초대 kind, 코드, phone_hash, elder_session, 인가 최소화) | v3.0.3 핵심, 어르신 진입 자체가 막힘 |
| P0 | E. 누락 이벤트 5종(`card_shown`, `card_dwell`, `card_response`, `family_contribution`, `ui_error`) | 소급 생성 불가, Stage 1/2 검증 불능 |
| P0 | A-5. 상태 변경 시 elder_session 무효화 | S1 (EX-F005-06) |
| P0 | C-3. "줄었다" 표현 제거 및 금칙어 보강 | S1 (EX-F404-04) 현행 위반 |
| P1 | B-1. 약 복용 알람 Phase 2 이관 | 규제 리스크 |
| P1 | C. F4-04 REI 엔진 + 조언 결정표 | F4-01/F4-02 문구 정합의 전제 |
| P1 | D-1/D-2. 리포트 1면 집계 정의, 3면 동의 게이트 | 라벨-데이터 정합, 동의 범위 초과 방지(R12) |
| P2 | F. S1 회귀 2건 보강 + 신설 케이스 | 릴리스 게이트 |
| P2 | B-4. F5-02 잔존 코드 정리 | 보류 결정 정합 |
| P2 | G. 동의 4종 분리, 보존 배치, 가명화 | 규제·개인정보 |
