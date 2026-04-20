# Platform 사용 기준

user-service는 `platform-governance`, `platform-security`, `platform-security-governance-bridge`를 감사 기록과 운영 governance/security 경계로 사용합니다.

인증 토큰 발급, 세션, credential 정책의 소유자는 auth-service입니다.

버전:

| Platform                              | Version  |
|---------------------------------------|----------|
| `platform-governance`                 | `2.0.1`  |
| `platform-security`                   | `2.0.3`  |
| `platform-security-governance-bridge` | `1.0.1`  |

## Governance

user-service는 `platform-governance`를 감사 기록과 governance policy 경계로 사용합니다.

도메인 판단과 이벤트 생성 시점은 user-service가 소유합니다.

```gradle
implementation platform(libs.platform.governance.bom)
implementation libs.platform.governance.starter
implementation platform(libs.platform.security.bom)
implementation libs.platform.security.starter
implementation libs.platform.security.governance.bridge
```

user-service 도메인 코드는 governance 1계층 구현 타입을 직접 소비하지 않습니다.

| 용도 | 타입 |
| --- | --- |
| 감사 이벤트 기록 | `io.github.jho951.platform.governance.api.AuditLogRecorder` |
| 감사 이벤트 envelope | `io.github.jho951.platform.governance.api.AuditEntry` |

Governance 구현 규칙:

- `UserAuditLogService`가 감사 이벤트 변환과 기록 경계를 담당합니다.
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
      strict: false
    violation:
      action: DENY
      handler-failure-fatal: false
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
| `PLATFORM_GOVERNANCE_ENGINE_STRICT` | `false` | strict evaluation |
| `PLATFORM_GOVERNANCE_VIOLATION_ACTION` | `DENY` | policy violation 처리 방식 |
| `PLATFORM_GOVERNANCE_VIOLATION_HANDLER_FAILURE_FATAL` | `false` | violation handler 실패를 fatal로 볼지 여부 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_FAIL_FAST_ENABLED` | `true` | 운영 정책 위반 시 기동 실패 여부 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_PRODUCTION_PROFILES` | `prod` | 운영 profile 목록 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_ALLOW_NON_STRICT_ENGINE_IN_PRODUCTION` | `false` | 운영 strict engine 예외 허용 여부 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_REQUIRE_POLICY_CONFIG_IN_ENFORCING_MODE` | `false` | enforcing mode에서 policy config 요구 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_REQUIRE_FATAL_HANDLER_FAILURES_IN_PRODUCTION` | `true` | 운영에서 handler failure fatal 요구 |

## Security 설정

user-service의 Spring Security 체인 조립은 `platform-security-starter`가 담당합니다.
user-service는 JWT decoder/converter와 서비스별 boundary 설정만 제공합니다.

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
      enabled: false
    rate-limit:
      enabled: false
```

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `PLATFORM_SECURITY_ENABLED` | `true` | platform-security auto-configuration 활성화 |
| `PLATFORM_SECURITY_SERVICE_ROLE_PRESET` | `API_SERVER` | API 서버용 보안 preset |
| `PLATFORM_SECURITY_AUTH_ENABLED` | `true` | platform-security starter 체인의 인증 정책 활성화 |
| `PLATFORM_SECURITY_INTERNAL_TOKEN_ENABLED` | `true` | internal boundary token 정책 활성화 |
| `PLATFORM_SECURITY_GATEWAY_HEADER_ENABLED` | `true` | gateway가 검증한 사용자 식별자 헤더 인증 활성화 |
| `PLATFORM_SECURITY_IP_GUARD_ENABLED` | `false` | user-service 기존 네트워크/접근 제어 흐름 사용 |
| `PLATFORM_SECURITY_RATE_LIMIT_ENABLED` | `false` | user-service 기존 traffic 제어 흐름 사용 |

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
