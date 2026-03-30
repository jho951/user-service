# User Service

>사용자 도메인의 기준 시스템으로 동작하는 멀티모듈 백엔드입니다.

## Contract Source

- 공통 계약 레포: `https://github.com/jho951/contract`
- 이 서비스의 코드 SoT: `User-server` `main`
- 인터페이스 변경 시 본 저장소 구현보다 계약 레포 변경을 먼저 반영합니다.

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
export USER_SERVICE_BASE_URL=http://localhost:8082
export USER_SERVICE_INTERNAL_JWT_ISSUER=auth-service
export USER_SERVICE_INTERNAL_JWT_AUDIENCE=user-service
export USER_SERVICE_INTERNAL_JWT_SECRET=<shared-hmac-secret>
# optional
export USER_SERVICE_INTERNAL_JWT_SCOPE=internal
```

로컬 실행 시 MySQL이 먼저 떠 있어야 합니다.

`RemoteUserDirectory`를 사용하는 `auth-server` 연동을 위해 아래 값은 반드시 채워야 합니다.

- `USER_SERVICE_BASE_URL`: auth-server가 호출할 user-service 주소
- `USER_SERVICE_INTERNAL_JWT_ISSUER`: 내부 JWT `iss`
- `USER_SERVICE_INTERNAL_JWT_AUDIENCE`: 내부 JWT `aud`
- `USER_SERVICE_INTERNAL_JWT_SECRET`: 내부 JWT HMAC 비밀키
- `USER_SERVICE_INTERNAL_JWT_SCOPE`(선택): 내부 호출 scope (기본값 `internal`)

호환성을 위해 기존 `AUTH_JWT_*` 환경변수도 계속 읽지만, 서비스 간 설정 일치를 위해 `USER_SERVICE_INTERNAL_JWT_*` 사용을 권장합니다.

2. 빌드/실행

```bash
./scripts/run.local.sh
```

또는 Gradle로 직접 실행하려면:

```bash
./gradlew clean :app:bootRun
```

기본 프로필은 `dev`이며, 서버는 `http://localhost:8082`으로 실행됩니다.

Docker로 실행하려면 아래 스크립트를 사용합니다.

```bash
# optional: 공유 MSA 네트워크 이름 커스터마이징
# export MSA_SHARED_NETWORK=msa-shared

./scripts/run.docker.sh dev
./scripts/run.docker.sh prod
```

Docker 실행 시 각 환경별 compose가 `mysql` 컨테이너와 `user-server`를 함께 기동합니다.

- 개발: `docker/docker-compose.dev.yml`
- 운영: `docker/docker-compose.prod.yml`

네트워크 구성:

- `msa-shared`(external): gateway/auth/user-service 간 통신용 공유 네트워크
- `user-private`(internal): user-service와 user-service DB 전용 내부 네트워크

MySQL 설정은 compose 파일에 직접 넣지 않고 아래 `cnf` 디렉터리로 분리되어 있습니다.

- 개발: `docker/mysql/dev/conf.d/my.cnf`
- 운영: `docker/mysql/prod/conf.d/my.cnf`

## User APIs

분리된 `user-service` 성격의 API가 추가되어 있습니다.

기본 기능 플래그:

- `features.public-user-api.enabled=false`
- `features.internal-user-api.enabled=true`

- `GET /users/me`
- `POST /users/signup`
- `POST /internal/users`
- `POST /internal/users/find-or-create-and-link-social`
- `PUT /internal/users/{userId}/status`
- `GET /internal/users/{userId}`
- `GET /internal/users/by-email?email=...`

`POST /internal/users/find-or-create-and-link-social`는 소셜 링크 매핑의 원본 데이터를 `user-service`가 소유합니다.

- 소셜 링크 원본 필드: `provider`, `providerUserId`, `email`, `userId`
- 요청 본문은 `provider`/`providerUserId`를 권장하며, 하위 호환으로 `socialType`/`providerId`도 허용합니다.
- 소셜 링크 생성/조회의 단일 진입점: `POST /internal/users/find-or-create-and-link-social`
- `POST /internal/users/social`, `POST /internal/users/ensure-social`, `GET /internal/users/by-social`는 비활성 정책이며 `400`을 반환합니다.

필수 관측 지표:

