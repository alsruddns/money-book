# money-book Development Guidelines

## 1. Project Overview

`money-book` is a household account book service designed for both web and mobile environments.

### Technology Stack

Frontend:

- Next.js
- TypeScript
- PWA
- Responsive / Mobile First UI

Backend:

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- QueryDSL when required
- PostgreSQL
- Flyway

Repository:

- Monorepo

```text
money-book/
├── backend/
├── frontend/
├── docs/
└── AGENTS.md
```

---

# 2. General Development Principles

- Prefer simple and explicit code over unnecessary abstraction.
- Do not introduce a new library without a clear reason.
- Do not modify unrelated code while implementing a feature.
- Follow the existing project conventions before introducing a new convention.
- Security-sensitive features such as authentication and authorization must be validated on the backend.
- Frontend permission checks are only for UI control. Backend permission checks are mandatory.
- Do not store secrets, passwords, tokens, API keys, or OAuth credentials in Git.
- All new code must be written so that future deployment environments can be configured through external configuration or environment variables.

---

# 3. Backend Package Convention

Base package:

```text
com.moneybook.backend
```

Common top-level package structure:

```text
com.moneybook.backend
├── config
├── entity
├── enums
├── common
│   ├── exception
│   └── response
│
├── auth
│   ├── controller
│   ├── dto
│   ├── service
│   │   └── impl
│   ├── repository
│   │   └── impl
│   └── provider
│
├── dashboard
│   ├── controller
│   ├── dto
│   ├── service
│   │   └── impl
│   ├── repository
│   │   └── impl
│   └── provider
│
├── calendar
│   ├── controller
│   ├── dto
│   ├── service
│   │   └── impl
│   ├── repository
│   │   └── impl
│   └── provider
│
└── ...
```

`provider` is optional and must only be created when orchestration between multiple services or domains is required.

---

# 4. Package Responsibilities

## config

Application configuration belongs here.

Examples:

```text
SecurityConfig
JpaConfig
QueryDslConfig
CorsConfig
```

Do not place business logic in `config`.

---

## entity

All JPA entities belong in the common `entity` package.

Example:

```text
entity/
├── User.java
├── MoneyBook.java
├── MoneyBookUser.java
└── Transaction.java
```

Entities must represent persistence state and must not contain service orchestration logic.

Avoid public setters unless there is a strong reason.

Prefer meaningful domain methods for state changes.

Example:

```java
transaction.changeAmount(amount);
```

instead of:

```java
transaction.setAmount(amount);
```

---

## enums

Common application enums belong here.

Examples:

```text
AuthProvider
TransactionType
PermissionType
UserStatus
```

Use enums rather than magic strings where appropriate.

---

## common.exception

Common exception handling belongs here.

Example:

```text
BusinessException
ErrorCode
GlobalExceptionHandler
```

Business exceptions should use a consistent error response format.

---

## common.response

Common API response objects belong here.

Examples:

```text
ApiResponse
ErrorResponse
PageResponse
```

API responses should follow one consistent structure across the project.

---

# 5. Business Package Convention

Each business domain is separated into its own package.

Example:

```text
dashboard/
calendar/
transaction/
moneybook/
user/
auth/
```

Each business package should normally contain:

```text
controller
dto
service
repository
```

Optional:

```text
provider
```

Do not create unnecessary packages before they are required.

---

# 6. Controller Rules

Controllers are responsible for:

- HTTP request mapping
- Request DTO validation
- Calling Service or Provider
- Returning response DTOs

Controllers must not contain business logic.

Bad:

```java
@PostMapping
public ResponseEntity<?> create(...) {
    // complex calculation
    // repository access
    // permission calculation
}
```

Good:

```java
@PostMapping
public ResponseEntity<?> create(...) {
    return ResponseEntity.ok(
        transactionService.create(request)
    );
}
```

Controllers must never access Repository directly.

---

# 7. DTO Rules

