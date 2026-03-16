# Signup 분리 리팩터링 계획서

## 1. 목적

현재 프로젝트에는 인증 책임과 사용자 생성 책임이 함께 존재한다.

- `auth-service`: 로그인, JWT 발급, refresh token, 인증 필터, SSO, 세션 기반 SSO
- 현재 포함된 사용자 책임: `signup`, `User`, `UserSocial`

목표는 `signup`과 사용자 도메인을 `user-service`로 분리하고, `auth-service`는 인증 전용 서비스로 정리하는 것이다.

## 2. 현재 문제

현재 구조에서는 아래 문제가 있다.

- 인증 로직과 사용자 도메인 로직이 같은 서비스에 공존한다.
- 회원가입 변경이 인증 서비스 배포와 결합된다.
- 사용자 프로필/상태 정책이 늘어날수록 `auth-service`가 비대해진다.
- 장기적으로 MSA 경계가 흐려진다.

## 3. 목표 상태

리팩터링 완료 후 역할은 아래처럼 나눈다.

### auth-service

- 로그인
- JWT access token 발급
- refresh token 발급 / rotation / 폐기
- 로그아웃
- 인증 필터
- OAuth2 / SSO 연동
- 세션 기반 SSO
- 인증 계정 생성 및 자격 증명 관리

### user-service

- 회원가입
- 사용자 기본 정보 생성
- 사용자 상태 관리
- 소셜 계정 매핑 정보 관리
- 프로필/동의/부가 사용자 정보 관리

## 4. 리팩터링 범위

### auth-service에서 제거 대상

- `UserController`의 `signup`
- `UserService`의 회원 생성 오케스트레이션
- 사용자 프로필성 도메인 책임

### auth-service에 남길 대상

- `Auth`
- 비밀번호 해시 관리
- 로그인 검증
- 토큰 발급/재발급/로그아웃
- SSO 인증 및 세션 교환

### user-service로 이동 또는 신규 구현 대상

- `User`
- `UserSocial`
- 회원가입 API
- 사용자 상태 조회 API
- 내부 사용자 조회 API

## 5. 설계 원칙

- 서비스 간 DB 직접 접근 금지
- 전역 사용자 식별자는 `userId`로 통일
- `auth-service`는 최소 사용자 정보만 참조
- 회원가입 흐름은 `user-service` 중심으로 재구성
- 로그인 시 필요한 사용자 상태는 API 또는 이벤트로 동기화

## 6. 목표 아키텍처

### 회원가입 흐름

1. 클라이언트가 `user-service`에 회원가입 요청
2. `user-service`가 사용자 기본 정보 생성
3. `user-service`가 `auth-service`에 인증 계정 생성 요청
4. 두 작업이 성공하면 회원가입 완료
5. 선택적으로 `auth-service`에서 즉시 로그인 또는 토큰 발급

### 로그인 흐름

1. 클라이언트가 `auth-service`에 로그인 요청
2. `auth-service`가 credential 검증
3. 필요 시 `user-service`에서 `status`, `role` 조회
4. `auth-service`가 access token / refresh token 발급

### SSO 흐름

1. 클라이언트가 `auth-service`로 SSO 시작
2. `auth-service`가 provider 인증 완료
3. `user-service`에서 provider 기반 사용자 조회
4. 없으면 `user-service`가 신규 사용자 생성
5. `auth-service`가 토큰 발급

## 7. 단계별 실행 계획

## 단계 1. 계약 정의

목표:

- `auth-service`와 `user-service` 간 내부 API 계약 확정

할 일:

- 내부 API 스펙 정의
- `userId`, `role`, `status`, `email` 필드 계약 확정
- 사용자 상태 enum 공유 또는 매핑 전략 정의
- 오류 코드 계약 정의

산출물:

- API 명세서
- DTO 초안
- 오류 응답 규칙

## 단계 2. user-service 골격 생성

목표:

- 회원가입과 사용자 조회를 담당할 신규 서비스 뼈대 구성

할 일:

- `user-service` 프로젝트 생성
- `users`, `user_social_accounts` 등 테이블 설계
- 회원가입 API 구현
- 내부 조회 API 구현
- 사용자 상태 조회 API 구현

산출물:

- `POST /api/users/signup`
- `GET /internal/users/{userId}`
- `GET /internal/users/by-email`
- `GET /internal/users/by-social`

## 단계 3. auth-service 내부 계정 생성 API 추가

목표:

- 회원가입 중 `auth-service`가 인증 계정을 생성할 수 있도록 내부 API 제공

할 일:

- 내부 전용 계정 생성 API 추가
- password hash 저장 책임을 `auth-service`로 제한
- 중복 계정 검증 로직 구현
- 내부 서비스 인증 방식 적용

예시 API:

- `POST /internal/auth/accounts`
- `DELETE /internal/auth/accounts/{userId}` rollback 용도

산출물:

- 인증 계정 생성/삭제 API
- 내부 호출 보안 정책

## 단계 4. signup 진입점 이동

목표:

- 외부 회원가입 API를 `auth-service`에서 제거하고 `user-service`로 이동

할 일:

- 클라이언트 진입점을 `user-service` 또는 gateway 경유 경로로 변경
- `auth-service`의 `/v1/user/signup` 사용 중단
- deprecation 공지 또는 임시 호환 레이어 제공

