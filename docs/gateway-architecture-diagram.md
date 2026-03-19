# Gateway 구조 다이어그램

이 문서는 현재 SSO 구조에서 `Api-gateway-server`가 가져야 할 역할, 책임, 비책임, 라우팅 흐름을 다이어그램 중심으로 정리한다.

기준 구조는 아래와 같다.

- `Api-gateway-server`: 외부 요청의 단일 진입점
- `BackEnd`: `auth-service`, 인증과 토큰 발급의 기준 시스템
- `User-server`: `user-service`, 사용자 도메인의 기준 시스템

## 1. Gateway의 역할

`Gateway`의 역할은 "외부 요청을 올바른 내부 서비스로 전달하는 진입 계층"이다.

즉, 클라이언트 요청을 경로에 따라 라우팅하고, 공통 필터를 적용하며, 인증 정보를 전달하는 경계 계층이다.

## 2. Gateway의 책임

- 경로 기반 라우팅
- 공통 필터 적용
- `Authorization` 헤더 전달
- 선택적 JWT 선검증
- 공통 예외 응답 처리
- 로깅, 추적 ID, rate limit
- CORS, 공통 헤더 정책
- 서비스별 prefix 정리와 외부 URL 일관성 유지

## 3. Gateway의 비책임

아래는 `Gateway`가 직접 소유하거나 수행하면 안 되는 일이다.

- 로그인 처리
- 비밀번호 검증
- JWT 발급
- refresh token 발급, rotation, 폐기
- OAuth2 provider 인증 처리
- SSO 세션 기준 저장
- 사용자 도메인 정보 저장
- 사용자 상태나 역할의 기준 판단

위 책임은 각각 `auth-service`와 `user-service`에 둔다.

## 4. 전체 위치

```mermaid
flowchart LR
    Client[Web / Mobile Client]
    Gateway[Api-gateway-server]
    Auth[BackEnd<br/>Auth Service]
    User[User-server<br/>User Service]

    Client --> Gateway
    Gateway --> Auth
    Gateway --> User
```

## 5. 책임 경계 다이어그램

```mermaid
flowchart TB
    subgraph GatewayLayer[Api Gateway]
        G1[Single entrypoint]
        G2[Route by path]
        G3[Forward Authorization header]
        G4[Optional JWT pre-check]
        G5[CORS / common headers]
        G6[Rate limit / logging / trace id]
        G7[Common error response]
        G8[Not responsible for token issuance]
    end

    subgraph AuthLayer[Auth Service]
        A1[Login]
        A2[OAuth2 / SSO]
        A3[Token issuance]
        A4[Refresh token rotation]
        A5[Logout]
    end

    subgraph UserLayer[User Service]
        U1[User domain source of truth]
        U2[Profile / role / status]
        U3[Protected API JWT validation]
    end
```

## 6. 기본 라우팅 구조

```mermaid
flowchart TB
    IN[Incoming Request]
    P1[/auth/**]
    P2[/users/**]
    P3[/internal/** optional block]
    A[Route to Auth Service]
    U[Route to User Service]
    B[Block external access]

    IN --> P1 --> A
    IN --> P2 --> U
    IN --> P3 --> B
```

## 7. 요청 처리 파이프라인

```mermaid
flowchart LR
    Req[Client Request]
    C1[CORS / Header Filter]
    C2[Trace Id Filter]
    C3[Rate Limit Filter]
    C4[Optional JWT Pre-check]
    C5[Route Decision]
    C6[Forward to Target Service]
    Res[Response Mapping]

    Req --> C1 --> C2 --> C3 --> C4 --> C5 --> C6 --> Res
```

## 8. 로그인 요청 처리 흐름

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as Api Gateway
    participant A as Auth Service

    C->>G: POST /auth/login
    G->>G: Apply common filters
    G->>A: Forward request
    A-->>G: access token + refresh token
    G-->>C: Response
```

## 9. 보호 API 요청 처리 흐름

`Gateway`는 JWT를 선검증할 수 있지만, 최종 검증 책임은 대상 서비스에 있다.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as Api Gateway
    participant U as User Service

    C->>G: GET /users/me + Bearer token
    G->>G: Optional pre-check
    G->>U: Forward Authorization header
    U->>U: Validate JWT signature and claims
    U-->>G: Business response
    G-->>C: Response
```

## 10. Gateway에서 JWT 선검증을 둘 때의 원칙

```mermaid
flowchart TB
    T1[Check Authorization header format]
    T2[Optional lightweight signature check]
    T3[Optional exp check]
    T4[Do not become token issuer]
    T5[Do not replace service-side validation]

    T1 --> T2 --> T3 --> T4 --> T5
```

설명:

- `Gateway`의 JWT 선검증은 성능과 공통 오류 응답을 위한 선택 기능이다.
- `User-service`와 `auth-service`는 각자 필요한 최종 검증을 유지해야 한다.

## 11. 에러 처리 구조

```mermaid
flowchart LR
    E1[Invalid route]
    E2[Rate limit exceeded]
    E3[Missing Authorization header]
    E4[Optional pre-check failed]
    E5[Downstream service error]
    OUT[Unified error response]

    E1 --> OUT
    E2 --> OUT
    E3 --> OUT
    E4 --> OUT
    E5 --> OUT
```

## 12. 공통 헤더와 추적 구조

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as Api Gateway
    participant A as Auth Service

    C->>G: Request
    G->>G: Generate or propagate traceId
    G->>A: Forward request with traceId
    A-->>G: Response with trace context
    G-->>C: Response
```

## 13. 권장 구현 원칙

- `Gateway`는 인증의 기준 시스템이 아니다.
- `Gateway`는 토큰을 발급하지 않는다.
- `Gateway`는 토큰을 저장소의 기준 값으로 관리하지 않는다.
- 내부 서비스는 게이트웨이 뒤에 있어도 JWT를 직접 검증한다.
- `Gateway`는 내부 전용 경로를 외부에 노출하지 않는다.
- 공통 보안 헤더와 CORS 정책은 게이트웨이에서 일관되게 적용한다.
- 인증 실패와 라우팅 실패에 대한 응답 형식은 통일한다.
- rate limit과 로깅은 게이트웨이에서 공통 처리하고, 비즈니스 판단은 내부 서비스에서 처리한다.

## 14. 한 줄 정리

- `Gateway`는 "들어오는 요청을 정리하고 전달하는 계층"이다.
- `auth-service`는 "인증하고 토큰을 발급하는 계층"이다.
- `user-service`는 "사용자 도메인의 기준 정보를 소유하는 계층"이다.
