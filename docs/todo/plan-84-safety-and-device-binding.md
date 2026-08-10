# 개발 플랜 — #84 회상 안전 검증·기기 토큰 연결 수정

> fix · PR #83 리뷰 후속 · 스택 최하단(base = develop). 동작 결함 2건 교정.

## 1. 이슈 분석

### 1-1. ContentSafetyValidator가 명시적 현재형을 통과시킨다

PR #82의 과차단(`"있"`, `"살"` 같은 초빈출 음절이 현재형 마커) 지적을 고치면서, 마커를 어미 포함 형태로 정제한 것까지는 맞다. 문제는 판정 **순서**를 함께 뒤집은 것이다.

```java
if (PAST_CONTEXT_MARKERS.stream().anyMatch(context::contains)) { ...continue; }
if (PRESENT_TENSE_MARKERS.stream().anyMatch(context::contains)
        || PAST_CONTEXT_MARKERS.stream().noneMatch(context::contains)) { return true; }
```

두 번째 `if`에 도달한 시점에는 이미 과거 마커가 없다고 확정돼 있다. 즉 `PAST.noneMatch`가 **항상 true**라 OR 전체가 무조건 참이고, `PRESENT_TENSE_MARKERS`는 결과에 아무 영향을 주지 못한다. 판정은 사실상 "과거 마커가 있나 없나" 하나로 축소됐다.

결과적으로 과거 단서 하나만 곁에 있으면 현재형 안부가 통과한다.

- `"그때 영희는 잘 계세요?"` → `"그때"` 때문에 통과
- `"예전 사진인데 영희는 지금 어디 계세요?"` → `"사진"` 때문에 통과

사별 인물에게 현재 안부를 묻는 문장은 이 규칙이 막으라고 존재하는 바로 그 케이스다.

### 1-2. DeviceToken.refresh가 어르신 기기 연결을 조용히 끊는다

`refresh(memberId, platform, elderId, now)`가 `elderId`를 무조건 대입한다. 그런데 3-인자 오버로드는 항상 `null`을 넘긴다.

```java
public void refresh(String memberId, DevicePlatform platform, LocalDateTime now) {
    refresh(memberId, platform, null, now);
}
```

앱이 FCM 토큰 갱신 콜백에서 `elderId` 없이 재등록하는 것만으로 어르신 기기 연결이 사라지고, 이후 `dispatchToElder`가 대상 토큰을 찾지 못한다. 실패가 조용해서(발송 0건 로그) 원인 추적도 어렵다.

## 2. 설계 판단

### 2-1. 우선순위: 현재형 > 모호성

마커가 이미 어미 포함 형태로 정제됐으므로, PR #82의 과차단 원인(초빈출 음절)은 순서와 무관하게 해소돼 있다. 따라서 안전하게 원래 의도대로 되돌린다.

1. 명시적 현재형 마커 → 과거 단서가 곁에 있어도 **차단**
2. 현재형은 아닌데 과거 단서도 없음 → 시제 모호 → **차단**
3. 과거 단서만 있음 → 통과

`"계세"`를 현재형 마커에 추가한다. 지금은 `"계시"`만 있어 `"계세요"`가 `"잘계세요"`/`"지금"` 같은 다른 마커에 우연히 걸릴 때만 잡힌다.

### 2-2. elderId는 소유자가 같을 때만 보존

명시적으로 넘어온 값만 반영하고, 없으면 기존 연결을 유지한다. 연결 해제는 토큰 해지 후 재등록 경로로 처리한다 — 조용히 끊기는 것보다 명시적 해제가 안전하다.

단, **소유자가 바뀐 재등록에서는 이전 연결을 버린다**. 무조건 보존하면 기기를 넘겨받은 사람에게 남의 어르신 알림이 계속 가는 반대편 결함이 생긴다. 보존은 "같은 사람이 같은 기기를 다시 등록한다"는 전제에서만 성립한다.

## 3. 변경 파일

| 파일 | 변경 |
| --- | --- |
| `m1/domain/model/reminiscence/ContentSafetyValidator.java` | 판정 순서 교정, `"계세"` 마커 추가 |
| `notification/domain/DeviceToken.java` | `refresh`에서 `elderId` null 보존 |
| `ContentSafetyValidatorTest.java` | 과거 단서 + 현재형 혼합 문장 회귀 4건 |
| `DeviceTokenTest.java` | elderId 생략 재등록 회귀 1건 |

## 4. 검증

- 기존 `clearPastPrompts` 20건이 그대로 통과해야 한다 (과차단 재발 방지 가드)
- `./gradlew test` 전체 green

## 5. Out-of-scope

- 스케줄러 전체 앨범 로딩 → #85
- `device_tokens.member_id` 타입 정합성 → #86
