## 기능 설명

`auth-service`에 포함된 `signup` 및 사용자 도메인 책임을 `user-service`로 분리합니다.  
최종적으로 `auth-service`는 로그인, JWT 발급, refresh token 관리, 인증 필터, OAuth2/SSO, 세션 기반 SSO만 담당하도록 정리합니다.

작업 범위는 아래를 포함합니다.

- `signup` 외부 진입점 이동
- `user-service` 신규 설계 및 기본 API 정의
- `auth-service` 내부 계정 생성 API 추가
- 로그인/SSO 흐름에서 사용자 상태 조회 연동
- 기존 사용자 데이터 이전 계획 수립
- deprecated 경로 제거 및 문서 정리

상세 계획서는 아래 문서를 기준으로 진행합니다.

- `docs/signup-separation-refactoring-plan.md`
- `docs/auth-user-service-design.md`

## 필요한 이유

- 인증 로직과 사용자 도메인 로직의 결합도를 낮추기 위해 필요합니다.
- 회원가입/프로필/상태 관리 기능이 늘어나도 `auth-service`가 비대해지지 않도록 해야 합니다.
- 인증 민감 영역과 일반 사용자 도메인 영역을 분리해 유지보수성과 보안 경계를 명확히 해야 합니다.
- 장기적으로 MSA 구조에서 배포, 확장, 장애 격리 전략을 분리하기 위해 필요합니다.

## 참고 자료

- `docs/auth-user-service-design.md`
- `docs/signup-separation-refactoring-plan.md`
