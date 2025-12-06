# Admin Backend v1

간단한 **운영자용 어드민 페이지 백엔드(v1)** 레포지토리입니다.  
현재 목표는 “**빠르게 돌려볼 수 있는 최소한의 어드민 API**”를 만드는 것이며,  
이후 v2부터는 공통 인프라 모듈(ip-guard, audit-log 등)과 연동할 예정입니다.

---

## 🧩 목표

- 어드민 페이지에서 사용할 **REST API 백엔드** 제공
- 사용자 관리 등 기본 도메인 CRUD 제공
- 로컬 환경 기준 **H2 메모리 DB**로 빠르게 개발 가능
- 계층 구조를 명확히 나눠서 이후 확장(v2, v3…)에 대비

---

## ✅ v1 기능 범위

> v1은 **인증/권한 없이 내부 관리자만 사용한다**는 가정의 최소 기능 버전입니다.

- `/api/admin/health` 헬스 체크
- `/api/admin/users` 사용자 관리
    - 사용자 생성 (username, password, email)
    - 사용자 목록 조회
    - 사용자 이메일 수정
    - 사용자 삭제
- H2 콘솔을 통한 데이터 확인 (`/h2-console`)
- 단일 애플리케이션 (Spring Boot) + 레이어드 아키텍처

**v2 이후에 추가 예정인 것들(로드맵)**

- Spring Security + JWT 기반 어드민 로그인
- ip-guard 모듈 연동 (IP 화이트리스트)
- audit-log 모듈 연동 (관리자 액션 감사 로그)
- rate-limiter, notification 등 공통 인프라 모듈 점진적 연결

---

## 🛠 기술 스택

- Java 17
- Spring Boot 3.x
    - spring-boot-starter-web
    - spring-boot-starter-data-jpa
- H2 Database (로컬 개발용)
- Gradle

---

## 📁 프로젝트 구조

> 패키지 기준: `com.admin`

```text
src
 └─ main
    ├─ java
    │   └─ com.admin
    │       ├─ AdminApplication.java        # 진입점
    │       ├─ config
    │       │   └─ WebConfig.java           # CORS 등 공통 Web 설정
    │       ├─ domain
    │       │   └─ User.java                # 어드민 사용자 엔티티
    │       ├─ repository
    │       │   └─ UserRepository.java      # JPA 리포지토리
    │       ├─ service
    │       │   └─ UserService.java         # 도메인 비즈니스 로직
    │       └─ controller
    │           ├─ UserAdminController.java # /api/admin/users
    │           └─ HealthController.java    # /api/admin/health
    └─ resources
        ├─ application.yml
        └─ application-local.yml            # 로컬 프로필(H2) 설정
