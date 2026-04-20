# Auth API

## 한눈에 보기

```txt
Client
  -> Gateway public route: /v1/users/*
  -> user-service upstream: /users/*

Internal service
  -> user-service upstream: /internal/users/*
```

user-service는 public API version prefix를 소유하지 않습니다.

Gateway는 public `/v1/...` route를 user-service upstream route로 전달하기 전에 `/v1` prefix를 제거합니다.

| 구분 | 경로 | 소유자 | 설명 |
| --- | --- | --- | --- |
| Public user route | `/v1/users/*` | Gateway | 외부 client가 호출하는 versioned route |
| user-service upstream route | `/users/*` | user-service | Gateway가 전달하는 사용자 route |
| Internal user route | `/internal/users/*` | user-service | auth-service 등 내부 서비스가 호출하는 route |
| Runtime route | `/actuator/health` | user-service | 상태 확인 |
| Metrics route | `/actuator/prometheus` | user-service | Prometheus metric |

## Endpoint

상세 schema는 [OpenAPI](./openapi/user-service.yml)를 기준으로 합니다.

| Area | Method | Upstream path | Public path example | Response | Notes |
| --- | --- | --- | --- | --- | --- |
| Public | `POST` | `/users/signup` | `/v1/users/signup` | `201 GlobalResponse<UserCreateResponse>` | 이메일 기반 사용자 생성 |
| Public | `GET` | `/users/me` | `/v1/users/me` | `200 GlobalResponse<UserDetailResponse>` | 인증 사용자 정보 조회 |
| Internal | `POST` | `/internal/users` | internal | `201 GlobalResponse<UserDetailResponse>` | 내부 사용자 생성 |
| Internal | `POST` | `/internal/users/social` | internal | `201 GlobalResponse<UserSocialResponse>` | 소셜 계정 연결 생성 |
| Internal | `POST` | `/internal/users/ensure-social` | internal | `200 GlobalResponse<UserDetailResponse>` | 소셜 사용자 보장 호환 API |
| Internal | `POST` | `/internal/users/find-or-create-and-link-social` | internal | `200 GlobalResponse<UserDetailResponse>` | 표준 소셜 사용자 조회/생성/연결 |
| Internal | `PUT` | `/internal/users/{userId}/status` | internal | `200 GlobalResponse<UserDetailResponse>` | 사용자 상태 변경 |
| Internal | `GET` | `/internal/users/{userId}` | internal | `200 GlobalResponse<UserDetailResponse>` | 사용자 id 조회 |
| Internal | `GET` | `/internal/users/by-email` | internal | `200 GlobalResponse<UserDetailResponse>` | 이메일 조회 |
| Internal | `GET` | `/internal/users/by-social` | internal | `200 GlobalResponse<UserDetailResponse>` | 소셜 provider key 조회 |


## Common

### GlobalResponse

```json
{
  "httpStatus": 200,
  "success": true,
  "message": "요청 성공",
  "code": 3000,
  "data": {}
}
```

### UserDetailResponse

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "version": 0,
  "email": "user@example.com",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2026-04-01T12:00:00",
  "modifiedAt": "2026-04-01T12:00:00",
  "userSocialList": []
}
```

### UserSocialResponse

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "version": 0,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "provider": "GITHUB",
  "providerUserId": "github-user-123",
  "email": "user@example.com",
  "socialType": "GITHUB",
  "providerId": "github-user-123"
}
```

## Public APIs

### `POST /users/signup`

이메일 기반 사용자를 생성합니다.

| 항목 | 값 |
| --- | --- |
| Upstream | `POST /users/signup` |
| Public example | `POST /v1/users/signup` |
| Request body | `email` |
| Success | `201 GlobalResponse<UserCreateResponse>` |
| Audit | 사용자 가입 |

Request:

```json
{
  "email": "user@example.com"
}
```

Rules:

- `email`은 유효한 이메일 형식이어야 합니다.
- 이메일은 중복될 수 없습니다.
- 생성된 사용자는 UUID 문자열 id를 가져야 합니다.
- 비밀번호는 저장하지 않습니다.
- 가입 이벤트는 감사 로그에 기록합니다.

### `GET /users/me`

인증된 사용자의 기본 정보를 반환합니다.

| 항목 | 값 |
| --- | --- |
| Upstream | `GET /users/me` |
| Public example | `GET /v1/users/me` |
| Credential | Bearer JWT 또는 gateway가 검증한 사용자 식별자 |
| Success | `200 GlobalResponse<UserDetailResponse>` |

Rules:

- 인증된 사용자 식별자가 필요합니다. JWT를 사용할 때는 `sub == userId`를 원칙으로 합니다.
- user-service는 인증된 `userId`로 DB에서 사용자와 상태를 조회합니다.
- DB의 사용자 상태가 `ACTIVE`가 아니면 user-service가 직접 거부합니다.
- gateway가 전달하는 `X-User-Status` 또는 JWT `status` claim은 `/users/me` 권한 판단에 사용하지 않습니다.

## Internal APIs

### Internal JWT

`/internal/users/**`는 auth-service가 발급한 내부 JWT만 허용합니다.

필수 claim:

