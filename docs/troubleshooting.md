# User 문제 해결

## `/users/me`가 401을 반환함

증상:

- 컨트롤러 로직에 도달하기 전에 인증에서 실패합니다.

가능한 원인:

- Bearer token이 없습니다.
- gateway가 검증한 사용자 식별자가 없습니다.
- JWT issuer, audience, secret이 설정과 다릅니다.
- `sub` claim이 비어 있습니다.

확인:

- `USER_SERVICE_INTERNAL_JWT_ISSUER`
- `USER_SERVICE_INTERNAL_JWT_AUDIENCE`
- `USER_SERVICE_INTERNAL_JWT_SECRET`
- gateway가 `Authorization` 또는 검증된 `X-User-Id`를 전달하는지 확인합니다.

조치:

- 설정값과 일치하는 토큰을 다시 발급합니다.
- gateway route의 인증 헤더 전달 설정을 확인합니다.

## `/users/me`가 403을 반환함

증상:

- 인증은 성공했지만 접근이 거부됩니다.

가능한 원인:

- DB의 사용자 상태가 `ACTIVE`가 아닙니다.
- JWT `sub` 또는 gateway가 전달한 사용자 식별자가 다른 사용자를 가리킵니다.

확인:

- `users` 테이블의 사용자 상태를 확인합니다.
- gateway principal 또는 JWT `sub`가 실제 사용자 ID와 일치하는지 확인합니다.

조치:

- 사용할 수 있는 계정이면 상태를 active로 변경합니다.
- gateway 사용자 식별자 전달 설정을 확인합니다.

## 내부 API가 403을 반환함

증상:

- `/internal/users/**` 요청이 Bearer token을 포함해도 거부됩니다.

가능한 원인:

- `internal` scope가 없습니다.
- issuer, audience, secret이 다릅니다.
- auth-service가 user-service용 내부 토큰을 발급하지 않았습니다.

확인:

- `scope=internal` 또는 `scp`에 `internal`이 있는지 확인합니다.
- `iss`와 `aud`가 user-service 설정과 일치하는지 확인합니다.

조치:

- auth-service에서 내부 호출용 JWT를 다시 발급합니다.
- 서비스 간 공유 secret 값을 맞춥니다.

## 가입 API가 400을 반환함

증상:

- `POST /users/signup`이 즉시 실패합니다.

가능한 원인:

- 이메일 형식이 잘못됐습니다.
- 이메일이 비어 있습니다.
- 이미 존재하는 이메일입니다.

확인:

- 요청 body의 `email` 값을 확인합니다.
- `users.email` 중복 여부를 확인합니다.

조치:

- 올바른 이메일로 다시 요청합니다.
- 테스트 환경이면 중복 데이터를 정리합니다.

## 소셜 링크 요청이 충돌함

증상:

- 같은 provider key 요청에서 충돌이 발생합니다.
- 기대한 사용자와 다른 사용자 id가 반환됩니다.

가능한 원인:

- `(social_type, provider_id)` 조합이 이미 다른 사용자에게 연결되어 있습니다.
- provider key 생성 규칙이 서비스 간 다릅니다.
- 이전 요청이 먼저 성공했고 재시도 요청이 뒤늦게 들어왔습니다.

확인:

- `user_social_accounts`에서 `social_type`, `provider_id`를 조회합니다.
- 반환된 `user_id`가 기대 소유자와 같은지 확인합니다.
- auth-service가 전달하는 provider key와 user-service 요청 body가 같은 기준인지 확인합니다.

조치:

- 같은 소유자면 멱등 성공으로 처리합니다.
- 다른 소유자면 소스 IdP 기준으로 소유권을 확인한 뒤 수동 정정합니다.

## 수정 요청이 409를 반환함

증상:

- 사용자 상태 변경 같은 수정 요청이 `409 Conflict`로 실패합니다.

가능한 원인:

- 같은 사용자 또는 소셜 계정이 다른 요청에서 먼저 수정됐습니다.
- 오래된 응답의 `version` 기준으로 후속 수정이 진행됐습니다.

확인:

- 대상 row의 현재 `version`, `modified_at` 값을 다시 조회합니다.
- 같은 대상에 대한 중복 요청 또는 재시도 흐름을 확인합니다.

