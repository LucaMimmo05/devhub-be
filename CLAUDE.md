# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

DevHub Backend — REST API for a developer dashboard (projects, tasks, notes, CLI snippets) with JWT auth. Quarkus 3.30.5, Java 21, PostgreSQL (Hibernate ORM + Panache), Redis (OTP + session support), SmallRye JWT.

## Commands

```bash
# Dev mode (live reload) — requires Postgres + Redis reachable (see docker-compose.yml for local Postgres)
./mvnw quarkus:dev

# Run tests
./mvnw test

# Run a single test class / method
./mvnw test -Dtest=ExampleResourceTest
./mvnw test -Dtest=ExampleResourceTest#testMethodName

# Build (JVM)
./mvnw -B clean package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar

# Native build (optional)
./mvnw package -Dnative
```

Local Postgres only: `docker-compose up -d` (starts `postgres:17` on 5432, db/user/pass all `devhub`). Redis is not included in docker-compose and must be provided separately (e.g. local install or a hosted instance) — `REDIS_HOST` etc. are required even in dev.

Required env vars are documented in `README.md` (Environment Variables section) — `POSTGRES_*`, `REDIS_HOST`/`REDIS_PASSWORD`, `MAIL_USERNAME`/`MAIL_PASSWORD`, `JWT_PUBLIC_KEY`/`JWT_PRIVATE_KEY` (prod only — dev reads `publicKey.pem`/`privateKey.pem` from `src/main/resources/`), `FRONTEND_ORIGIN`.

There is no linter/formatter configured in this repo.

## Architecture

### Package-by-feature layout

Code under `src/main/java/com/devhub/` is organized by domain feature, not by technical layer: `auth`, `command`, `note`, `project`, `task`, `user`, plus `common` (shared base entity, enums, global exception mappers) and `security/jwt` (JWT issuance, refresh-token persistence, cleanup scheduler). Each feature package follows the same internal structure: `controller/` (JAX-RS resources), `dto/` (request/response records/classes), `service/` (`@ApplicationScoped`, `@Transactional` on mutating methods), `entity/` (Panache entities), `repository/` (`PanacheRepositoryBase` implementations, custom finder methods). When adding a new feature, mirror this structure rather than introducing a layered (`controllers/`, `services/`...) top-level split.

### Auth model: `User` vs `UserProfile`

Two entities represent an account, joined 1:1:
- `user/entity/User.java` — auth identity: email, username, `passwordHash`, `role` (`UserRole`), `isEmailVerified`, `isActive`. This is what JWT `sub` and `userId` claims refer to.
- `user/entity/UserProfile.java` — the domain-facing profile (first/last name, avatar, username) that owns projects, tasks, notes, and commands via `@OneToMany` back-references. All feature entities (`Task`, `Note`, `Command`, `ProjectMember`) point at `UserProfile`, not `User`.

Controllers pull the authenticated `User.id` out of `SecurityContext.getUserPrincipal().getName()` (a UUID string set as JWT subject), then look up the corresponding `UserProfile` via `userProfileRepository.find("user.id", userId)` before doing anything domain-specific. Follow this same pattern in new authenticated endpoints rather than trusting a profile ID from the request body.

### JWT + refresh token flow

- Access tokens are short-lived (4h), signed by `JwtService.generateAccessToken`, carrying `sub` = user UUID, a `groups` claim with the role, and a `userId` claim.
- Refresh tokens are opaque UUIDs persisted in Postgres (`security/jwt/entity/RefreshToken.java`, 30-day expiry), not JWTs. `AuthService.login`/`refresh` delete-then-reissue refresh tokens (single active refresh token per user). `JwtCleanupService` purges expired ones nightly via `@Scheduled`.
- Both tokens are delivered as HttpOnly cookies (`auth/util/CookieUtil.java`); `access_token` is read by `quarkus-smallrye-jwt`, `refreshToken` is read manually by the refresh/logout endpoints. `secure(false)` is currently hardcoded on cookies — check this before changing cookie behavior for prod (HTTPS) correctness.
- Path-based auth is enforced in `application.properties` (`quarkus.http.auth.permission.authenticated.paths=/projects/*,/tasks/*,/notes/*,/users/*`) in addition to (or instead of) `@Authenticated` on individual controllers — check both when adding a new protected feature package, since a new top-level path won't be covered by the properties-based rule until added there.

### Authorization pattern in services

There's no central permission/ACL layer — each service method inline-checks access, typically "is creator" or "is project owner", e.g. `TaskService.updateTask`/`deleteTask` compare `task.createdBy.user.id` / `task.project.owner.user.id` against the requesting user before mutating, throwing `ForbiddenException`/`NotFoundException` (JAX-RS) directly from the service layer. Follow this inline-check style for new mutating endpoints rather than introducing a separate authorization abstraction.

### Error handling

Global JAX-RS `ExceptionMapper`s in `common/error/` translate exceptions to JSON bodies: `AuthExceptionMapper` (custom `AuthException` → 401, `{type: "AUTH_ERROR", message}`), `GlobalExceptionMapper` (Bean Validation `ConstraintViolationException` → 400, `{type: "VALIDATION_ERROR", errors: {field: message}}`), `WebApplicationExceptionMapper` (any `WebApplicationException`, e.g. `NotFoundException`/`ForbiddenException` → its status code, `{message, status}`). Raise these standard exception types from services/controllers rather than building `Response` objects by hand for error cases.

### OTP / email verification

`auth/service/OtpService.java` generates 6-digit OTPs, bcrypt-hashes them, and stores the hash in Redis under `otp:{email}:{type}` with a 5-minute TTL (`type` distinguishes flows, e.g. email verification vs password reset). `EmailService` sends the OTP via Quarkus Mailer (Gmail SMTP) using templates in `src/main/resources/templates/`.

### Entities

All entities extend `common/entity/BaseEntity.java` (`@MappedSuperclass`): UUID primary key (`GenerationType.UUID`), `createdAt`/`updatedAt` auto-managed via Hibernate `@CreationTimestamp`/`@UpdateTimestamp`. Shared enums live in `common/enums/` (`Status`, `Priority`, `ProjectRole`, `UserRole`) and are persisted as `EnumType.STRING`.

Hibernate schema is auto-migrated (`quarkus.hibernate-orm.schema-management.strategy=update`) — there is no separate migration tool (no Flyway/Liquibase). `src/main/resources/import.sql` is present but currently inert (fully commented out).

### Profile bootstrap quirk

`AuthService.login`/`refresh` both contain a self-healing check that backfills `UserProfile.username` from `User.username` if it's ever null/blank — a defensive patch for profiles created before `username` was added to `UserProfile`. Keep this in mind if refactoring the login/refresh path; it's not dead code.
