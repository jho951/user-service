_# SSO 전체 구조 다이어그램

이 문서는 아래 3개 저장소를 기준으로 SSO 구조를 한 번에 볼 수 있도록 정리한다.

- `Api-gateway-server`: 외부 진입점, 라우팅, 공통 필터
- `BackEnd`: `auth-service`, 로그인 / OAuth2 / SSO / JWT 발급
- `User-server`: `user-service`, 사용자 도메인의 기준 시스템

## 1. 전체 아키텍처

```mermaid
flowchart LR
    Client[Web / Mobile Client]
    Gateway[Api-gateway-server]
    Auth[BackEnd<br/>Auth Service]
    User[User-server<br/>User Service]
    Redis[(Redis<br/>refresh token / SSO session)]
    AuthDB[(Auth DB<br/>account / credential / audit)]
    UserDB[(User DB<br/>user / profile / social)]
    OAuth[OAuth2 Provider<br/>Google / Kakao / Naver]

    Client --> Gateway
    Gateway --> Auth
    Gateway --> User

    Auth --> Redis
    Auth --> AuthDB
    Auth --> User
    User --> UserDB
    Auth --> OAuth
```

## 2. User-service의 역할과 책임

### 2.1 역할

`User-service`의 역할은 "사용자 도메인의 기준 시스템(System of Record)"이다.

즉, 사용자와 관련된 비즈니스 데이터의 최종 기준 저장소이며, 다른 서비스는 사용자 자체를 소유하지 않고 `User-service`를 참조한다.

### 2.2 책임

- 회원 생성과 기본 사용자 정보 관리
- 프로필, 상태, 역할에 대한 비즈니스 규칙 관리
- 소셜 계정 매핑 정보 관리
- `auth-service`가 필요로 하는 사용자 조회용 내부 API 제공
- 사용자 보호 API에서 access token 검증 후 사용자 문맥 구성

### 2.3 책임이 아닌 범위

아래는 `User-service`의 책임이 아니다.

- 로그인 처리
- 비밀번호 검증
- JWT access token 발급
- refresh token 발급, rotation, 폐기
- OAuth2 provider 인증 처리
- SSO 세션 또는 ticket 발급

위 책임은 모두 `Auth-service`에 둔다.

## 3. 역할 기반 책임 분리

```mermaid
flowchart TB
    subgraph GatewayLayer[Gateway]
        G1[Route by path]
        G2[Forward Authorization header]
        G3[Optional JWT pre-check]
        G4[Common error / logging / rate limit]
    end

    subgraph AuthLayer[BackEnd - Auth Service]
        A1[Login]
        A2[OAuth2 / SSO]
        A3[Access token issuance]
        A4[Refresh token rotation]
        A5[Logout]
        A6[SSO session / ticket]
        A7[Internal auth account API]
    end

    subgraph UserLayer[User-server - User Service]
        U0[Role: System of Record for User Domain]
        U1[User signup]
        U2[Profile management]
        U3[Role / status policy]
        U4[Social account mapping]
        U5[Internal user lookup API]
        U6[JWT validation for protected APIs]
        U7[Not responsible for login or token issuance]
    end
```

## 4. 로그인 흐름

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as Api Gateway
    participant A as Auth Service
    participant U as User Service
    participant R as Redis
    participant AD as Auth DB

    C->>G: POST /auth/login
    G->>A: Forward request
    A->>AD: Validate credential
    AD-->>A: Account matched
    A->>U: GET /internal/users/{userId}
    U-->>A: role, status, email
    A->>R: Store refresh token metadata
    A-->>G: access token + refresh token
    G-->>C: Return tokens
```

## 5. 사용자 API 호출 흐름

`User-service`는 게이트웨이를 통과한 요청이라도 토큰을 직접 검증해야 한다.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as Api Gateway
    participant U as User Service

    C->>G: GET /users/me + Bearer access token
    G->>G: Optional JWT pre-check
    G->>U: Forward Authorization header
    U->>U: Validate JWT signature, exp, iss, aud
    U->>U: Read sub, role, status claims
    U-->>G: User response
    G-->>C: 200 OK
```

## 6. 회원가입 흐름

외부 회원가입 진입점은 `User-service`로 두고, 인증 계정 생성은 `Auth-service`의 내부 API로 위임한다.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as Api Gateway
    participant U as User Service
    participant UD as User DB
    participant A as Auth Service
    participant AD as Auth DB

    C->>G: POST /users/signup
    G->>U: Forward request
    U->>UD: Create user domain data
    UD-->>U: userId created
    U->>A: POST /internal/auth/accounts
    A->>AD: Create auth account
    AD-->>A: account created
    A-->>U: success
    U-->>G: signup success
    G-->>C: 201 Created