Request and response DTOs must be separated.

Recommended naming:

```text
CreateTransactionReqDto
CreateTransactionResDto

TransactionDetailResDto
TransactionListResDto
```

Do not expose Entity objects directly through APIs.

Prefer Java `record` for immutable DTOs when appropriate.

Example:

```java
public record TransactionCreateReqDto(
        Long moneyBookUid,
        BigDecimal amount,
        TransactionType type
) {
}
```

---

# 8. Service Rules

Service must always use:

```text
Interface
→ Implementation
```

Example:

```text
service/
├── TransactionService.java
└── impl/
    └── TransactionServiceImpl.java
```

Interface:

```java
public interface TransactionService {

    TransactionDetailResDto create(
            TransactionCreateReqDto request
    );
}
```

Implementation:

```java
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl
        implements TransactionService {
}
```

---

# 9. Service-to-Service Calls Are Prohibited

A Service must never directly call another Service.

Forbidden:

```java
@Service
public class DashboardServiceImpl {

    private final TransactionService transactionService;
    private final CalendarService calendarService;
}
```

This rule exists to prevent:

- Circular dependencies
- Strong coupling between business domains
- Business logic becoming difficult to trace
- Service classes becoming orchestration layers

---

# 10. Provider Rule

When multiple Services must be combined, introduce a higher orchestration layer called `Provider`.

Structure:

```text
Controller
    ↓
Provider
    ↓
Service A
Service B
Service C
    ↓
Repository
```

Example:

```java
@Component
@RequiredArgsConstructor
public class DashboardProvider {

    private final TransactionService transactionService;
    private final CalendarService calendarService;

}
```

A Provider may call multiple Services.

A Service must not call another Service.

Use Provider only when orchestration is actually required.

Simple functionality should remain:

```text
Controller
→ Service
→ Repository
```

Do not create Provider unnecessarily.

---

# 11. Repository Rules

Repository follows:

```text
Interface
→ Implementation
```

Example:

```text
repository/
├── TransactionRepository.java
└── impl/
    └── TransactionRepositoryImpl.java
```

Business logic must not be placed in Repository.

Repository responsibilities:

- Database queries
- Persistence
- QueryDSL queries
- Data retrieval

Service responsibilities:

- Business validation
- Business rules
- Transaction boundaries

---

# 12. Transaction Rules

Use `@Transactional` at the Service implementation method level where possible.

Example:

```java
@Transactional
@Override
public TransactionDetailResDto create(...) {
}
```

Read-only operations should use:

```java
@Transactional(readOnly = true)
```

Avoid unnecessarily applying transactions to an entire class.

---

# 13. Database Migration

Database schema is managed by Flyway.

Hibernate must not modify production schema automatically.

Recommended:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Migration directory:

```text
backend/src/main/resources/db/migration
```

Flyway naming convention:

```text
VyyyyMMdd_N__description.sql
```

Examples:

```text
V20261001_1__create_users.sql
V20261001_2__create_user_auth.sql
V20261002_1__create_money_book.sql
```

Rules:

- Start sequence number from `1` for each date.
- Never modify a migration that has already been applied.
- Schema changes require a new migration.
- CREATE TABLE, ALTER TABLE, indexes and constraints must be managed through Flyway.

---

# 14. Gradle Dependency Rule

Every dependency added to `build.gradle` must include a comment explaining its purpose.

Bad:

```gradle
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-database-postgresql'
```

Good:

```gradle
// DB schema versioning and migration management
implementation 'org.flywaydb:flyway-core'

// PostgreSQL support for Flyway migrations
implementation 'org.flywaydb:flyway-database-postgresql'
```

Do not add unused dependencies.

---

# 15. Money Book Authorization Model

Money books are shared by multiple users.

A money book has one original owner.

Permissions:

```text
O = Administrator
C = Create
R = Read
U = Update
D = Delete
```

The original owner must retain full permissions.

