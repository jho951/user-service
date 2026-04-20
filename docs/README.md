# user-service 문서

user-service의 구현, 운영, 문제 해결 문서는 이 디렉토리에서 관리합니다.
서비스 간 책임, API 계약, gateway route 계약은 contract 레포를 기준으로 합니다.

## 문서 가이드

- [구조](./architecture.md): 모듈, 패키지, 의존 방향, 코드 배치 기준
- [Auth API](./auth-api.md): 공개 사용자 API, 내부 사용자 API, JWT 보안 계약
- [CI와 구현 기준](./ci-and-implementation.md): Gradle 멀티모듈, Java 17, CI, 로컬 구현 규칙
- [DB](./database.md): 관리 테이블, UUID `CHAR(36)` 바인딩, 상태 코드, 제약조건
- [Docker](./docker.md): dev/prod Compose 구조, 네트워크, 실행 스크립트
- [Platform 사용 기준](./platform.md): `platform-security`, `platform-governance`, `platform-integrations` 소비 기준
- [문제 해결](./troubleshooting.md): 운영 판단 기준과 자주 막히는 문제
- [OpenAPI](./openapi/user-service.yml): user-service HTTP API 명세

## OpenAPI Sync

`docs/openapi/user-service.yml`은 service-contract의 upstream OpenAPI local copy입니다.
계약이 바뀌면 contract repo의 user-service upstream OpenAPI를 먼저 갱신한 뒤, 이 파일을 같은 내용으로 맞춥니다.
