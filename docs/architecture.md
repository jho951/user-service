# user-service 구조

## 계약 기준

- 구현 저장소 이름은 `user-service`입니다.
- 런타임 서비스 이름은 `user-service`입니다.
- Docker service, gateway upstream, metric tag, 감사 로그 service name은 `user-service`를 기준으로 합니다.
- 서비스 간 책임, API 계약, 이벤트 계약, 공통 identity header 계약은 contract 레포를 기준으로 합니다.
- 이 문서는 contract를 반복 정의하지 않고, user-service 내부 구현 배치 기준을 다룹니다.
- 인터페이스 변경 시 본 저장소 구현보다 계약 레포 변경을 먼저 반영합니다.

## 서비스 책임

user-service가 소유합니다.

- 사용자 마스터 데이터
- 사용자 상태
- 이메일 기준 사용자 조회
- 사용자 id 기준 사용자 조회
- 소셜 provider key와 내부 사용자 id의 연결 원본
- 사용자 생성, 소셜 링크 생성, 상태 변경 감사 로그
- `/users/**`, `/internal/users/**` API 접근 정책

user-service가 직접 소유하지 않습니다.

- 비밀번호 저장
- 로그인 인증
- refresh token 발급 또는 저장
- 최종 권한 truth 관리
- 외부 CORS 정책
- gateway route versioning

서비스 경계:

- 비밀번호와 인증 세션은 auth-service가 담당합니다.
- 세부 권한 판정의 기준은 authz-service가 담당합니다.
- 외부 경로 노출과 CORS는 gateway가 담당합니다.

## 모듈 경계

| 모듈 | 로컬 역할 | 둘 위치 | 두지 않을 것 |
| --- | --- | --- | --- |
| `app` | Spring Boot 애플리케이션과 user-service 업무 로직 | 사용자 API, 소셜 링크, 보안 정책 입력, 감사 adapter, runtime 설정 | 여러 서비스가 공유해야 하는 범용 라이브러리 코드 |
| `common` | 서비스 내부 공통 인프라 | 공통 응답, 공통 예외, base entity, logging helper | user-service 도메인 규칙 |
| `docker` | 컨테이너 실행 정의 | Dockerfile, dev/prod Compose, MySQL 설정 | 로컬 shell orchestration |
| `scripts` | 로컬/운영 보조 명령 | Docker 실행 래퍼, 로컬 bootRun 래퍼 | 서비스 런타임 로직 |
| `docs` | 설계와 운영 문서 | 구조, API, DB, Docker, platform, OpenAPI | 코드가 기준이어야 하는 구현 세부사항 |

## 패키지 경계

`app/src/main/java/com/userservice/app`

| 패키지 | 로컬 역할 |
| --- | --- |
| `config.security` | platform-security 체인에서 호출하는 서비스별 접근 정책 |
| `config.logging` | request MDC와 access log filter |
| `config.governance` | platform-governance 감사 설정 |
| `domain.user` | 사용자, 사용자 상태, 소셜 링크 API와 업무 규칙 |
| `domain.audit` | user-service 감사 이벤트 기록 adapter |

`common/src/main/java/com/userservice/app/common`

| 패키지 | 로컬 역할 |
| --- | --- |
| `base` | 공통 응답, 성공/오류 코드, base entity, 공통 예외 처리 |
| `logging` | 로깅 header, MDC key, 민감정보 마스킹 |

## 의존 방향

- `app`은 `common`에 의존할 수 있습니다.
- `common`은 `app`에 의존하면 안 됩니다.
- 도메인 패키지는 controller 패키지에 의존하지 않습니다.
- Controller는 request/response 변환과 흐름 위임만 담당하고, 판단은 service에 위임합니다.
- 소셜 링크의 canonical owner는 user-service입니다. auth-service는 인증 흐름을 담당하되 소셜 링크 소유권을 직접 판단하지 않습니다.
- Spring Security 체인 조립과 JWT 검증은 platform-security가 담당합니다. `config.security`는 서비스별 접근 정책 입력만 둡니다.
- 감사 이벤트 발생 시점은 도메인 service가 결정하고, platform-governance 변환은 adapter에 둡니다.

## 코드 배치 규칙

- 새 사용자 API request/response DTO는 `domain.user.dto`에 둡니다.
- 사용자 상태, role, 소셜 타입 enum은 `domain.user.constant`에 둡니다.
- 사용자 persistence는 `domain.user.entity`, `domain.user.repository`에 둡니다.
- 소셜 링크 metric은 `domain.user.observability`에 둡니다.
- 삭제 시각이 필요해지면 `deletedAt`은 `User` 엔티티에만 추가하고, 공통 `BaseEntity`에는 넣지 않습니다.
- 여러 도메인에서 재사용할 응답/예외/로깅 코드는 `common`에 둡니다.
- 환경별 런타임 값은 `app/src/main/resources/application-{dev,prod}.yml`에 둡니다.
- public contract가 바뀌면 `docs/openapi/user-service.yml`과 contract repo를 함께 갱신합니다.

## 테스트 배치

- 단일 컴포넌트를 검증하는 단위 테스트는 production package를 따라 배치합니다.
- 여러 패키지를 조합하는 흐름 테스트는 `com.userservice.app` 아래에 둘 수 있습니다.
- 보안 흐름은 `spring-security-test`로 인증 주체와 JWT claim 조건을 명시합니다.
- DB나 Docker 의존 테스트는 integration test로 이름을 구분하고 Gradle/JUnit 설정에서 선택 실행되게 둡니다.

## 변경 체크리스트

새 dependency 또는 패키지를 추가하기 전에 확인합니다.

1. 코드가 `app`에 속하는지 `common`에 속하는지 먼저 결정합니다.
2. 이미 해당 책임을 가진 패키지 경계가 있는지 확인합니다.
3. 외부 서비스 호출은 경계별 interface 뒤에 둡니다.
4. 변경 패키지 가까이에 집중된 테스트를 추가하거나 갱신합니다.
5. public contract가 바뀌면 OpenAPI 또는 문서를 같이 갱신합니다.