`O` represents administrative capabilities such as:

- Adding users
- Removing users
- Managing user permissions
- Managing money book settings

Backend authorization is mandatory.

Frontend authorization is only for controlling visible UI.

---

# 16. Git Branch Strategy

Branches:

```text
main
release
develop
feature/*
```

Meaning:

```text
main
→ Production

release
→ Staging

develop
→ Development integration

feature/*
→ Individual feature development
```

Development flow:

```text
feature/*
    ↓
develop
    ↓
release
    ↓
main
```

Do not develop features directly on:

```text
main
release
develop
```

Always create a feature branch from the latest `develop`.

Example:

```bash
git checkout develop
git pull
git checkout -b feature/auth
```

Recommended feature branch examples:

```text
feature/init-backend
feature/auth
feature/money-book
feature/transaction
feature/dashboard
feature/calendar
```

---

# 17. Git Commit Convention

Commit messages follow:

```text
type: description
```

Supported types:

```text
feat      New feature
fix       Bug fix
refactor  Code restructuring without behavior change
docs      Documentation
chore     Project/configuration maintenance
test      Test changes
perf      Performance improvement
build     Build/dependency changes
ci        CI/CD changes
```

Examples:

```text
feat: add money book creation API
fix: prevent duplicate user invitations
refactor: separate transaction query logic
docs: add backend development conventions
chore: initialize backend project
build: add Flyway dependencies
```

A commit should represent one logical change.

Do not combine unrelated modifications into one commit.

---

# 18. Push and Pull Request Rules

Before starting work:

```bash
git checkout develop
git pull
```

Create feature branch:

```bash
git checkout -b feature/<feature-name>
```

After implementation:

```bash
git status
git add .
git commit -m "type: description"
git push -u origin feature/<feature-name>
```

Then create a Pull Request:

```text
feature/*
→ develop
```

After development verification:

```text
develop
→ release
```

After staging verification:

```text
release
→ main
```

---

## Codex 작업 규칙

Codex는 코드를 수정하기 전에 반드시 다음 규칙을 따른다.

1. 작업을 시작하기 전에 반드시 이 `AGENTS.md` 파일을 읽는다.
2. 기존 패키지 구조와 코딩 컨벤션을 먼저 확인한다.
3. 요청된 작업과 관련 없는 코드는 수정하지 않는다.
4. 아키텍처나 구조에 큰 영향을 주는 변경은 가능한 경우 구현 전에 변경 이유와 방향을 설명한다.
5. Service와 Repository는 반드시 `Interface -> Impl` 구조를 따른다.
6. Service에서 다른 Service를 직접 호출하지 않는다.
7. 여러 Service의 조합이 필요한 경우 상위 `Provider` 레이어에서 조합 로직을 구현한다.
8. `build.gradle`에 새로운 dependency를 추가할 때는 반드시 해당 dependency의 용도를 설명하는 주석을 함께 작성한다.
9. Flyway migration 파일명 규칙인 `VyyyyMMdd_N__description.sql` 형식을 따른다.
10. 이미 적용된 Flyway migration 파일은 절대 수정하지 않는다.
11. 비밀번호, 토큰, API Key, OAuth Secret 등 민감정보를 Git에 커밋하지 않는다.
12. 프로젝트의 Git 브랜치 전략과 커밋 컨벤션을 유지한다.
13. 모든 API Controller 클래스에는 해당 Controller의 역할을 설명하는 주석을 작성한다.
14. 모든 API Endpoint 메서드에는 해당 API의 기능과 목적을 설명하는 주석을 작성한다.
15. 요청/응답 구조, 권한 조건, 중요한 비즈니스 조건이 있는 API는 필요한 내용을 주석에 함께 명시한다.
16. 주요 Service 및 Repository 메서드도 동작 의도가 명확하지 않은 경우 설명 주석을 작성한다.

When a requested implementation conflicts with this document, this document takes priority unless the developer explicitly requests a convention change.

