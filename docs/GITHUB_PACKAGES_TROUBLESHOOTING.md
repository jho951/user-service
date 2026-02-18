# GitHub Packages 인증 트러블슈팅

아래 에러가 나면 인증 정보가 없거나 권한이 부족한 상태입니다.

```text
Could not GET 'https://maven.pkg.github.com/...'
Received status code 401 from server: Unauthorized
```

## 1) 원인

- `GITHUB_ACTOR` / `GITHUB_TOKEN` 미설정
- `gprUser` / `gprKey` 미설정
- PAT 권한 부족 (`read:packages` 없음)
- 다른 계정 토큰 사용 (패키지 접근 권한 없음)
- 조직 리포지토리인 경우 SSO 미승인

## 2) 해결

환경변수 설정:

```bash
export GITHUB_ACTOR=<github-username>
export GITHUB_TOKEN=<github-pat>
```

또는 `~/.gradle/gradle.properties`:

```properties
gprUser=<github-username>
gprKey=<github-pat>
```

그 다음 의존성 캐시를 갱신해서 다시 빌드:

```bash
./gradlew --refresh-dependencies :api:compileJava
```

## 3) 점검 체크리스트

- `settings.gradle` 저장소 URL이 정확한지 확인
  - `https://maven.pkg.github.com/jho951/auth`
- PAT scope에 `read:packages`가 포함됐는지 확인
- 패키지 소유자(`jho951`)에 접근 가능한 계정인지 확인
- PAT 만료/폐기 여부 확인

## 4) 보안 가이드

- 실제 토큰을 레포 파일에 커밋하지 않습니다.
- 로컬/CI Secret 저장소를 사용합니다.
- 토큰 노출 시 즉시 폐기 후 재발급합니다.
