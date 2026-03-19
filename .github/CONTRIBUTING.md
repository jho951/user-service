# 기여 가이드

안녕하세요! 이 프로젝트에 관심을 가져주셔서 감사합니다.

이 문서는 프로젝트 간단한 안내서입니다.

---
## 📦 프로젝트 세팅

1. 리포지토리를 포크(Fork)하세요
2. 로컬에 클론(Clone)하세요
3. root 레벨 터미널에서 `docker/` 하위 스크립트로 실행할 스택을 선택해 docker compose를 올리세요
4. 종료 시 동일한 스택 기준으로 docker compose를 내리세요
5. 운영 환경 실행 시 `docker/Dockerfile`에서 CMD ["--spring.profiles.active=prod"]로 수정 
6. docker run your-image-name --spring.profiles.active=prod 입력

### docker compose 실행 법
root 레벨에서 아래 명령 중 하나를 사용하세요.

```bash
./docker/start.sh dev all
./docker/start.sh dev app
```

### docker compose 내리는 법
root 레벨에서 아래 명령 중 하나를 사용하세요.

```bash
./docker/shutdown.sh dev all
./docker/shutdown.sh dev app
```

### 아키텍처 의도
포트폴리오 단계에서는 물리 서버를 처음부터 분리하지 않고, `app stack`과 `observability stack`을 Docker Compose 레벨에서 분리했습니다.

- `app stack`: mysql, redis
- `observability stack`: elasticsearch, logstash, kibana

운영 환경에서는 ELK를 별도 서버 또는 별도 클러스터로 분리하는 것을 전제로 합니다.

## ✨ 기여 가능한 항목

- 버그 제보 및 수정
- 기능 추가 또는 개선
- 문서 개선 (README, 주석 등)
- 테스트 코드 추가
- 코드 리팩토링

---



```bash
git clone https://github.com/yourname/your-repo.git
cd your-repo
```

3. 필요한 패키지를 설치하고 빌드하세요

```bash
./gradlew build
```
---

## 🌱 브랜치 전략
본 프로젝트는 <b>GitHub Flow</b> 전략을 채택하고 있습니다.

새로운 기능/수정 작업은 <b>main 브랜치</b>에서 분기해서 작업해주세요:

``` bash
# 새로운 기능
git checkout -b feature/기능명
# 버그 수정
git checkout -b fix/버그명
# 문서 수정
git checkout -b docs/문서명
```
완료된 작업은 반드시 main 브랜치로 <b>PR을 생성하여 코드 리뷰를 받아야 하며</b>, 승인 후 병합됩니다.

## 📮 커밋 메시지 컨벤션
### 종류

feat: | 새로운 기능 추가

fix: | 버그 수정

docs: | 문서 수정

style: | 코드 포맷팅, 세미콜론 등 비기능 수정

refactor: | 코드 리팩토링

test: | 테스트 코드 추가/수정

chore: | 빌드 설정, 패키지 매니저 설정 등

### 예시

``` bash
feat: 게시글 작성 API 추가
fix: 로그인 오류 처리 로직 수정
docs: README에 실행 방법 추가
```

## 🔐 브랜치 보호 및 병합 규칙 안내
### ✏️ 참고
규칙은 GitHub의 Branch protection rules 및 Rulesets 기능 기반으로 설정되어 있습니다.

규칙 위반 시 병합이 차단되며, 관리자에게 요청해야 할 수 있습니다.

### ✅ Pull Request 보내기
1. 모든 변경사항은 반드시 PR을 통해 병합되어야 합니다. (직접 main 브랜치에 push 금지)
2. main 브랜치 기준으로 작업 브랜치를 만들어 주세요
3. 작성 후 main으로 Pull Request를 생성해주세요
4. PR 설명에 변경 내용, 테스트 방법 등을 명확하게 작성해주세요
5. PR은 최소 1명 이상의 승인(Approve) 을 받아야 병합할 수 있습니다.
6. PR에 새로운 커밋을 push하면, 이전에 받은 승인 리뷰는 자동으로 무효화됩니다.
7. 가장 마지막에 push된 커밋은 본인이 아닌 다른 사람의 승인을 받아야 합니다.
8. PR에 남긴 모든 리뷰 코멘트(대화)는 해결된 상태여야 병합할 수 있습니다.
9. 리뷰어가 확인 후 머지됩니다 🎉.

### ✅ 테스트 및 배포 관련 규칙
1. PR은 사전에 설정된 테스트/빌드(Status Checks) 를 모두 통과해야 합니다.
2. 특정 환경(예: production)으로의 배포가 성공해야 PR을 병합할 수 있습니다.
3. 단, 브랜치 생성 시에는 상태 검사가 없어도 생성은 허용됩니다.

### ✅ 커밋 및 병합 방식 관련 규칙
1. 강제 푸시(push --force)는 금지되어 있습니다.
2. Merge commit은 허용되지 않으며, Squash merge 또는 Rebase merge만 사용할 수 있습니다.
3. PR 대상 브랜치는 항상 최신 상태(main과 동기화) 여야 병합할 수 있습니다.

## 🧼 코드 스타일
1. Java 17 기반 (Gradle 프로젝트)
2. IDE에서 자동 포맷팅 권장 (IntelliJ 사용 시 Google Java Style 적용 가능)
3. 가능하면 Lombok 사용 시 @Builder, @Getter, @Slf4j 등 일관성 유지
4. Google Java Style 또는 네이버 핵데이 Java 코딩 컨벤션 권장
5. IntelliJ 사용 시 XML 포맷 적용 가능
6. 개행 문자: LF (\n)

## ❗ 추가 사항
RegexpMultiline 관련 경고 발생 시 naver-checkstyle-suppressions.xml에 아래 설정을 추가하세요.
``` xml
<suppressions>
  <suppress files=".*" checks="RegexpMultiline" />
</suppressions>
```

## 🧾 주석 규칙
### 필수 주석 대상
Service, Repository, Global: JavaDocs 필수

DTO: Request 시 필수 (Constraint 설명 포함)

### 예시
``` java
/**
 * 게시물 관련 Repository입니다.
 * @author 홍길동
 */
@Repository
public interface PostRepository { ... }

/**
 * 게시물 추가 로직입니다.
 * @param post
 * @return 저장된 게시물 ID
 */
public Long createPost(Post post) { ... }
```

## 🔁 응답 구조
공통 응답 구조 사용: <b>BaseResponseWrapper<T></b>

``` java
public class BaseResponseWrapper<T> {...}
```
``` java 
BaseResponseWrapper.ok(SuccessCode.SUCCESS, response);
```

## ⚙️ 클래스 및 메서드 작성 규칙
1. 메서드 네이밍은 동사 + 명사 방식 사용 (예: addNumber() ✅ / numberAdd() ❌)

2. CRUD 메서드는 다음 키워드 사용: create, find, modify, delete
``` java
public class PostCreateRequest(String title, String content) {...}
```

## 🔄 트랜잭션 처리
트랜잭션 어노테이션은 메서드 단위로 적용

복잡한 경우, 팀원과 사전 협의 필수

## 📬 문의
기여 중 문의사항이 있으면 이슈에 자유롭게 남겨주세요!

혹은 이메일: <b>jho951@naver.com</b>
