# Platform 사용 기준

user-service는 `platform-governance`, `platform-security`, `platform-security-governance-bridge`를 감사 기록과 운영 governance/security 경계로 사용합니다.

인증 토큰 발급, 세션, credential 정책의 소유자는 auth-service입니다.

`platform-resource`는 현재 user-service에서 사용하지 않습니다.
프로필 이미지 업로드처럼 owner 기반 resource lifecycle이 필요한 기능이 생길 때 도입합니다.

버전:

| Platform                              | Version  |
|---------------------------------------|----------|
| `platform-governance`                 | `3.0.0`  |
| `platform-security`                   | `3.0.0`  |
| `platform-security-governance-bridge` | `3.0.0`  |

## Resource 도입 기준

현재 user-service는 파일/리소스 저장 기능을 제공하지 않으므로 `platform-resource` 의존성을 추가하지 않습니다.

향후 프로필 이미지 업로드를 도입하면 `platform-resource`를 사용합니다.

도입 기준:

- 프로필 이미지를 stable `resourceId`로 관리합니다.
- `User` 엔티티에는 파일 경로, bucket key, storage id를 직접 저장하지 않고 `profileImageResourceId`만 저장합니다.
- resource owner는 사용자 id로 둡니다.
- 접근 정책은 owner 기반으로 시작하고, 관리자 조회가 필요하면 `OWNER_OR_ADMIN` 정책을 검토합니다.
- 삭제 정책은 사용자 탈퇴, 프로필 이미지 교체, 개인정보 파기 요구사항에 맞춰 `SOFT` 또는 `HARD`를 명시합니다.
- 저장/삭제 후 감사나 후처리가 필요하면 lifecycle event 또는 outbox 정책을 사용합니다.
- 이미지 MIME type, 최대 크기, lifecycle publish 여부는 `profile-image` kind policy로 선언합니다.

도입하지 않는 경우:

- 단순 기본 아바타 URL처럼 고정 값을 내려주는 경우
- 외부 OAuth provider의 avatar URL을 그대로 참조만 하는 경우
- classpath/static asset을 읽는 경우

예상 kind:

```yaml
platform:
  resource:
    kinds:
      profile-image:
        allowed-content-types: [image/png, image/jpeg, image/webp]
        max-size: 5MB
        access-mode: OWNER_ONLY
        delete-mode: SOFT
        notification-mode: OUTBOX
        lifecycle:
          publish-on-store: true
          publish-on-delete: true
```

## Governance

user-service는 `platform-governance`를 감사 기록과 governance policy 경계로 사용합니다.

도메인 판단과 이벤트 생성 시점은 user-service가 소유합니다.

```gradle
implementation platform(libs.platform.runtime.bom)
implementation platform(libs.platform.governance.bom)
implementation libs.platform.governance.starter
implementation platform(libs.platform.security.bom)
implementation libs.platform.security.starter
implementation libs.platform.security.ratelimit.bridge.starter
implementation libs.platform.security.governance.bridge
implementation "io.github.jho951:audit-log-api"
```

user-service 도메인 코드는 governance adapter/core/engine 구현 타입을 직접 소비하지 않습니다.
다만 감사 SPI인 `com.auditlog.api.*`는 `io.github.jho951:audit-log-api`를 명시적으로 추가한 뒤 사용합니다.

| 용도 | 타입 |
| --- | --- |
| 감사 이벤트 기록 | `com.auditlog.api.AuditLogger` |
| 감사 이벤트 sink 확장 | `com.auditlog.api.AuditSink` |
| 감사 이벤트 envelope | `com.auditlog.api.AuditEvent` |

Governance 구현 규칙:

- `UserAuditLogService`가 감사 이벤트 변환과 기록 경계를 담당합니다.
- `AuditSink`, `AuditLogger`, `AuditEvent` compile surface는 `platform-governance-starter`가 아니라 `audit-log-api`가 제공합니다.
- 사용자 가입, 내부 사용자 생성, 소셜 링크, 상태 변경 시점은 user-service 도메인 코드에서 명시합니다.
- `platform.governance.*` 설정은 `application.yml`과 환경변수에서 관리합니다.
- business decision을 governance 모듈에 숨기지 않습니다.

## Governance 설정

