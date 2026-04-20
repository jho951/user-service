# CI와 구현 기준

## Gradle 프로젝트

Gradle 루트는 멀티모듈 집계 프로젝트입니다.

```text
user-service
├── app
└── common
```

모듈 구성:

| 모듈 | Gradle 플러그인 | 로컬 역할 |
| --- | --- | --- |
| root | Gradle root project | 전체 모듈 집계, 공통 group/version 관리 |
| `app` | Spring Boot, Java | 실행 가능한 user-service 애플리케이션 |
| `common` | `java-library` | 공통 응답/예외/로깅/base entity 인프라 |

루트 `settings.gradle`는 아래 모듈만 include합니다.

```groovy
include 'common'
include 'app'
```

## Java와 버전 기준

공통 기준:

- Java toolchain: 17
- Spring Boot: `3.5.x`
- Gradle wrapper 사용
- 테스트 플랫폼: JUnit Platform

버전 값은 `gradle.properties`에서 관리합니다.

필수 속성:

- `projectGroup`
- `projectVersion`
- `javaVersion`
- `springBootVersion`
- `jakartaPersistenceVersion`
- `lombokVersion`

## 의존성 해석

Repository는 `settings.gradle`의 `dependencyResolutionManagement`에서 중앙 관리합니다.

사용 repository:

- Maven Central
- GitHub Packages: `https://maven.pkg.github.com/jho951/platform-governance`
- GitHub Packages: `https://maven.pkg.github.com/jho951/platform-security`
- GitHub Packages: `https://maven.pkg.github.com/jho951/platform-integrations`

platform package는 private package이므로 인증값이 필요합니다.

로컬 shell:

```bash
export GITHUB_ACTOR=jho951
export GH_TOKEN=<github-token-with-read-packages>
```

또는 `~/.gradle/gradle.properties`:

```properties
githubPackagesUsername=your-github-id
githubPackagesToken=your-package-read-token
```

기존 로컬 설정과의 호환을 위해 `githubUsername`, `githubToken`도 fallback으로 읽습니다.

CI와 Docker build도 `GH_TOKEN`을 GitHub Packages token으로 사용합니다.
토큰에는 `read:packages` 권한이 필요합니다.

## CI workflow

현재 GitHub Actions workflow:

| workflow | 파일 | 실행 조건 | 주요 명령 |
| --- | --- | --- | --- |
| CI | `.github/workflows/ci.yml` | 저장소 설정 기준 | Gradle build/test |
| CD | `.github/workflows/cd.yml` | 저장소 설정 기준 | Docker image 또는 배포 준비 |

CI 공통 기준:

- Runner: `ubuntu-latest`
- JDK: 17
- package 권한: `packages: read`
- source 권한: `contents: read`
- GitHub Actions secret `GH_TOKEN`은 private package read 권한을 가져야 합니다.

## 로컬 검증

일반 검증:

```bash
./gradlew build
```

특정 모듈만 확인:

```bash
./gradlew :app:test
./gradlew :common:compileJava
```

로컬 실행:

```bash
./scripts/run.local.sh
```

또는 Gradle로 직접 실행합니다.

```bash
./gradlew clean :app:bootRun
```

Docker 실행 기준은 [docker.md](./docker.md)에 정리되어 있습니다.

## 구현 규칙

모듈 의존 방향:

- `app`은 `common`에 의존할 수 있습니다.
- `common`은 `app`에 의존하면 안 됩니다.
- 외부 서비스별 client boundary는 `app` 도메인 하위 패키지에 둡니다.
- 공통 응답/예외/로깅/base entity만 `common`으로 올립니다.

Spring Boot 실행 진입점:

- `app/src/main/java/com/userservice/app/ApiApplication.java`

리소스 구성:

- 공통 설정: `app/src/main/resources/application.yml`
- dev profile: `app/src/main/resources/application-dev.yml`
- prod profile: `app/src/main/resources/application-prod.yml`

구현 시 코드 배치 기준은 [architecture.md](./architecture.md)를 따릅니다.

DB 변경 기준은 [database.md](./database.md)를 따릅니다.

Docker 실행 기준은 [docker.md](./docker.md)를 따릅니다.

## 완료 기준

- `./gradlew build`가 성공해야 합니다.
- `./scripts/run.docker.sh up dev`가 개발 스택을 기동해야 합니다.
- `./scripts/run.docker.sh ps dev`로 mysql, user-service 상태를 확인할 수 있어야 합니다.
- `/actuator/health`가 정상 응답해야 합니다.
- 공개 가입 API가 동작해야 합니다.
- `/users/me`는 비활성 사용자를 거부해야 합니다.
- 내부 API는 `internal` scope 없는 JWT를 거부해야 합니다.
- 소셜 링크 요청은 재시도 상황에서 멱등하게 처리되어야 합니다.

## 변경 체크리스트

코드 변경 전후 확인:

1. 새 코드가 `app`인지 `common`인지 먼저 결정합니다.
2. 새 dependency는 `gradle.properties` 관리가 필요한지 확인합니다.
3. private dependency가 추가되면 CI secret과 Docker build arg 영향도 확인합니다.
4. public API 변경이면 `docs/openapi/user-service.yml`을 같이 갱신합니다.
5. DB 변경이면 entity 변경과 운영 schema/migration 필요 여부를 먼저 구분합니다.
6. Docker runtime 값이 바뀌면 `docker/{dev,prod}/compose.yml`, [docker.md](./docker.md)를 같이 갱신합니다.
7. 최소 `./gradlew test` 또는 변경 범위에 맞는 Gradle task를 실행합니다.
