# user-service

## 역할

- 사용자 기본 정보의 기준 저장소입니다.
- 소셜 제공자 계정과 내부 사용자 계정의 연결 정보를 소유합니다.
- gateway 뒤에서 `/users/**`, `/internal/users/**` API를 제공합니다.
- 사용자 생성, 상태 변경, 소셜 링크 생성 이력을 감사 로그로 기록합니다.

## 서비스 이름

| 항목           | 값                  |
|--------------|--------------------|
| 구현/PR/런타임 이름 | `user-service`     |
| Gradle group | `com.userservice` |
| 서비스 포트       | `8082`             |

## Contract Source

- 공통 계약 레포: `https://github.com/jho951/contract`
- 계약 동기화 기준 파일: [contract.lock.yml](contract.lock.yml)
- PR에서는 `.github/workflows/contract-check.yml`이 lock 파일과 계약 영향 변경 여부를 검사합니다.
- 인터페이스 변경 시 본 저장소 구현보다 계약 레포 변경을 먼저 반영합니다.

## 빠른 시작

GitHub Packages 의존성을 받으려면 `GH_TOKEN`이 필요합니다.

```bash
export GITHUB_ACTOR=jho951
export GH_TOKEN=<github-token-with-read-packages>
```

Docker 개발 스택 실행:

```bash
./scripts/run.docker.sh up dev
```

로컬 직접 실행:

```bash
./scripts/run.local.sh
```

빌드와 테스트:

```bash
./gradlew build
```

상태 확인:

```bash
curl -i http://localhost:8082/actuator/health
curl -i http://localhost:8082/actuator/prometheus
```

## 문서

- [문서 홈](docs/README.md)
- [구조](docs/architecture.md)
- [Auth API](docs/api.md)
- [CI와 구현 기준](docs/ci-and-implementation.md)
- [DB](docs/database.md)
- [Docker](docs/docker.md)
- [Terraform 인프라](infra/terraform/README.md)
- [Platform 사용 기준](docs/platform.md)
- [문제 해결](docs/troubleshooting.md)
- [OpenAPI](docs/openapi/user-service.yml)