조치:

- 최신 데이터를 다시 조회한 뒤 수정 요청을 재시도합니다.
- 반복 발생하면 호출 측에서 같은 대상에 대한 병렬 수정을 직렬화합니다.

## DB 연결 실패

증상:

- 애플리케이션 시작 실패
- health check 실패
- `Communications link failure` 또는 인증 실패 로그 발생

가능한 원인:

- MySQL 컨테이너가 healthy 상태가 아닙니다.
- `SPRING_DATASOURCE_URL`이 잘못됐습니다.
- DB 사용자명 또는 비밀번호가 다릅니다.
- Docker network가 맞지 않습니다.

확인:

```bash
./scripts/run.docker.sh ps dev
./scripts/run.docker.sh logs dev mysql
```

조치:

- MySQL 컨테이너 healthcheck 통과 여부를 확인합니다.
- dev 환경에서는 `jdbc:mysql://mysql:3306/user_service...` 형식을 사용합니다.
- 로컬 직접 실행에서는 `localhost:3306`을 사용합니다.

## CI에서 GitHub Packages 의존성 해석 실패

증상:

- Gradle build가 `401 Unauthorized` 또는 package not found로 실패합니다.

가능한 원인:

- 저장소 secret `GH_TOKEN`이 없습니다.
- 토큰에 `read:packages` 권한이 없습니다.
- package 접근 권한이 현재 저장소에 열려 있지 않습니다.

확인:

- GitHub Actions secret에 `GH_TOKEN`이 등록되어 있는지 확인합니다.
- [settings.gradle](../settings.gradle)이 `GH_TOKEN`을 읽는지 확인합니다.
- [.github/workflows/ci.yml](../.github/workflows/ci.yml)이 `GH_TOKEN`을 주입하는지 확인합니다.

조치:

- `read:packages` 권한이 있는 PAT를 `GH_TOKEN`으로 등록합니다.
- package 소유 저장소에서 해당 repo 접근 권한을 허용합니다.

## Docker build가 private package를 받지 못함

증상:

- `./scripts/run.docker.sh up dev` 또는 `build`가 Gradle dependency resolution 단계에서 실패합니다.
- Dockerfile builder stage에서 `401 Unauthorized`가 발생합니다.

가능한 원인:

- `GH_TOKEN`이 Docker build 환경에 전달되지 않았습니다.
- 토큰에 `read:packages` 권한이 없습니다.
- `GITHUB_ACTOR`가 package 접근 권한이 있는 사용자와 다릅니다.

확인:

```bash
echo "$GITHUB_ACTOR"
test -n "$GH_TOKEN"
./scripts/run.docker.sh build dev
```

조치:

- `GITHUB_ACTOR`, `GH_TOKEN`을 shell에 export한 뒤 다시 빌드합니다.
- 토큰 권한과 package 접근 허용 설정을 확인합니다.

## Docker에서 user-service가 gateway에서 보이지 않음

증상:

- gateway가 `http://user-service:8082`로 user-service를 호출하지 못합니다.
- user-service 컨테이너는 떠 있지만 gateway route가 502 또는 connection error를 반환합니다.

가능한 원인:

- gateway와 user-service가 같은 external shared network에 붙어 있지 않습니다.
- `SERVICE_SHARED_NETWORK`, `BACKEND_SHARED_NETWORK`, `MSA_SHARED_NETWORK` 값이 서비스별로 다릅니다.
- shared network가 생성되지 않았습니다.

확인:

```bash
docker network inspect service-backbone-shared
./scripts/run.docker.sh ps dev
```

조치:

- 모든 서비스가 같은 shared network 이름을 사용하도록 맞춥니다.
- 필요한 경우 `./scripts/run.docker.sh up dev`로 네트워크 생성을 유도합니다.

## 서비스 이름 기준 확인

정리:

- GitHub 저장소, PR, Docker service, gateway upstream, 로그와 metric 이름은 모두 `user-service`를 씁니다.
- 계약 도메인 경로는 contract repo의 `contracts/user/**` 기준을 유지합니다.

조치:

- 새 workflow, compose, Terraform, 문서에서 별도 legacy 이름을 만들지 않습니다.
