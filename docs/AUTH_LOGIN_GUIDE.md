# Auth 로그인 연동 가이드

이 프로젝트는 `io.github.jho951:auth-config` 모듈로 로그인 기능을 제공합니다.

사전 준비(패키지 인증) 이슈는 `docs/GITHUB_PACKAGES_TROUBLESHOOTING.md`를 참고하세요.

## 1) 현재 구조

- 로그인 API는 프로젝트 코드가 아니라 `auth-config` 모듈이 자동 등록합니다.
- 사용자 조회는 `AdminUserFinder`가 담당합니다.
- 비밀번호 검증은 `PasswordEncoder`(BCrypt) 기반으로 동작합니다.

관련 파일:

- `api/build.gradle`
- `settings.gradle`
- `api/src/main/java/com/api/config/AdminUserFinder.java`
- `api/src/main/java/com/api/config/PasswordConfig.java`
- `api/src/main/resources/application-dev.yml`

## 2) 제공 엔드포인트

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

`/auth/**`는 인증 없이 접근 가능하고, 그 외 API는 인증 토큰이 필요합니다.

## 3) 로컬 테스트 계정

애플리케이션 시작 시 기본 SUPER_ADMIN 계정이 자동 생성됩니다.

- username: `superadmin`
- password: `superadmin1234`

생성 로직: `api/src/main/java/com/api/config/SuperAdminInitializer.java`

## 4) 로그인/호출 예시

### 로그인

```bash
curl -i -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"superadmin1234"}'
```

응답:

- 본문: `accessToken`
- 쿠키: `ADMIN_REFRESH_TOKEN` (refresh token)

### 보호 API 호출

```bash
curl http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

### 액세스 토큰 재발급

```bash
curl -i -X POST http://localhost:8080/auth/refresh \
  --cookie "ADMIN_REFRESH_TOKEN=<REFRESH_TOKEN>"
```

### 로그아웃

```bash
curl -i -X POST http://localhost:8080/auth/logout \
  --cookie "ADMIN_REFRESH_TOKEN=<REFRESH_TOKEN>"
```

## 5) 주요 설정 값

`api/src/main/resources/application-dev.yml`

- `auth.endpoints-enabled`: auth 엔드포인트 사용 여부
- `auth.auto-security`: 기본 SecurityFilterChain 자동 적용 여부
- `auth.bearer-prefix`: Authorization 헤더 prefix
- `auth.refresh-cookie-enabled`: refresh cookie 사용 여부
- `auth.refresh-cookie-name`: refresh cookie 이름
- `auth.refresh-cookie-secure`: 로컬 HTTP 테스트에서는 `false` 권장
- `auth.jwt.secret`: JWT 서명 키 (운영은 환경변수 주입 필수)
- `auth.jwt.access-seconds`: access token 만료(초)
- `auth.jwt.refresh-seconds`: refresh token 만료(초)
