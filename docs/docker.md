# User Docker

## Compose 파일

| 환경 | Compose 파일 | Spring 프로필 |
| --- | --- | --- |
| `dev` | `docker/dev/compose.yml` | `dev` |
| `prod` | `docker/prod/compose.yml` | `prod` |

실행:

```bash
./scripts/run.docker.sh up dev
./scripts/run.docker.sh up prod
```

종료:

```bash
./scripts/run.docker.sh down dev
./scripts/run.docker.sh down prod
```

빌드:

```bash
./scripts/run.docker.sh build dev
./scripts/run.docker.sh build prod
```

## 디렉토리 구조

Docker runtime 파일은 환경 단위로 관리합니다.

```text
docker
├── Dockerfile
├── dev
│   ├── compose.yml
│   └── services
│       └── mysql
│           └── my.cnf
└── prod
    ├── compose.yml
    └── services
        └── mysql
            └── my.cnf
```

`docker/Dockerfile`은 dev/prod가 공유합니다.
Compose와 환경별 MySQL 설정은 `docker/{env}` 아래에 함께 둡니다.

## 실행 스크립트

형식:

```bash
./scripts/run.docker.sh [up|down|build|logs|ps|restart] [dev|prod] [docker compose options]
```

예시:

```bash
./scripts/run.docker.sh ps dev
./scripts/run.docker.sh logs dev user-service
./scripts/run.docker.sh logs dev mysql
./scripts/run.docker.sh restart dev
```

`up`은 `--build -d`를 포함합니다.
`down`은 `--remove-orphans`를 포함합니다.

## 서비스

| 서비스 | 이미지 또는 빌드 | 네트워크 | 비고 |
| --- | --- | --- | --- |
| `user-service` | `docker/Dockerfile` build | `service-shared`, `user-private` | Spring Boot user-service |
| `mysql` | `mysql:8.4` | `user-private` | user-service 전용 MySQL |

서비스는 기본적으로 호스트 포트를 publish하지 않습니다.
gateway가 `service-shared` 네트워크에서 `http://user-service:8082`로 접근하는 구성을 전제로 합니다.

## 네트워크

- `service-shared` external network
  - gateway, auth-service, user-service 간 통신용 공유 네트워크입니다.
  - 기본 실제 네트워크 이름은 `service-backbone-shared`입니다.
- `user-private` internal network
  - `user-service`와 MySQL만 붙는 private network입니다.
  - MySQL은 공유 네트워크에 노출하지 않습니다.

구성 의도:

- 서비스 간 HTTP 호출은 external shared network를 사용합니다.
- user-service 전용 DB 접근은 internal private network로 제한합니다.
- 다른 서비스가 user DB에 직접 붙지 않도록 DB는 `user-private`에만 둡니다.

## 네트워크 이름 결정

`scripts/run.docker.sh`는 `up` 실행 시 공유 네트워크 이름을 아래 우선순위로 결정합니다.

```text
SERVICE_SHARED_NETWORK
BACKEND_SHARED_NETWORK
MSA_SHARED_NETWORK
service-backbone-shared
```

결정된 네트워크가 없으면 스크립트가 `docker network create`로 external network를 먼저 생성합니다.

Compose 파일에서는 아래 값으로 external network를 참조합니다.

```yaml
networks:
  service-shared:
    external: true
    name: ${SERVICE_SHARED_NETWORK:-${BACKEND_SHARED_NETWORK:-${MSA_SHARED_NETWORK:-service-backbone-shared}}}
```

## 환경 변수

주요 Docker runtime 변수:

| 변수 | 기본값 또는 기준 | 용도 |
| --- | --- | --- |
| `SERVICE_SHARED_NETWORK` | `service-backbone-shared` | external shared network 이름 |
| `SPRING_PROFILES_ACTIVE` | `dev` 또는 `prod` | Spring profile |
| `SPRING_DATASOURCE_URL` | Docker 기준 `jdbc:mysql://mysql:3306/user_service...` | user DB JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `user_service` | DB 사용자 |
| `SPRING_DATASOURCE_PASSWORD` | 환경별 값 | DB 비밀번호 |
| `MYSQL_DATABASE` | `user_service` | MySQL database |
| `MYSQL_USER` | `user_service` | MySQL 애플리케이션 사용자 |
| `MYSQL_PASSWORD` | 환경별 값 | MySQL 애플리케이션 비밀번호 |
| `MYSQL_ROOT_PASSWORD` | 환경별 값 | MySQL root 비밀번호 |
| `USER_SERVICE_INTERNAL_JWT_SECRET` | 환경별 값 | 내부 JWT HMAC secret |
| `USER_SERVICE_INTERNAL_JWT_ISSUER` | `auth-service` | 내부 JWT issuer |
| `USER_SERVICE_INTERNAL_JWT_AUDIENCE` | `user-service` | 내부 JWT audience |
| `FEATURES_PUBLIC_USER_API_ENABLED` | `true` | 공개 사용자 API 활성화 |
| `FEATURES_INTERNAL_USER_API_ENABLED` | `true` | 내부 사용자 API 활성화 |
| `GITHUB_ACTOR` | `jho951` | private GitHub Packages build 인증 사용자 |
| `GH_TOKEN` | 없음 | private GitHub Packages build 인증 토큰 |

`GITHUB_ACTOR`와 `GH_TOKEN`은 Docker build 단계에서 Gradle이 private GitHub Packages 의존성을 받을 때 필요합니다.

## 볼륨과 설정

MySQL data volume:

| 환경 | volume |
| --- | --- |
| `dev` | `user_service_mysql_data_dev` |
| `prod` | `user_service_mysql_data_prod` |

MySQL config:

| 환경 | `my.cnf` |
| --- | --- |
| `dev` | `docker/dev/services/mysql/my.cnf` |
| `prod` | `docker/prod/services/mysql/my.cnf` |

운영 데이터가 있는 환경에서는 Docker volume 삭제 명령을 별도로 실행하지 않습니다.

## 빌드 흐름

`docker/Dockerfile`은 multi-stage build입니다.

1. `eclipse-temurin:17-jdk`에서 의존성 확인과 `clean :app:bootJar -x test`를 실행합니다.
2. `eclipse-temurin:17-jre` runtime image에 app jar만 복사합니다.
3. 기본 entrypoint는 `java -jar /app/app.jar`입니다.

컨테이너 내부 기본 포트:

```text
8082
```

## 운영 확인

공유 네트워크 확인:

```bash
docker network inspect service-backbone-shared
```

컨테이너 상태 확인:

```bash
./scripts/run.docker.sh ps dev
```

로그 확인:

```bash
./scripts/run.docker.sh logs dev user-service
./scripts/run.docker.sh logs dev mysql
```

DB healthcheck가 실패하면 먼저 MySQL 환경 변수와 `mysql` 로그를 확인합니다.