```yaml
platform:
  governance:
    enabled: true
    audit:
      enabled: true
      service-name: user-service
      environment: prod
      failure-policy: FAIL_CLOSED
      identity:
        enabled: true
        validation-enabled: true
        fail-on-validation-error: true
    engine:
      strict: true
    violation:
      action: DENY
      handler-failure-fatal: true
    operational:
      fail-fast-enabled: true
      production-profiles: prod
      allow-non-strict-engine-in-production: false
      require-policy-config-in-enforcing-mode: false
      require-fatal-handler-failures-in-production: true
```

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `PLATFORM_GOVERNANCE_ENABLED` | `true` | platform-governance auto-configuration 활성화 |
| `PLATFORM_GOVERNANCE_AUDIT_ENABLED` | `true` | audit recorder 활성화 |
| `PLATFORM_GOVERNANCE_AUDIT_SERVICE_NAME` | `user-service` | 감사 service name |
| `PLATFORM_GOVERNANCE_AUDIT_ENVIRONMENT` | active profile | 감사 environment |
| `PLATFORM_GOVERNANCE_AUDIT_FAILURE_POLICY` | `FAIL_CLOSED` | audit failure policy |
| `PLATFORM_GOVERNANCE_AUDIT_IDENTITY_ENABLED` | `true` | identity audit 활성화 |
| `PLATFORM_GOVERNANCE_AUDIT_IDENTITY_VALIDATION_ENABLED` | `true` | identity audit validation 활성화 |
| `PLATFORM_GOVERNANCE_AUDIT_IDENTITY_FAIL_ON_VALIDATION_ERROR` | `true` | identity audit validation 실패 처리 |
| `PLATFORM_GOVERNANCE_ENGINE_STRICT` | `true` | strict evaluation |
| `PLATFORM_GOVERNANCE_VIOLATION_ACTION` | `DENY` | policy violation 처리 방식 |
| `PLATFORM_GOVERNANCE_VIOLATION_HANDLER_FAILURE_FATAL` | `true` | violation handler 실패를 fatal로 볼지 여부 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_FAIL_FAST_ENABLED` | `true` | 운영 정책 위반 시 기동 실패 여부 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_PRODUCTION_PROFILES` | `prod` | 운영 profile 목록 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_ALLOW_NON_STRICT_ENGINE_IN_PRODUCTION` | `false` | 운영 strict engine 예외 허용 여부 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_REQUIRE_POLICY_CONFIG_IN_ENFORCING_MODE` | `false` | enforcing mode에서 policy config 요구 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_REQUIRE_FATAL_HANDLER_FAILURES_IN_PRODUCTION` | `true` | 운영에서 handler failure fatal 요구 |

## Security 설정

user-service의 Spring Security 체인 조립은 `platform-security-starter`가 담당합니다.
user-service는 JWT decoder/converter와 서비스별 boundary 설정만 제공합니다. raw rate-limit 연동은 `platform-security-ratelimit-bridge-starter`가 담당합니다.

```yaml
platform:
  security:
    enabled: true
    service-role-preset: API_SERVER
    boundary:
      public-paths:
        - /actuator/**
        - /v3/api-docs/**
        - /swagger-ui/**
        - /swagger-ui.html
        - /favicon.ico
        - /error
        - /users/signup
      protected-paths:
        - /users/**
      admin-paths: []
      internal-paths:
        - /internal/**
    auth:
      enabled: true
      internal-token-enabled: true
      jwt-secret: ${USER_SERVICE_INTERNAL_JWT_SECRET}
      jwt-issuer: auth-service
      jwt-audience: user-service
      internal-required-authorities:
        - SCOPE_internal
      gateway-header:
        enabled: true
    ip-guard:
      enabled: true
    rate-limit:
      enabled: true
```

`/error`는 서블릿 컨테이너가 에러 요청을 전달하는 runtime endpoint이며, user-service가 `ServletErrorResponse` JSON으로 반환합니다.

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `PLATFORM_SECURITY_ENABLED` | `true` | platform-security auto-configuration 활성화 |
| `PLATFORM_SECURITY_SERVICE_ROLE_PRESET` | `API_SERVER` | API 서버용 보안 preset |
| `PLATFORM_SECURITY_AUTH_ENABLED` | `true` | platform-security starter 체인의 인증 정책 활성화 |
| `PLATFORM_SECURITY_INTERNAL_TOKEN_ENABLED` | `true` | internal boundary token 정책 활성화 |
| `PLATFORM_SECURITY_GATEWAY_HEADER_ENABLED` | `true` | gateway가 검증한 사용자 식별자 헤더 인증 활성화 |
| `PLATFORM_SECURITY_IP_GUARD_ENABLED` | `true` | 운영에서는 admin/internal boundary를 강제합니다. |
| `PLATFORM_SECURITY_RATE_LIMIT_ENABLED` | `true` | 운영에서는 shared backing `RateLimiter`를 함께 제공합니다. |

## 감사 이벤트

다음 이벤트는 감사 로그로 기록해야 합니다.

- 사용자 가입
- 내부 사용자 생성
- 소셜 링크 생성
- 기존 소셜 링크 재사용
- 사용자 상태 변경

감사 로그는 가능한 경우 다음 속성을 포함해야 합니다.

- `userId`
- `email`
- `actor`
- `action`
- `beforeStatus`
- `afterStatus`
- `socialType`
- `providerId`

## 관측성

### 접근 로그

접근 로그는 최소 다음 값을 포함해야 합니다.

- `method`
- `path`
- `query`
- `status`
- `durationMs`
- `forwardedFor`
- `requestId`
- `correlationId`
- `gatewayUserId`

로그 형식:

```text
http_access method=... path=... query=... status=... durationMs=... forwardedFor=... requestId=... correlationId=... gatewayUserId=...
```

`email`, `providerId`, token 계열 query 값은 로그에서 마스킹되어야 합니다.

### 소셜 링크 metric

다음 metric을 유지해야 합니다.

- `social_link_requests_total`
- `social_link_conflicts_total`
- `social_link_create_total`
- `social_link_existing_total`
- `social_link_latency_ms`

소셜 링크 구조화 로그는 가능한 경우 다음 값을 포함합니다.

- `provider`
- `providerUserId`
- `email`
- `userId`
- `result`
- `errorCode`

### Actuator

- `/actuator/health`를 제공합니다.
- `/actuator/prometheus`를 제공합니다.

운영에서는 actuator endpoint 노출 범위를 네트워크 정책과 gateway 정책으로 제한합니다.
