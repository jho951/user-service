# Admin Backend v1

간단한 **운영자용 어드민 페이지 백엔드(v1)** 레포지토리입니다.  
현재 목표는 **“빠르게 돌려볼 수 있는 최소한의 어드민 API”** 를 만드는 것이고,  
v2부터는 인증·권한, 공통 인프라 모듈(ip-guard, audit-log 등)을 연동할 예정입니다.

---

## ✨ 목표 / 컨셉

- 어드민 페이지에서 사용할 **REST API 백엔드** 제공
- v1에서는 **인증/권한 없이** 내부망/개발환경에서만 사용
- H2 메모리 DB 사용 → 빠르게 개발/테스트
- 계층 구조를 명확히 나눠서 이후 v2, v3…로 자연스럽게 확장 가능

---

## ✅ 현재 v1 기능 범위

> ※ v1은 **개발·내부용** 최소 기능 버전입니다. (실 서비스용 아님)

- 헬스 체크
    - `GET /api/admin/health` → `"OK"`
- 사용자 관리 (Admin User)
    - `GET /api/admin/users` : 사용자 목록 조회
    - `POST /api/admin/users` : 사용자 생성
    - `PUT /api/admin/users/{id}` : 이메일 수정 등 일부 정보 수정
    - `DELETE /api/admin/users/{id}` : 사용자 삭제
- H2 콘솔 제공
    - `/h2-console` (로컬/개발용)

### v2 이후 로드맵(예정)

- Spring Security + JWT 기반 인증/인가
- ip-guard 모듈 연동 (IP 화이트리스트)
- audit-log 모듈 연동 (관리자 액션 감사 로그)
- MySQL 등 외부 DB 연동 및 프로필 분리
- rate-limiter, notification, feature-flag 등 공통 인프라 모듈 순차 적용

---

## 🛠 기술 스택

- **Language**: Java 17
- **Framework**: Spring Boot 3.x
    - `spring-boot-starter-web`
    - `spring-boot-starter-data-jpa`
- **Database (v1)**: H2 (in-memory)
- **Build**: Gradle
- **Container**: Docker, Docker Compose (옵션)

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
    │       │   └─ WebConfig.java           # CORS 등 Web 공통 설정
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
        ├─ application.yml                  # 공통 설정(placeholder)
        └─ application-local.yml            # 로컬(H2) 프로필 설정