권장 방안:

- 초기에는 `auth-service` signup API를 deprecated 처리
- 내부적으로는 `user-service` 호출로 위임
- 안정화 후 endpoint 제거

## 단계 5. 데이터 이전

목표:

- 기존 사용자 데이터를 `user-service` 저장소로 이동

할 일:

- 기존 `users`, `user_social` 테이블 데이터 분석
- 신규 스키마로 마이그레이션 스크립트 작성
- 정합성 검증
- 마이그레이션 중단/복구 전략 수립

검증 항목:

- 사용자 수 일치 여부
- 이메일 중복 여부
- provider 매핑 정합성
- userId 참조 무결성

## 단계 6. 로그인 연동 전환

목표:

- 로그인 시 필요한 사용자 상태 조회를 `user-service` 기준으로 전환

할 일:

- `auth-service`에서 사용자 상태를 직접 소유하지 않도록 조정
- 로그인 시 내부 API 조회 또는 캐시 전략 적용
- 장애 시 fallback 정책 정의

선택지:

- 초기: 동기 API 조회
- 이후: 이벤트 기반 캐시 동기화

## 단계 7. SSO 신규 사용자 생성 전환

목표:

- SSO 최초 로그인 시 사용자 생성을 `user-service`가 수행하도록 변경

할 일:

- provider 사용자 식별값 기준 내부 조회 API 구현
- 미존재 시 `user-service`가 사용자와 소셜 링크 생성
- 생성 후 `auth-service`는 토큰만 발급

## 단계 8. 기존 코드 정리

목표:

- `auth-service`에서 사용자 도메인 잔존 코드 제거

할 일:

- `UserController` 제거
- `signup` 관련 DTO 제거 또는 내부 전용 변환
- `UserService` 의존 제거
- auth-service 문서와 Swagger 갱신

## 8. 세부 작업 항목

### auth-service

- 내부 계정 생성 API 추가
- 로그인 시 사용자 상태 조회 연동
- SSO 사용자 매핑 로직 수정
- 기존 signup endpoint deprecated 처리
- 최종 제거 시 관련 DTO / Service / Controller 정리

### user-service

- 회원가입 API 구현
- 내부 사용자 조회 API 구현
- 소셜 사용자 조회/생성 API 구현
- 사용자 상태/역할 반환 API 구현

### 공통

- 내부 서비스 인증 방식 정의
- 공통 error code 정리
- distributed tracing / request id 연계
- 테스트 픽스처 분리

## 9. API 초안

### user-service 외부 API

`POST /api/users/signup`

요청 예시:

```json
{
  "email": "user@example.com",
  "password": "plain-password",
  "name": "홍길동",
  "provider": "LOCAL"
}
```

응답 예시:

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "status": "ACTIVE"
}
```

### auth-service 내부 API

`POST /internal/auth/accounts`

요청 예시:

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "password": "plain-password"
}
```

### user-service 내부 API

`GET /internal/users/{userId}`

응답 예시:

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "role": "USER",
  "status": "ACTIVE"
}
```

## 10. 데이터 이전 전략

권장 순서:

1. 신규 `user-service` 스키마 준비
2. 데이터 복제
3. 읽기 검증
4. signup write path 전환
5. 로그인 조회 경로 전환
6. 구 경로 제거

전략 포인트:

- big bang 전환보다 점진 전환 권장
- 신규 write는 user-service 우선
- 기존 데이터는 배치 이관
- cutover 전후 비교 리포트 생성

## 11. 테스트 계획

### 단위 테스트

- 계정 생성 검증
- 중복 이메일 검증
- 사용자 상태별 로그인 허용/차단
- SSO 최초 가입 로직

### 통합 테스트

- signup end-to-end
- login end-to-end
- refresh rotation
- logout 후 refresh 차단
- SSO 최초 가입 및 재로그인

### 회귀 테스트

- 기존 로그인 API 호환성
- access token claim 검증
- Redis refresh token 폐기 검증

## 12. 위험 요소

- 회원가입 트랜잭션이 서비스 간 분산 처리로 바뀐다
- 부분 실패 시 보상 트랜잭션이 필요하다
- 로그인 시 user-service 장애가 인증 실패로 전이될 수 있다
- SSO 최초 가입 시 중복 사용자 생성 위험이 있다

대응:

- idempotency key 또는 중복 방지 키 적용
- 내부 API timeout / retry 정책 정의
- 계정 생성 rollback API 또는 outbox/eventual consistency 도입
- 사용자 상태 조회 캐시 전략 도입 검토

## 13. 완료 기준

아래 조건을 만족하면 분리 완료로 본다.

- 외부 회원가입 API가 `user-service`에서 제공된다
- `auth-service`는 로그인/토큰/SSO만 담당한다
- `auth-service`가 사용자 DB를 직접 소유하지 않는다
- 로그인/SSO/refresh/logout 회귀 테스트가 통과한다
- 운영 문서와 API 문서가 최신 상태다

## 14. 권장 구현 순서 요약

1. 내부 API 계약 정의
2. `user-service` 골격 생성
3. `auth-service` 내부 계정 생성 API 추가
4. 신규 signup 경로 구현
5. 데이터 이전
6. 로그인/SSO 연동 전환
7. 기존 signup 제거
8. 문서 및 테스트 마무리
