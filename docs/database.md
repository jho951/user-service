# User DB

이 문서는 DB 스키마를 변경할 때 확인해야 하는 구현 기준입니다.

## 테이블 종류

현재 애플리케이션이 직접 관리하는 주요 테이블은 아래 2개입니다.

| 테이블 | 엔티티 | 용도 |
| --- | --- | --- |
| `users` | `User` | 사용자 마스터 데이터, 이메일, role, 상태 |
| `user_social_accounts` | `UserSocial` | 소셜 provider key와 내부 사용자 id 연결 |

감사 로그는 `audit-log` 공통 모듈과 `platform-governance` 감사 API를 통해 기록합니다.

## UUID 바인딩

DB에는 UUID를 표준 하이픈 포함 36자리 문자열로 저장합니다.

```text
550e8400-e29b-41d4-a716-446655440000
```

구현 기준:

- DB type: `CHAR(36)`
- 저장 형식: 표준 UUID 문자열
- Java service/API type: `java.util.UUID`
- Hibernate JDBC type: `SqlTypes.CHAR`
- API path, request, response 모두 표준 UUID 문자열 사용

공통 base entity는 UUID를 `CHAR(36)`으로 저장합니다.

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
@Column(nullable = false, updatable = false, length = 36, columnDefinition = "char(36)")
@JdbcTypeCode(SqlTypes.CHAR)
private UUID id;
```

## `users`

`users` 테이블은 최소 다음 컬럼을 가집니다.

| 컬럼 | 타입 기준 | 설명 |
| --- | --- | --- |
| `user_id` | `CHAR(36)` | primary key |
| `email` | `VARCHAR(191)` | 사용자 이메일, unique |
| `role` | `VARCHAR(20)` | 사용자 role |
| `status` | `CHAR(1)` | 사용자 상태 코드 |
| `version` | integer/long | 낙관적 락 |
| `created_at` | datetime | 생성 시간 |
| `modified_at` | datetime | 수정 시간 |

향후 탈퇴/개인정보 파기 정책이 확정되면 `deleted_at` 컬럼을 추가합니다.
현재는 삭제 상태를 `status = D`로 표현하고, 삭제 시각은 별도 컬럼으로 저장하지 않습니다.

중요 제약:

- `users.email`은 unique입니다.
- 사용자 식별자는 표준 UUID 문자열입니다.
- 주요 엔티티는 `version` 컬럼으로 동시 수정 충돌을 감지합니다.
- 비밀번호는 user-service가 저장하지 않습니다. 인증 책임은 auth-service에 있습니다.

## `user_social_accounts`

`user_social_accounts` 테이블은 최소 다음 컬럼을 가집니다.

| 컬럼 | 타입 기준 | 설명 |
| --- | --- | --- |
| `user_social_id` | `CHAR(36)` | primary key |
| `user_id` | `CHAR(36)` | `users.user_id` 참조 |
| `social_type` | short string | 소셜 제공자 타입 |
| `provider_id` | `VARCHAR(150)` | IdP가 제공한 사용자 식별자 |
| `email` | `VARCHAR(191)` | 연결 당시 이메일 |
| `version` | integer/long | 낙관적 락 |
| `created_at` | datetime | 생성 시간 |
| `modified_at` | datetime | 수정 시간 |

중요 제약:

- `user_social_accounts.user_id`는 `users.user_id`를 참조합니다.
- `(social_type, provider_id)` 조합은 unique입니다.
- 같은 provider key에 대한 반복 요청은 중복 row를 만들면 안 됩니다.

## 상태 코드

상태 코드는 다음 값을 사용합니다.

| DB code | Enum | 설명 |
| --- | --- | --- |
| `A` | `ACTIVE` | 활성 |
| `P` | `PENDING` | 대기 |
| `S` | `SUSPENDED` | 정지 |
| `D` | `DELETED` | 삭제 |

API request와 response에서는 enum 이름을 사용합니다.

DB에는 DB code로 저장합니다.

삭제 정책 메모:

- `is_deleted` 컬럼은 `status = D`와 의미가 중복되므로 추가하지 않습니다.
- 삭제 시각이 필요한 탈퇴 복구, 재가입 제한, 개인정보 파기 배치 요구사항이 생기면 `users.deleted_at`을 추가합니다.
- `deleted_at`은 `User` 엔티티에만 둡니다. 공통 `BaseEntity`에는 두지 않습니다.

## 프로필별 DDL 정책

| 프로필 | DDL 정책 | 용도 |
| --- | --- | --- |
| `dev` | `spring.jpa.hibernate.ddl-auto: update` | 로컬 개발 편의 |
| `prod` | `spring.jpa.hibernate.ddl-auto: none` | 운영 DB는 명시적 schema/migration으로만 변경 |

운영 스키마 변경은 entity 변경만으로 끝내지 않습니다.
실제 DB에 적용할 schema 또는 migration 계획을 별도로 관리합니다.

## 스키마 확인

UUID 컬럼 타입 확인:

```sql
SELECT
  table_name,
  column_name,
  column_type,
  is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('users', 'user_social_accounts')
  AND column_name IN ('user_id', 'user_social_id')
ORDER BY table_name, column_name;
```

기대 결과는 `column_type = char(36)`입니다.

provider key unique 제약 확인:

```sql
SHOW INDEX
FROM user_social_accounts
WHERE Key_name = 'uk_user_social_provider_key';
```

상태 코드 이상값 확인:

```sql
SELECT user_id, status
FROM users
WHERE status NOT IN ('A', 'P', 'S', 'D')
LIMIT 20;
```

결과가 없어야 정상입니다.

## 변경 규칙

DB 변경 시 지켜야 할 기준:

- UUID 컬럼은 entity와 SQL 양쪽에 `CHAR(36)`을 명시합니다.
- `users.email` unique 제약은 유지합니다.
- `(social_type, provider_id)` unique 제약은 유지합니다.
- 운영 중인 테이블 변경이 필요하면 migration SQL 또는 운영 적용 절차를 별도로 둡니다.
- 동시 수정 대상 엔티티에는 `version` 컬럼을 유지합니다.
- 새 public API가 UUID를 주고받으면 `docs/openapi/user-service.yml`도 같이 갱신합니다.
