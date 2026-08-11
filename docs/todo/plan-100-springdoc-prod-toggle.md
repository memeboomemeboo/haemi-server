# plan-100 · 운영 API 문서 노출 토글

이슈: #100 `chore: 운영에서 Swagger UI와 API 문서가 비인증 공개 상태 (결정 필요)`
브랜치: `chore/springdoc-prod-toggle/#100` (base: `refactor/domain-validation-exception/#99`, 스택 2번째)

## 결정

이슈의 선택지 2번 — **환경변수 토글**. 기본값은 지금처럼 켜 둔다.
클라이언트 개발이 진행 중이므로 동작을 바꾸지 않고, 정식 오픈 시점에 배포 설정만
바꿔서 끌 수 있는 스위치만 마련한다. 코드 재배포 없이 전환된다.

## 설계

`API_DOCS_ENABLED` 하나로 `api-docs`와 `swagger-ui`를 **함께** 켜고 끈다.

변수를 둘로 나누면 "UI만 껐는데 `/v3/api-docs`는 계속 전체 API 표면을 내려주는"
반쪽 상태가 생긴다. 실제 노출은 UI가 아니라 스펙 문서 쪽이므로 나눌 이유가 없다.

```yaml
springdoc:
  api-docs:
    enabled: ${API_DOCS_ENABLED:true}
  swagger-ui:
    enabled: ${API_DOCS_ENABLED:true}
```

### SecurityConfig의 permitAll은 건드리지 않는다

처음에는 `permitAll`도 같은 값에 묶어 "토글 하나로 완결"시키려 했다. 그런데
`ProductionSurfaceIntegrationTest.disabledOpenApiReturnsNotFoundInsteadOfServerError`가
문서를 껐을 때 `/v3/api-docs`가 **깔끔한 404 JSON**을 돌려주는 것을 이미 고정하고 있다.
매처를 빼면 `anyRequest().authenticated()`에 걸려 403이 된다.

이건 의도적으로 고정된 동작이고 #100의 범위 밖이라 되돌렸다.
springdoc을 끄면 핸들러 자체가 등록되지 않으므로, 매처가 남아 있어도 열리는 것은 없다.
결과적으로 이번 변경은 **설정 파일에만** 닿는다.

## 변경

- `application-prod.yaml` — `API_DOCS_ENABLED` 도입 (기본 true)
- `compose.yaml` · `.env.example` — 변수 노출 및 설명
- `ApiDocsToggleIntegrationTest` — 켰을 때/껐을 때 동작 고정

## 범위 밖

기기 토큰 테스트 발송 API는 이미 `@Profile("dev")`라 운영에 뜨지 않는다.
이번 작업은 문서 노출에 한정한다.
