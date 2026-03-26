# User Service

>사용자 도메인의 기준 시스템으로 동작하는 멀티모듈 백엔드입니다.

## Modules

- `app`: Spring Boot 실행 모듈, 일반 회원/내부 연동 API
- `domain`: 공통 도메인/상수/예외/DTO

## Tech Stack

- Java 17
- Spring Boot 3.3.5
- Spring Data JPA, Spring Security, Validation
- MySQL 8.x
- Gradle 9.x

## Prerequisites

- JDK 17

## Quick Start

1. 환경 변수 설정

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/user_service?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true
export SPRING_DATASOURCE_USERNAME=user_service
export SPRING_DATASOURCE_PASSWORD=user_service
```

로컬 실행 시 MySQL이 먼저 떠 있어야 합니다.

2. 빌드/실행

```bash
./scripts/run.local.sh
```

기본 프로필은 `dev`이며, 서버는 `http://localhost:8082`으로 실행됩니다.

Docker로 실행하려면 아래 스크립트를 사용합니다.

```bash
./scripts/run.docker.sh dev
./scripts/run.docker.sh prod
```

Docker 실행 시 각 환경별 compose가 `mysql` 컨테이너와 `user-server`를 함께 기동합니다.

- 개발: `docker/docker-compose.dev.yml`
- 운영: `docker/docker-compose.prod.yml`

MySQL 설정은 compose 파일에 직접 넣지 않고 아래 `cnf` 디렉터리로 분리되어 있습니다.

- 개발: `docker/mysql/dev/conf.d/my.cnf`
- 운영: `docker/mysql/prod/conf.d/my.cnf`

## User APIs

분리된 `user-service` 성격의 API가 추가되어 있습니다.

기본 기능 플래그:

- `features.public-user-api.enabled=false`
- `features.internal-user-api.enabled=true`

- `GET /api/users/me`
- `POST /api/users/signup`
- `POST /internal/users`
- `POST /internal/users/social`
- `PUT /internal/users/{userId}/status`
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

## Block Server Alignment

현재 구조 기준으로 블록 서버 연동 시 사용자 식별 계약은 아래와 같습니다.

- 표준 사용자 식별자: `userId`
- 값 형식: UUID 문자열
- JWT 사용자 식별자: `sub == userId`
- 블록 서버 저장 기준: `createdBy`, `updatedBy` 에는 표시명이 아니라 위 UUID 식별자를 저장

현재 `GET /internal/users/{userId}` 응답으로 확인 가능한 기본 정보:

- `id`
- `email`
- `role`
- `status`

현재 상태 정의:

- `ACTIVE`
- `PENDING`
- `SUSPENDED`
- `DELETED`

현재 구조에는 `name`, `displayName` 필드는 없습니다.

## Secret Management

- 저장소의 `gradle.properties`에는 실제 키를 넣지 않습니다.
- 실제 값은 환경변수 또는 `~/.gradle/gradle.properties`를 사용합니다.
- 토큰/웹훅/JWT 시크릿이 노출되면 즉시 폐기 후 재발급하세요.