## API Comment Rules

- 모든 API Controller 클래스에는 해당 Controller의 역할을 설명하는 주석을 작성한다.
- 모든 API Endpoint 메서드에는 해당 API의 기능과 목적을 설명하는 주석을 작성한다.
- 요청/응답이나 권한 조건이 중요한 경우 주석에 함께 명시한다.
- 단순한 코드 내용을 그대로 반복하는 무의미한 주석은 피한다.
- 주요 Service / Repository 메서드에도 동작 의도가 바로 드러나지 않는 경우 설명 주석을 작성한다.

Example:

````java
/**
 * 가계부 거래내역 API를 제공한다.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    /**
     * 특정 가계부에 새로운 수입 또는 지출 거래를 등록한다.
     *
     * @param request 거래 등록 정보
     * @return 등록된 거래 정보
     */
    @PostMapping
    public ApiResponse<TransactionCreateResDto> create(
            @Valid @RequestBody TransactionCreateReqDto request
    ) {
        return ApiResponse.success(
                transactionService.create(request)
        );
    }
}


## Git 커밋 컨벤션

커밋 메시지는 아래 형식을 사용한다.

```text
Type : [scope] 작업 내용
````

### Type

- `Add` : 신규 기능, 신규 API, 신규 화면, 신규 파일을 추가한 경우
- `Update` : 기존 기능 또는 기존 소스의 동작을 수정한 경우
- `Fix` : 버그 또는 오류를 수정한 경우
- `Refactor` : 기존 기능의 목적은 유지하지만 구조나 구현 방식을 크게 변경한 경우
- `Delete` : 기존 기능, 코드, 파일을 제거한 경우
- `Docs` : 문서 작성 또는 수정
- `Config` : Gradle, 환경설정, 의존성, 프로젝트 설정 등을 변경한 경우
- `Test` : 테스트 코드를 추가하거나 수정한 경우
- `Perf` : 성능 개선 작업

### Scope

`scope`에는 변경 대상 업무 또는 영역을 작성한다.

예시:

```text
[auth]
[user]
[moneybook]
[transaction]
[dashboard]
[calendar]
[common]
[backend]
[frontend]
[db]
```

### 예시

```text
Add : [calendar] 월별 캘린더 조회 API 추가

Update : [dashboard] 월별 지출 집계 로직 수정

Fix : [transaction] 이체 거래가 지출로 집계되는 오류 수정

Refactor : [auth] 로그인 인증 구조 전면 개편

Delete : [moneybook] 사용하지 않는 사용자 권한 API 제거

Docs : [common] 프로젝트 개발 규칙 문서 추가

Config : [backend] Flyway 의존성 및 설정 추가

Perf : [dashboard] 월별 통계 조회 쿼리 개선
```

### 작성 규칙

- 커밋 메시지는 가능하면 한글로 작성한다.
- 하나의 커밋에는 하나의 논리적인 변경만 포함한다.
- 서로 관련 없는 작업을 하나의 커밋에 섞지 않는다.
- 작업 내용은 `추가`, `수정` 같은 단순 표현보다 실제 변경 내용을 알 수 있게 작성한다.
- 기능 개발 브랜치에서 작업 후 `develop`으로 병합한다.

## 검증 규칙

- 모든 작업에서 무조건 전체 테스트를 실행하지 않는다.
- 변경 범위에 맞는 최소 검증을 수행한다.
- 문서/주석 변경은 테스트를 생략할 수 있다.
- 설정 및 의존성 변경은 compile 또는 build로 검증한다.
- DB/Flyway 변경은 애플리케이션 기동과 migration 적용 여부를 확인한다.
- API 변경은 해당 API의 정상 응답을 확인한다.
- 비즈니스 로직 변경은 관련 테스트를 우선 수행한다.
- 대규모 리팩토링 또는 공통 모듈 변경 시 전체 build/test를 수행한다.