- `social_link_requests_total`
- `social_link_conflicts_total`
- `social_link_create_total`
- `social_link_existing_total`
- `social_link_latency_ms`

구조화 로그 필드(키-값 로그):

- `provider`
- `providerUserId` (SHA-256 해시 앞 16자)
- `email`
- `userId`
- `result` (`created|existing|error`)
- `errorCode`

운영 반영 순서:

1. user-service 대시보드/알림 지표를 위 `social_link_*` 기준으로 전환 완료 확인
2. 전환 완료 후 auth-service의 `auth_social_accounts` drop 적용

user-service 마이그레이션(이 저장소에서 수행):

```bash
export MYSQL_HOST='<prod-user-service-db-endpoint>'
export MYSQL_PORT='3306'
export MYSQL_DB='user_service'
export MYSQL_USER='<user_service_db_user>'
export MYSQL_PASSWORD='<secret>'

# apply
./scripts/migrations/user-service/run_social_link_email_migration.sh apply

# rollback
./scripts/migrations/user-service/run_social_link_email_migration.sh rollback
```

Docker 로그 확인:

```bash
# user-service 애플리케이션 로그 (gateway 경유 접근 + SQL 포함)
docker logs -f user-server-dev

# MySQL 로그
docker logs -f user-server-mysql-dev
```

로그 성격:

- gateway 경유 접근 로그: `http_access method=... path=... status=... durationMs=...`
- DB 저장 SQL 로그(dev): `org.hibernate.SQL`, `org.hibernate.orm.jdbc.bind`

운영에서 SQL 로그를 임시 활성화하려면:

```bash
export LOGGING_LEVEL_ORG_HIBERNATE_SQL=DEBUG
export LOGGING_LEVEL_ORG_HIBERNATE_BIND=TRACE
./scripts/run.docker.sh prod
```

Gateway 버저닝 정책:

- 외부(클라이언트) 계약은 gateway에서 `/v1/...` 로 노출합니다.
- 내부(서비스 간) 계약은 gateway rewrite로 `/v1`을 제거해 user-service에 전달합니다.
- 따라서 user-service는 버전 prefix 없는 경로(`/users/**`, `/internal/users/**`)만 운영합니다.

주의:

- 공개 API인 `/users/**` 는 기본 설정에서는 비활성화되어 있습니다.
- 내부 API인 `/internal/users/**` 는 기본 설정에서 활성화되어 있습니다.

일반 회원 데이터는 `users`, `user_social_accounts` 테이블에 저장됩니다.
비밀번호 저장 책임은 이 서버가 아닌 `auth-service`로 분리하는 것을 전제로 합니다.

## Security

`user-service`는 경로별로 인증 방식을 분리합니다.

- 공개 API: `POST /users/signup` (`features.public-user-api.enabled=true` 일 때만 노출)
- 보호 API: `GET /users/me` (`features.public-user-api.enabled=true` 일 때만 노출)
- 내부 API: `/internal/users/**`

기본 개발 설정(권장):

- `USER_SERVICE_INTERNAL_JWT_ISSUER=auth-service`
- `USER_SERVICE_INTERNAL_JWT_SECRET=<shared-hmac-secret>`
- `USER_SERVICE_INTERNAL_JWT_AUDIENCE=user-service`
- `USER_SERVICE_INTERNAL_JWT_SCOPE=internal`

호환 별칭:

- `AUTH_JWT_ISSUER`
- `AUTH_JWT_SECRET`
- `AUTH_JWT_AUDIENCE`
- `AUTH_INTERNAL_SCOPE`

현재 구현은 HMAC 서명 JWT를 기준으로 동작합니다. 운영 환경에서는 `auth-service`의 실제 서명 정책에 맞춰 키 관리나 JWK 기반 검증으로 전환하는 것이 바람직합니다.

추가 정책:

- `/internal/**`:
  - `auth-service` 내부 Bearer JWT만 허용
  - `iss`, `aud`, `sub`를 검증
  - `scope` 또는 `scp`에 `internal`이 포함되어야 허용
- `/users/**`:
  - gateway 사용자 컨텍스트 헤더(`X-User-Id`, `X-User-Status`) 또는 Bearer 사용자 토큰 기준으로 인증 처리
  - `/users/me`는 `status=A`일 때만 허용

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
