# Auth Service / User Service 분리 설계

## 1. 문서 목적

이 문서는 현재 프로젝트를 `auth-service` 중심으로 재정의하고, 향후 `user-service`를 별도 서비스로 분리하기 위한 책임 경계와 설계 기준을 정리한다.

핵심 목표는 아래와 같다.

- `auth-service`는 인증과 세션에만 집중한다.
- `user-service`는 회원과 프로필 도메인에 집중한다.
- 두 서비스의 경계를 명확히 하여 확장성과 유지보수성을 높인다.

## 2. 서비스 분리 원칙

### 2.1 Auth Service가 책임지는 범위

`auth-service`는 아래 책임만 가진다.

- 로그인
- JWT access token 발급
- refresh token 발급 및 rotation
- refresh token 검증 및 재발급
- 로그아웃
- 인증 필터 연동
- OAuth2 / SSO 연동
- 세션 기반 SSO 처리
- 인증 성공 후 최소 인증 정보 제공

즉, 이 서비스는 "사용자가 누구인지 증명하고, 인증 상태를 유지하고, 다른 서비스가 신뢰할 수 있는 토큰을 발급하는 역할"에 집중한다.

### 2.2 Auth Service가 책임지지 않는 범위

`auth-service`는 아래 책임을 직접 가지지 않는다.

- 회원가입의 전체 비즈니스 규칙
- 사용자 프로필 관리
- 닉네임, 소개, 이미지, 환경설정 관리
- 약관 동의 이력 관리
- 회원 상태 변경에 대한 비즈니스 정책
- 탈퇴, 휴면, 복구 정책의 도메인 처리
- 마이페이지 기능
- 사용자 검색, 목록 조회, 관리자용 사용자 관리

이 영역은 `user-service`의 책임이다.

### 2.3 User Service가 책임지는 범위

`user-service`는 아래 책임을 가진다.

- 회원 생성
- 사용자 기본 정보 관리
- 프로필 관리
- 사용자 상태 관리
- 사용자 권한/역할의 비즈니스 규칙 관리
- 약관 동의 및 부가 정보 관리
- 서비스별 사용자 조회 API 제공

## 3. 현재 프로젝트 기준 해석

현재 레포는 이름과 주요 엔드포인트 기준으로 `auth-service` 지향 구조다.

- `/api/auth/login`
- `/api/auth/refresh`
- `/api/auth/logout`
- OAuth2 / SSO 관련 엔드포인트

다만 현재는 `signup`과 `User`, `UserSocial` 도메인이 함께 존재한다. 이는 포트폴리오나 초기 단계에서는 허용 가능하지만, 장기적으로는 `user-service` 분리를 전제로 정리하는 것이 맞다.

즉, 현재 구조는 다음 상태로 볼 수 있다.

- 방향: 인증 서버 중심 설계
- 상태: 일부 사용자 도메인이 아직 auth-service 내부에 공존
- 목표: 사용자 도메인을 user-service로 이동

## 4. 목표 아키텍처

### 4.1 최종 구성

- `auth-service`
- `user-service`
- `api-gateway` 또는 `backend-for-frontend` 선택 적용 가능
- `redis` for refresh token / SSO session
- `mysql` 또는 별도 DB를 서비스별로 분리

### 4.2 설계 원칙

- 서비스별 DB 분리
- 인증 데이터와 사용자 프로필 데이터 분리
- 서비스 간 직접 테이블 공유 금지
- 서비스 간 통신은 API 또는 이벤트 기반으로 수행
- access token에는 최소한의 claim만 포함

## 5. Auth Service 상세 설계

### 5.1 핵심 책임

`auth-service`는 아래 기능을 수행한다.

1. Credential 기반 로그인
2. OAuth2 로그인 시작 및 콜백 처리
3. 인증 성공 시 access token / refresh token 발급
4. refresh token rotation
5. 로그아웃 시 refresh token 폐기
6. 인증 필터를 통한 access token 검증
7. 세션 기반 SSO 상태 유지 및 교환 처리

### 5.2 데이터 책임

`auth-service`는 아래 수준의 인증 데이터를 가진다.

- auth account id
- userId
- password hash
- login provider 정보
- refresh token 메타데이터
- SSO session / ticket
- 최근 로그인 이력, 인증 감사 로그

