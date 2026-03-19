# Auth-service가 가져야 할 내용

이 문서는 현재 SSO 구조를 기준으로 `BackEnd`가 `auth-service`로서 반드시 가져야 할 역할, 책임, 데이터, API, 보안 원칙을 정리한다.

기준 구조는 아래와 같다.

- `Api-gateway-server`: 외부 진입점, 라우팅, 공통 필터
- `BackEnd`: `auth-service`
- `User-server`: `user-service`, 사용자 도메인의 기준 시스템

## 1. 역할

`auth-service`의 역할은 "인증과 토큰 발급의 기준 시스템"이다.

즉, 사용자가 누구인지 검증하고, 다른 서비스가 신뢰할 수 있는 인증 상태를 토큰으로 발급하며, SSO와 세션 기반 인증 상태를 통제하는 서비스다.

## 2. 핵심 책임

`auth-service`는 아래 책임을 가진다.

- 로그인 처리
- 비밀번호 검증과 해시 관리
- OAuth2 / SSO 시작, 콜백 처리, provider 인증 연동
- access token 발급
- refresh token 발급, 검증, rotation, 폐기
- 로그아웃 처리
- SSO 세션 또는 ticket 생성, 검증, 만료 관리
- 내부 인증 계정 생성 API 제공
- 인증 필터 또는 JWT 검증 공통 모듈 제공
- 인증 감사 로그와 로그인 이력 관리
- 계정 잠금, 로그인 실패 횟수, 이상 인증 탐지 같은 인증 보안 정책 관리

## 3. 책임이 아닌 범위

아래는 `auth-service`가 직접 소유하면 안 되는 영역이다.

- 사용자 프로필 관리
- 사용자 상태 정책의 기준 저장
- 사용자 역할 정책의 기준 저장
- 닉네임, 소개, 이미지, 환경설정 같은 사용자 도메인 데이터
- 약관 동의, 마케팅 수신 동의, 부가 사용자 정보
- 사용자 검색, 목록 조회, 관리자용 사용자 도메인 기능

위 데이터와 정책의 기준 시스템은 `user-service`다.

## 4. auth-service가 소유해야 하는 데이터

`auth-service`는 자기 DB 또는 캐시에 인증 도메인 데이터를 직접 소유해야 한다.

### 4.1 Auth DB

`Auth DB`에 두는 것이 적절한 데이터 예시는 아래와 같다.

- `auth_accounts`
  - `id`
  - `user_id`
  - `login_id` or `email`
  - `password_hash`
  - `password_updated_at`
  - `account_locked`
  - `failed_login_count`
  - `last_login_at`
  - `created_at`
  - `updated_at`
- `auth_social_accounts`
  - `id`
  - `user_id`
  - `provider`
  - `provider_user_key`
  - `linked_at`
- `auth_audit_logs`
  - `id`
  - `user_id`
  - `event_type`
  - `ip`
  - `user_agent`
  - `created_at`

### 4.2 Redis

`Redis`는 짧은 수명 또는 세션성 인증 데이터를 둔다.

- refresh token 메타데이터
- refresh token rotation 상태
- revoked token or session marker
- SSO 세션
- one-time ticket
- OAuth2 state / nonce

## 5. auth-service가 참조만 해야 하는 데이터

아래 데이터는 `auth-service`가 직접 기준 저장소가 되어서는 안 된다.

- 사용자 이름
- 프로필 이미지
- 사용자 상태의 비즈니스 기준값
- 사용자 역할의 비즈니스 기준값
- 마케팅 수신 여부
- 약관 동의 이력

로그인 차단에 필요한 최소 정보만 `user-service`에서 조회하거나 캐시 동기화한다.

예시:

- `userId`
- `role`
- `status`
- `email`

## 6. 내부 API 책임

`auth-service`는 아래 내부 API를 가질 수 있다.

### 6.1 인증 계정 생성 API

- `POST /internal/auth/accounts`

용도:

- `user-service`가 회원 생성 후 인증 계정을 만들기 위해 호출

예시 요청 필드:

- `userId`
- `email`
- `rawPassword` 또는 사전 합의된 credential 입력값

주의:

- password hash 저장 책임은 `auth-service`에 둔다.

### 6.2 인증 계정 삭제 API

- `DELETE /internal/auth/accounts/{userId}`

용도:

- 회원가입 보상 트랜잭션 또는 롤백 처리

### 6.3 토큰 발급 API

외부 공개 API 예시:

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /auth/oauth2/authorize/{provider}`
- `GET /auth/oauth2/callback/{provider}`

## 7. 로그인 시 auth-service가 수행해야 하는 일

로그인 요청 시 `auth-service`는 아래 순서로 동작한다.

1. credential 검증
2. 인증 계정 상태 확인
3. 필요 시 `user-service`에서 `role`, `status` 조회
4. 로그인 차단 여부 판단
5. access token 발급
6. refresh token 저장 및 rotation 상태 생성
7. 감사 로그 기록

## 8. OAuth2 / SSO에서 auth-service가 수행해야 하는 일

`auth-service`는 SSO의 중심 서비스가 된다.

해야 할 일:

- OAuth2 provider redirect 생성
- state / nonce 생성과 검증
- provider callback 처리
- provider access token 또는 사용자 정보 교환
- 사용자 식별 결과를 `user-service`와 매핑
- 신규 사용자면 `user-service` 생성 API 호출
- 최종적으로 access token / refresh token 발급
- SSO 세션 또는 ticket 저장과 만료 관리

하지 말아야 할 일:

- 사용자 프로필 자체를 기준 데이터로 저장
- 사용자 도메인 정책을 독자적으로 판단

## 9. JWT에서 auth-service가 책임지는 것

`auth-service`는 토큰의 유일한 발급자여야 한다.

최소 책임:

- 서명 키 관리
- `iss` 고정
- `aud` 정책 관리
- `exp`, `iat` 설정
- 최소 claim만 포함
- refresh token과의 lifecycle 분리

권장 access token claim:

- `sub`: `userId`
- `iss`: `auth-service`
- `aud`: gateway or service
- `role`
- `status`
- `sid`: optional
- `exp`

## 10. 보안 원칙

- 비밀번호는 `auth-service`에서만 관리한다.
- access token은 짧은 수명으로 유지한다.
- refresh token은 rotation을 적용한다.
- 로그아웃 시 refresh token과 SSO 세션을 즉시 폐기한다.
- 내부 API는 별도 서비스 인증으로 보호한다.
- `auth-service`와 `user-service`는 서로 DB를 직접 조회하지 않는다.
- 토큰 서명 키와 OAuth2 client secret은 별도 시크릿 저장소로 관리한다.

## 11. auth-service가 user-service와 연동할 때의 원칙

- 사용자 도메인의 기준 정보는 `user-service`가 소유한다.
- 인증 가능 여부 판단에 필요한 최소 정보만 `user-service`에서 가져온다.
- 내부 연동은 API 또는 이벤트로 처리한다.
- DB join이나 직접 테이블 접근은 금지한다.

## 12. 한 줄 정리

- `auth-service`는 "사용자를 인증하고, 인증 상태를 토큰과 세션으로 통제하는 서비스"다.
- `user-service`는 "사용자 도메인의 기준 정보를 소유하는 서비스"다.
- 둘은 `userId`를 공통 식별자로 연결하고, 데이터는 API로만 교환한다.