- `iss`: `USER_SERVICE_INTERNAL_JWT_ISSUER`와 일치
- `aud`: `USER_SERVICE_INTERNAL_JWT_AUDIENCE` 포함
- `sub`: 비어 있지 않은 사용자 또는 내부 주체 식별자
- `scope` 또는 `scp`: `internal` 포함

신규 설정은 `USER_SERVICE_INTERNAL_JWT_*`를 사용합니다.
`AUTH_JWT_*`는 하위 호환 목적으로만 유지합니다.

필수 설정:

- `USER_SERVICE_INTERNAL_JWT_ISSUER`
- `USER_SERVICE_INTERNAL_JWT_AUDIENCE`
- `USER_SERVICE_INTERNAL_JWT_SECRET`
- `USER_SERVICE_INTERNAL_JWT_SCOPE`

### `POST /internal/users`

내부 서비스 요청으로 사용자를 생성합니다.

Request:

```json
{
  "email": "user@example.com",
  "role": "USER",
  "status": "ACTIVE"
}
```

Rules:

- 내부 JWT가 필요합니다.
- 이메일, role, status를 입력받습니다.
- 이메일 중복을 허용하지 않습니다.
- 내부 생성 이벤트는 감사 로그에 기록합니다.

### `PUT /internal/users/{userId}/status`

사용자 상태를 변경합니다.

Request:

```json
{
  "status": "ACTIVE"
}
```

Rules:

- 내부 JWT가 필요합니다.
- `userId`는 UUID 문자열이어야 합니다.
- 변경 전 상태와 변경 후 상태를 감사 로그에 기록합니다.
- 지원하지 않는 상태 값은 거부합니다.

### 조회 API

| API | 기준 | 실패 |
| --- | --- | --- |
| `GET /internal/users/{userId}` | UUID 문자열 user id | 존재하지 않으면 표준 not found |
| `GET /internal/users/by-email?email=...` | 이메일 | 존재하지 않으면 표준 not found |
| `GET /internal/users/by-social?socialType=...&providerId=...` | provider key | 존재하지 않으면 표준 not found |

## Social Link APIs

### 표준 진입점

`POST /internal/users/find-or-create-and-link-social`은 신규 소셜 링크 연동의 표준 진입점입니다.

Rules:

- 내부 JWT가 필요합니다.
- provider key 기준으로 기존 링크를 먼저 찾습니다.
- 기존 링크가 있으면 같은 소유자를 반환합니다.
- 기존 링크가 없으면 사용자 생성 또는 기존 사용자 조회 후 소셜 링크를 생성합니다.

Request:

```json
{
  "email": "user@example.com",
  "socialType": "GITHUB",
  "providerId": "github-user-123",
  "role": "USER",
  "status": "ACTIVE"
}
```

### 호환 API

다음 API는 기존 auth-service 연동 호환을 위해 유지합니다.

- `POST /internal/users/social`
- `POST /internal/users/ensure-social`
- `GET /internal/users/by-social`

신규 연동은 `find-or-create-and-link-social`을 우선 사용합니다.

### Provider Key

소셜 링크의 provider key는 다음 값으로 구성합니다.

- `social_type`
- `provider_id`

요청 호환 필드:

- `provider` 또는 `socialType`
- `providerUserId` 또는 `providerId`

### Idempotency

- 같은 provider key에 대한 반복 요청은 중복 row를 만들면 안 됩니다.
- 같은 provider key가 같은 사용자에게 연결되어 있으면 기존 결과를 반환합니다.
- 같은 provider key가 다른 사용자에게 연결되어 있으면 충돌로 처리합니다.

## 상태와 Role

API request와 response의 상태 값은 enum 이름만 사용합니다.

| JSON/Enum | DB code | 설명 |
| --- | --- | --- |
| `ACTIVE` | `A` | 활성 |
| `PENDING` | `P` | 대기 |
| `SUSPENDED` | `S` | 정지 |
| `DELETED` | `D` | 삭제 |

DB에는 enum 이름을 그대로 저장하지 않고 DB code로 저장합니다.

탈퇴 복구, 재가입 제한, 개인정보 파기 배치 요구사항이 확정되면 `deletedAt` 응답 필드와 `users.deleted_at` 컬럼을 추가할 예정입니다.

Role은 다음 값을 사용합니다.

- `SUPER_ADMIN`
- `ADMIN`
- `USER`
- `GUEST`

Spring Security authority 문자열인 `ROLE_SUPER_ADMIN`, `ROLE_ADMIN`, `ROLE_USER`, `ROLE_GUEST`도 내부 변환에서 허용합니다.

## Gateway 사용자 컨텍스트

- gateway가 검증 후 전달하는 `X-User-Id`는 인증된 사용자 식별자로 사용할 수 있습니다.
- gateway는 외부 요청의 사용자 컨텍스트 헤더를 제거하고 검증된 값으로 재생성해야 합니다.
- `X-User-Status`는 user-service 권한 판단에 사용하지 않습니다. 필요하면 로그나 디버깅 참고값으로만 취급합니다.
- 사용자 active 상태 검사는 user-service가 DB 상태를 기준으로 직접 수행합니다.
