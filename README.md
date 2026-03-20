# User Service

>사용자 도메인의 기준 시스템으로 동작하는 멀티모듈 백엔드입니다.

## Modules

- `app`: Spring Boot 실행 모듈, 일반 회원/내부 연동 API
- `domain`: 공통 도메인/상수/예외/DTO

## Tech Stack

- Java 17
- Spring Boot 3.3.5
- Spring Data JPA, Spring Security, Validation
- H2 (dev)
- Gradle 9.x

## Prerequisites

- JDK 17

## Quick Start

1. 환경 변수 설정

```bash
export USER_SERVICE_BASE_URL=http://localhost:8082
```

`USER_SERVICE_BASE_URL`은 서비스 분리 환경에서 user-service의 기준 URL입니다.
기본값은 `http://localhost:8082`입니다.

2. 빌드/실행

```bash
./gradlew clean :app:bootRun
```

기본 프로필은 `dev`이며, 서버는 `http://localhost:8082`으로 실행됩니다.

## User APIs

분리된 `user-service` 성격의 API가 추가되어 있습니다.

기본 기능 플래그:

- `features.public-user-api.enabled=false`
- `features.internal-user-api.enabled=true`

- `GET /api/users/me`
- `POST /api/users/signup`
- `POST /internal/users`
- `POST /internal/users/social`
- `PATCH /internal/users/{userId}/status`
- `GET /internal/users/{userId}`
- `GET /internal/users/by-email?email=...`
- `GET /internal/users/by-social?socialType=GOOGLE&providerId=...`

주의:

- 공개 API인 `/api/users/**` 는 기본 설정에서는 비활성화되어 있습니다.
- 내부 API인 `/internal/users/**` 는 기본 설정에서 활성화되어 있습니다.

일반 회원 데이터는 `users`, `user_social_accounts` 테이블에 저장됩니다.
비밀번호 저장 책임은 이 서버가 아닌 `auth-service`로 분리하는 것을 전제로 합니다.

## Security

`user-service`는 `auth-service`가 발급한 JWT를 직접 검증합니다.

- 공개 API: `POST /api/users/signup` (`features.public-user-api.enabled=true` 일 때만 노출)
- 보호 API: `GET /api/users/me` (`features.public-user-api.enabled=true` 일 때만 노출)
- 내부 API: `/internal/users/**`

기본 개발 설정:

- `AUTH_JWT_ISSUER=auth-service`
- `AUTH_JWT_SECRET=<shared-hmac-secret>`
- `AUTH_JWT_AUDIENCE=user-service`
- `AUTH_INTERNAL_SCOPE=internal`

현재 구현은 HMAC 서명 JWT를 기준으로 동작합니다. 운영 환경에서는 `auth-service`의 실제 서명 정책에 맞춰 키 관리나 JWK 기반 검증으로 전환하는 것이 바람직합니다.

추가 정책:

- `/api/users/me`는 인증된 사용자 토큰이면서 `status=ACTIVE`일 때만 허용됩니다.
- `/internal/users/**`는 `scope` 또는 `scp` claim에 `internal`이 포함된 서비스 토큰만 허용됩니다.
- JWT 검증 시 `iss`, `aud`, `sub` 존재 여부를 함께 검사합니다.

## Secret Management

- 저장소의 `gradle.properties`에는 실제 키를 넣지 않습니다.
- 실제 값은 환경변수 또는 `~/.gradle/gradle.properties`를 사용합니다.
- 토큰/웹훅/JWT 시크릿이 노출되면 즉시 폐기 후 재발급하세요.
