# Admin Backend v2

운영자(Admin) API 백엔드입니다.  
`v2`에서는 멀티모듈(`api`, `core`) 구조와 인증 모듈(`auth-config`) 연동이 포함됩니다.

## Modules

- `api`: Spring Boot 실행 모듈, 인증/사용자 관리 API
- `core`: 공통 도메인/예외/DTO

## Tech Stack

- Java 17
- Spring Boot 3.3.5
- Spring Data JPA, Spring Security, Validation
- H2 (dev)
- Gradle 9.x

## Prerequisites

- JDK 17
- GitHub Packages 접근 가능한 PAT (`read:packages`)

## Quick Start

1. GitHub Packages 인증 정보 설정

```bash
export GITHUB_ACTOR=<github-username>
export GITHUB_TOKEN=<github-pat>
```

또는 `~/.gradle/gradle.properties`:

```properties
gprUser=<github-username>
gprKey=<github-pat>
```

2. 빌드/실행

```bash
./gradlew clean :api:bootRun
```

기본 프로필은 `dev`이며, 서버는 `http://localhost:8080`으로 실행됩니다.

## Authentication

`io.github.jho951:auth-config:1.0.8` 모듈이 아래 엔드포인트를 제공합니다.

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

상세 내용: `docs/AUTH_LOGIN_GUIDE.md`

## Admin APIs

- `GET /api/admin/users`
- `POST /api/admin/users`
- `PUT /api/admin/users/{id}`
- `DELETE /api/admin/users/{id}`

## Local Default Admin

앱 시작 시 기본 SUPER_ADMIN 계정이 자동 생성됩니다.

- username: `superadmin`
- password: `superadmin1234`

구현: `api/src/main/java/com/api/config/SuperAdminInitializer.java`

## Secret Management

- 저장소의 `gradle.properties`에는 실제 키를 넣지 않습니다.
- 실제 값은 환경변수 또는 `~/.gradle/gradle.properties`를 사용합니다.
- 토큰/웹훅/JWT 시크릿이 노출되면 즉시 폐기 후 재발급하세요.

## Troubleshooting

- `401 Unauthorized`로 `auth-config` 다운로드 실패 시: `docs/GITHUB_PACKAGES_TROUBLESHOOTING.md`