반대로 아래 데이터는 직접 소유하지 않는 것이 원칙이다.

- 상세 프로필
- 사용자 자기소개
- 마케팅 수신 동의
- 서비스별 선호 설정

### 5.3 토큰 설계

access token claim 예시:

- `sub`: userId
- `iss`: auth-service
- `aud`: frontend-user 또는 내부 서비스 audience
- `role`: USER, ADMIN
- `status`: ACTIVE, SUSPENDED
- `sid`: 선택적으로 세션 식별자

원칙은 다음과 같다.

- access token은 짧은 수명
- refresh token은 Redis 등에 저장 후 rotation 적용
- refresh token은 DB 또는 캐시에 해시/메타데이터 형태로 관리 가능
- 민감한 프로필 정보는 claim에 넣지 않음

### 5.4 세션 기반 SSO

세션 기반 SSO는 아래 흐름으로 설계한다.

1. 사용자가 OAuth2 또는 SSO 진입점을 호출
2. `auth-service`가 세션 또는 일회용 ticket 생성
3. 인증 성공 후 클라이언트는 `auth-service`에서 ticket 또는 세션을 교환
4. 최종적으로 access token / refresh token 발급

이때 `auth-service` 책임은 "SSO 인증 상태 수립과 세션 유지"까지만이다.

사용자 생성 또는 프로필 동기화가 필요하면 `user-service`와 연동한다.

## 6. User Service 상세 설계

### 6.1 핵심 책임

`user-service`는 "회원 도메인 시스템"으로 정의한다.

주요 책임:

- 회원 생성
- 이메일, 이름, 닉네임, 프로필 이미지 관리
- 사용자 상태 변경
- 사용자 역할에 대한 비즈니스 규칙 반영
- 탈퇴/휴면/복구 정책 적용
- 약관 동의 및 부가 정보 관리
- 관리자용 사용자 조회/관리 API 제공

### 6.2 user-service 데이터 모델 예시

#### users

- `id`
- `email`
- `name`
- `nickname`
- `status`
- `role`
- `createdAt`
- `updatedAt`

#### user_profiles

- `user_id`
- `profile_image_url`
- `bio`
- `timezone`
- `locale`

#### user_consents

- `user_id`
- `terms_version`
- `privacy_version`
- `marketing_opt_in`
- `consented_at`

#### user_social_accounts

- `id`
- `user_id`
- `provider`
- `provider_user_key`
- `linked_at`

핵심은 `auth-service`의 인증 계정 정보와 `user-service`의 사용자 도메인 정보를 분리하는 것이다.

### 6.3 user-service API 예시

공개/내부 API는 아래 수준으로 설계할 수 있다.

#### 내부 생성/동기화 API

- `POST /internal/users`
- `POST /internal/users/social`
- `PATCH /internal/users/{userId}/status`
- `GET /internal/users/{userId}`
- `GET /internal/users/by-email`

#### 외부 사용자 API

- `GET /api/users/me`
- `PATCH /api/users/me`
- `DELETE /api/users/me`
- `GET /api/users/{id}`

외부 API는 BFF 또는 gateway 뒤에 두고, 내부 연동 API는 서비스 간 통신 전용으로 분리하는 것이 좋다.

## 7. Auth Service와 User Service의 연동 방식

### 7.1 회원가입 플로우

권장 흐름:

1. 클라이언트가 회원가입 요청
2. `user-service`가 사용자 엔티티 생성
3. `auth-service`가 사용자 인증 계정 생성
4. 완료 후 로그인 또는 토큰 발급

주의할 점은 "누가 회원가입의 오케스트레이션을 담당하는가"이다.

권장안:

- 단순한 구조에서는 `api-gateway` 또는 `bff`가 오케스트레이션
- 내부적으로는 `user-service` 생성 후 `auth-service` 계정 생성

대안:

- `auth-service`가 signup entrypoint를 받아도 되지만, 장기적으로는 user 도메인 책임이 auth로 다시 섞인다

따라서 최종적으로는 회원가입 시작점도 `user-service` 또는 별도 orchestrator에 두는 쪽이 더 명확하다.

### 7.2 일반 로그인 플로우

1. 클라이언트가 `auth-service`에 로그인 요청
2. `auth-service`가 credential 검증
3. 필요 시 `user-service`에서 사용자 상태 확인
4. `auth-service`가 토큰 발급

