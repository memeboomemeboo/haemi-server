# plan-101: 어르신 참여 로그인 (F0-01-E)

- 이슈: #120
- 명세: 기능명세서 v3.0.3 F0-01-E (v3.0.3 신설), F0-01, F0-05

## 배경

어르신은 어르신 화면에서 **성함·전화번호·초대 코드(6자리)**를 최초 1회 입력해 가족 그룹의
elder로 합류하고, 이후 **평생 세션(자동 로그인)**으로 유지된다. 현재 `invitation`은 가족
초대(이메일 + 링크 토큰) 전용이라 어르신 합류 경로 자체가 없다.

## 설계 결정

### 1. 어르신 세션 principal은 기존 `Member(role=ELDER)` 모델을 재사용한다

명세는 "어르신 세션 principal은 `elder_id`"라고 쓰지만, 서버에는 이미 어르신 기기용 인증
경로가 있다 — `Member(role=ELDER)`를 `Elder.memberId`로 연결하고, `hasRole('ELDER')`와
`ElderDeviceIdentityQuery.isLinkedElderMember`로 대상을 판정한다
(`ElderMemoryFeedController`, `HomeController`, `device_tokens.elder_id`).

여기에 별도 principal 타입을 새로 만들면 위 검증들을 전부 이중화해야 하므로, 참여 로그인은
**계정 발급/연결까지만 자동화**하고 세션 수명만 `elder_sessions`로 따로 관리한다. 어르신은
비밀번호 로그인을 하지 않으므로 자격증명은 임의값으로 둔다.

기존 수동 연결 경로(`PUT /elders/{id}/member`)와 공존한다. 이미 연결된 계정이 있으면 그
계정을 그대로 재사용해 중복 계정을 만들지 않는다.

### 2. 세 항목의 역할 분리

| 항목 | 역할 |
| --- | --- |
| 초대 코드 | 어느 가족 그룹의 elder인지 결정하는 핵심 키 |
| 성함 | 코드가 그 어르신 본인에게 쓰였는지 교차 검증 |
| 전화번호 | 식별·중복 방지용. `phone_hash`로만 저장, **OTP 미사용** |

### 3. 성함 불일치는 차단이 아니라 보류

EX-F001E-02는 "합류 보류 + owner 확인 요청"이다. 코드를 폐기하지 않고 `invitations.held_at`만
남겨, owner가 확인한 뒤 같은 코드로 다시 시도할 수 있게 한다.

### 4. 상태 판정은 `isDispatchable`이 아니라 `isLiving`

`isDispatchable`은 ACTIVE/DECLINING만 통과시키므로, 이 기준으로 세션을 막으면 입원·휴면
어르신이 퇴원 후 로그인 화면을 다시 만난다. 입원·휴면은 **발송만 멈추는 상태**이므로 세션
판정에서는 분리하고, 사별·추모(DECEASED/MEMORIAL)에서만 막는다.

### 5. 사별 오등록 복구 시 세션을 되살린다

사별 확정에서 세션을 끊는 것은 EX-F005-06(S1)의 서버 측 관문이다. 다만 48시간 내 오등록
복구까지 세션을 죽인 채로 두면 어르신이 스스로 재로그인할 수 없다(검증 지표: 재로그인 요구
0건). 복구 리스너에서 `ELDER_STATUS_CHANGED` 사유로 끊긴 세션만 되살린다.

### 6. 코드 유일성은 부분 인덱스가 아니라 발급 시 재시도로 보장

`CREATE UNIQUE INDEX ... WHERE`는 H2가 지원하지 않아 `FlywayMigrationTest`가 깨진다. 코드는
미수락(pending) 범위에서만 유일하면 되므로, 발급 시 pending 충돌이 없을 때까지 다시 뽑는다.

## 데이터 모델

| 테이블 | 변경 |
| --- | --- |
| `invitations` | `kind(FAMILY/ELDER)`, `code(6자리)`, `attempt_count`, `held_at` 추가. `relation` NOT NULL 해제 |
| `elders` | `phone_hash` 추가 |
| `elder_sessions` | 신규 — 기기 바인딩 rolling refresh 세션 |

## API

| 메서드 | 엔드포인트 | 설명 |
| --- | --- | --- |
| POST | `/api/v1/groups/{id}/invitations` | `kind=ELDER`면 6자리 코드 발급(기본 FAMILY) |
| POST | `/api/v1/invitations/{code}/accept-elder` | 성함·전화번호·코드 검증 → elder 연결 + 평생 세션 |
| POST | `/api/v1/elder-sessions/refresh` | silent refresh, 사용 시마다 만료 연장 |
| DELETE | `/api/v1/elders/{elderId}/sessions` | owner 전용 기기 연결 초기화 |

앞 두 경로는 토큰을 받기 전 단계이므로 `permitAll`이다. 코드 대입은 초대 1건당 시도 10회로
제한한다(`Invitation.MAX_CODE_ATTEMPTS`).

## 예외 처리

| ID | 처리 |
| --- | --- |
| EX-F001E-01 | 코드 불일치 → 400 "코드를 다시 확인해주세요." |
| EX-F001E-02 | 성함 불일치 → 409 "정보가 맞는지 확인 중이에요." + `held_at` 기록 |
| EX-F001E-03 | 코드 만료 → 400 "초대가 만료되었어요. 다시 요청해주세요." |
| EX-F001E-04 | 번호 중복 → 409 "이미 참여 중인 가족이 있어요." |

어느 항목이 틀렸는지는 알려주지 않는다. 대입 공격에 힌트를 주지 않기 위해서다.

## 테스트

`ElderJoinApiIntegrationTest`
- 합류 → 어르신 전용 API 호출 → refresh 회전 → 옛 토큰·다른 기기 거부
- 성함 불일치 보류 후 정정 재시도 성공, 잘못된 코드 거부
- 같은 번호로 다른 그룹 합류 차단
- 사별 확정 시 세션 폐기(EX-F005-06), owner 세션 초기화
- 어르신 초대는 가족 수락 경로로 소비 불가, 정원(10명) 미차감

## 남은 과제 (이번 범위 밖)

- `m2` 추억 등록/피드에 역할 가드가 없어 ELDER 토큰으로도 호출된다. 명세 F0-01-E ⑥의
  "업로드 API 비허용"에 어긋나므로 별도 인가 정리가 필요하다.
- 어르신 access token 수명이 전역 설정(기본 30분)을 따른다. 명세 예시는 1시간이다.