```

## 7. 회원가입 실패 보상 흐름

```mermaid
sequenceDiagram
    autonumber
    participant U as User Service
    participant UD as User DB
    participant A as Auth Service
    participant AD as Auth DB

    U->>UD: Create user
    UD-->>U: userId
    U->>A: Create auth account
    A->>AD: Save account
    AD-->>A: failure
    A-->>U: error
    U->>UD: Rollback or mark pending-failed
```

## 8. OAuth2 / SSO 로그인 흐름

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as Api Gateway
    participant A as Auth Service
    participant O as OAuth2 Provider
    participant U as User Service
    participant R as Redis

    C->>G: GET /auth/oauth2/authorize/{provider}
    G->>A: Forward request
    A->>R: Create SSO state / session / ticket
    A-->>C: Redirect to provider
    C->>O: Authenticate and consent
    O-->>A: Callback with code
    A->>O: Exchange code for user info
    O-->>A: Provider identity
    A->>U: GET /internal/users/by-social
    alt user exists
        U-->>A: existing userId, role, status
    else new user
        A->>U: POST /internal/users/social
        U-->>A: new userId, role, status
    end
    A->>R: Save refresh token / SSO session
    A-->>G: access token + refresh token
    G-->>C: Final login response
```

## 9. Refresh token 재발급 흐름

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as Api Gateway
    participant A as Auth Service
    participant R as Redis
    participant U as User Service

    C->>G: POST /auth/refresh
    G->>A: Forward refresh token
    A->>R: Validate refresh token and rotation state
    R-->>A: valid
    A->>U: GET /internal/users/{userId}
    U-->>A: current role, status
    A->>R: Rotate refresh token
    A-->>G: new access token + new refresh token
    G-->>C: reissued tokens
```

## 10. 로그아웃 / 세션 종료 흐름

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as Api Gateway
    participant A as Auth Service
    participant R as Redis

    C->>G: POST /auth/logout
    G->>A: Forward request
    A->>R: Revoke refresh token / session / ticket
    A-->>G: logout success
    G-->>C: 200 OK
```

## 11. 권장 JWT Claim

```mermaid
classDiagram
    class AccessTokenClaims {
        +sub: userId
        +iss: auth-service
        +aud: gateway or service
        +role: USER | ADMIN | SUPER_ADMIN
        +status: ACTIVE | SUSPENDED
        +sid: optional session id
        +exp: expiration
    }
```

## 12. 구현 원칙

- 토큰 발급은 `Auth-service`만 담당한다.
- `Gateway`는 토큰을 발급하지 않는다.
- `User-service`는 게이트웨이 뒤에 있어도 access token을 직접 검증한다.
- 서비스 간 DB 직접 접근은 금지한다.
- `refresh token`과 SSO 세션은 `Redis`로 관리한다.
- 사용자 상태와 역할 정책은 `User-service`가 소유한다.
- 로그인 시 필요한 최소 사용자 정보만 `User-service`에서 조회하거나 캐시로 동기화한다.
- `User-service`는 사용자 도메인의 기준 시스템이지만, 인증 상태의 기준 시스템은 아니다.

## 13. 서비스별 역할 / 책임 / 비책임

| 서비스 | 역할 | 책임 | 비책임 |
| --- | --- | --- | --- |
| `Api-gateway-server` | 외부 요청의 단일 진입점 | 경로 기반 라우팅, 공통 필터, `Authorization` 헤더 전달, 선택적 JWT 선검증, 공통 예외 처리, 로깅, rate limit | 로그인 처리, 사용자 정보 소유, JWT 발급, refresh token 관리 |
| `BackEnd` (`auth-service`) | 인증과 토큰 발급의 기준 시스템 | 로그인, 비밀번호 검증, OAuth2 / SSO, access token 발급, refresh token rotation, logout, SSO 세션 / ticket 관리, 내부 인증 계정 API 제공 | 프로필 관리, 사용자 도메인 정책 소유, 사용자 상세 정보 저장의 기준 시스템 |
| `User-server` (`user-service`) | 사용자 도메인의 기준 시스템 | 회원 생성, 프로필 관리, 역할 / 상태 정책 관리, 소셜 계정 매핑 관리, 내부 사용자 조회 API 제공, 보호 API에서 JWT 검증 | 로그인 처리, 비밀번호 검증, JWT 발급, refresh token 관리, OAuth2 provider 인증, SSO 세션 관리 |

## 14. 한 줄 정리

- `Gateway`는 "들어오는 요청을 올바른 서비스로 전달하는 계층"이다.
- `Auth-service`는 "누가 누구인지 증명하고 토큰을 발급하는 계층"이다.
- `User-service`는 "사용자 도메인의 기준 정보를 소유하고 관리하는 계층"이다._