여기서 로그인 자체는 `auth-service` 책임이다.

### 7.3 OAuth2 / SSO 로그인 플로우

1. 클라이언트가 `auth-service`에 SSO 시작 요청
2. `auth-service`가 외부 provider 인증 처리
3. 인증된 provider key 기준으로 사용자 매핑 확인
4. 사용자 정보가 없으면 `user-service`에 사용자 생성 요청
5. 사용자 식별자 확보 후 `auth-service`가 토큰 발급

즉, SSO의 인증 자체는 auth 책임이고, "최초 사용자 생성"은 user 책임이다.

### 7.4 사용자 상태 동기화

로그인 차단이 필요한 상태는 `auth-service`에서도 알아야 한다.

예:

- ACTIVE
- PENDING
- SUSPENDED
- DELETED

동기화 방법은 두 가지가 있다.

- 동기 조회: 로그인 시 `user-service` 조회
- 이벤트 동기화: `user-service`의 상태 변경 이벤트를 받아 `auth-service`에 반영

초기에는 동기 조회가 단순하고, 트래픽이 커지면 이벤트 기반 캐시 동기화를 고려한다.

## 8. 서비스 간 데이터 계약

### 8.1 식별자 원칙

- 전역 사용자 식별자는 `userId` 하나로 통일
- auth/account id와 userId는 구분 가능하지만 외부 식별자는 userId로 맞춘다

### 8.2 최소 공유 필드

`auth-service`가 필요로 하는 최소 사용자 정보:

- `userId`
- `email`
- `role`
- `status`

이 이상을 기본 토큰 claim이나 인증 응답에 과도하게 실지 않는다.

## 9. 인프라 및 저장소 설계

### 9.1 Auth Service 저장소

- RDB: auth account, audit, provider link 메타데이터
- Redis: refresh token, SSO session, one-time ticket

### 9.2 User Service 저장소

- RDB: users, profiles, consent, policy-related data
- 필요 시 검색 인덱스 또는 캐시 별도 구성

### 9.3 DB 분리 원칙

- auth-service는 user-service DB를 직접 조회하지 않는다
- user-service도 auth-service DB를 직접 조회하지 않는다
- 반드시 API 또는 이벤트로 통신한다

## 10. 보안 원칙

- refresh token은 rotation 적용
- 로그아웃 시 refresh token 즉시 폐기
- access token TTL은 짧게 유지
- 서비스 간 내부 API는 mTLS, internal gateway, signed token 등으로 보호
- SSO session/ticket은 일회성 또는 짧은 만료시간 적용
- 비밀번호는 `user-service`가 아닌 `auth-service`에서만 관리

## 11. 권장 분리 시나리오

### 단계 1. 현재 구조 유지

- auth-service에 signup이 남아 있는 상태
- 빠른 구현과 포트폴리오 정리에 유리

### 단계 2. user-service 신설

- `User`, `UserSocial`, 프로필 관련 로직 이동
- 회원가입 API를 user-service로 이동
- auth-service는 로그인, 토큰, SSO만 담당

### 단계 3. 서비스 간 계약 안정화

- 내부 API 정리
- 상태 동기화 방식 정의
- 장애 시 fallback 정책 정리

### 단계 4. 완전한 책임 분리

- auth-service는 인증 전용
- user-service는 회원 전용
- 필요 시 admin-service도 추가 분리

## 12. 최종 결론

이 프로젝트의 목표 아키텍처는 아래처럼 정의한다.

- `auth-service`는 로그인, JWT 발급, refresh token 관리, 인증 필터 연동, OAuth2/SSO 연동, 세션 기반 SSO만 책임진다.
- `user-service`는 회원가입, 사용자 정보, 프로필, 상태, 동의, 사용자 관리 기능을 책임진다.

이 분리는 다음 장점을 가진다.

- 인증 로직과 사용자 도메인 로직의 결합도 감소
- 배포 및 확장 전략 분리 가능
- 보안 민감 영역과 일반 도메인 영역 분리
- 향후 MSA 확장 시 책임 경계가 명확함

현재 레포는 그 최종 구조로 가는 중간 단계이며, 다음 리팩터링 목표는 `signup`과 사용자 도메인을 `user-service`로 이동시키는 것이다.
