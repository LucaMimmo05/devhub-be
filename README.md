# DevHub — Backend

> REST API for the DevHub developer dashboard. Built with Quarkus 3, Java 21 and PostgreSQL.

---

## Tech Stack

| Layer | Library / Tool |
|---|---|
| Framework | Quarkus 3.30.5 |
| Language | Java 21 |
| ORM | Hibernate ORM + Panache |
| Database | PostgreSQL (JDBC) |
| Auth | SmallRye JWT (RS256, PEM keys) |
| Session store | Redis (OTP + token blacklist) |
| Email | Quarkus Mailer (Gmail SMTP) |
| Validation | Hibernate Validator |
| Scheduler | Quarkus Scheduler (JWT cleanup) |

---

## Project Structure

```
src/main/java/com/devhub/
├── auth/
│   ├── controller/     # AuthController, EmailController
│   ├── dto/            # Login/Register/Reset request & response DTOs
│   ├── service/        # AuthService, EmailService, OtpService
│   └── util/           # CookieUtil (HttpOnly cookie helpers)
├── command/            # CLI snippet CRUD
├── common/
│   ├── controller/     # HealthController
│   ├── entity/         # BaseEntity (id, createdAt, updatedAt)
│   ├── enums/          # Priority, Status, ProjectRole, UserRole
│   └── error/          # Global exception mappers
├── note/               # Markdown note CRUD
├── project/            # Project CRUD + member management
├── security/
│   └── jwt/            # JwtService, RefreshToken entity, cleanup scheduler
├── task/               # Task CRUD
└── user/               # User profile CRUD + search
```

---

## API Reference

All authenticated endpoints require a valid `access_token` HttpOnly cookie obtained at login.

### Auth — `/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | No | Create account |
| POST | `/auth/login` | No | Login → sets JWT cookies |
| POST | `/auth/refresh` | No | Rotate access token |
| POST | `/auth/logout` | Yes | Revoke tokens |
| POST | `/auth/forgot-password` | No | Send OTP to email |
| POST | `/auth/verify-reset-otp` | No | Validate OTP |
| POST | `/auth/reset-password` | No | Set new password |
| POST | `/auth/resend-reset-otp` | No | Resend OTP |

### Email — `/email`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/email/verify` | No | Verify email with OTP |
| POST | `/email/resend` | No | Resend verification OTP |

### Projects — `/projects`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/projects` | Yes | List user's projects |
| POST | `/projects` | Yes | Create project |
| GET | `/projects/{id}` | Yes | Get project by ID |
| PUT | `/projects/{id}` | Yes | Update project |
| DELETE | `/projects/{id}` | Yes | Delete project |
| POST | `/projects/{id}/members` | Yes | Add member |
| DELETE | `/projects/{id}/members/{profileId}` | Yes | Remove member |
| GET | `/projects/{id}/tasks` | Yes | List tasks for a project |

### Tasks — `/tasks`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/tasks` | Yes | List user's tasks |
| POST | `/tasks` | Yes | Create task |
| PUT | `/tasks/{id}` | Yes | Update task |
| DELETE | `/tasks/{id}` | Yes | Delete task |

### Notes — `/notes`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/notes` | Yes | List user's notes |
| POST | `/notes` | Yes | Create note |
| PUT | `/notes/{id}` | Yes | Update note |
| DELETE | `/notes/{id}` | Yes | Delete note |

### Commands — `/commands`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/commands` | Yes | List user's CLI commands |
| POST | `/commands` | Yes | Create command |
| PUT | `/commands/{id}` | Yes | Update command |
| DELETE | `/commands/{id}` | Yes | Delete command |

### Users — `/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/users/me` | Yes | Get current user profile |
| PUT | `/users/me` | Yes | Update profile |
| DELETE | `/users/me` | Yes | Delete account |
| GET | `/users/search` | Yes | Search users by username |

### Health — `/health`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/health` | No | Liveness check |

---

## Getting Started

### Prerequisites
- Java 21
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- PostgreSQL running locally
- Redis running locally

### Environment Variables

Copy and fill in the following variables (e.g. in `.env` or shell):

```env
# Database
POSTGRES_USER=devhub
POSTGRES_PASSWORD=secret
POSTGRES_DB=devhub
POSTGRES_PORT=5432        # optional, defaults to 5432

# JWT (dev profile reads from .pem files; production reads from env vars)
JWT_PUBLIC_KEY=<contents of publicKey.pem>
JWT_PRIVATE_KEY=<contents of privateKey.pem>

# Redis
REDIS_HOST=redis://localhost:6379
REDIS_PASSWORD=

# Email (Gmail SMTP)
MAIL_USERNAME=you@gmail.com
MAIL_PASSWORD=<Gmail App Password>

# CORS
FRONTEND_ORIGIN=http://localhost:5173
```

> **Dev profile**: place `publicKey.pem` and `privateKey.pem` in `src/main/resources/` — they are read automatically in dev mode.

### Run in development mode

```bash
./mvnw quarkus:dev
```

The API will be available at `http://localhost:8080`.  
Quarkus Dev UI: `http://localhost:8080/q/dev`.

### Build (JVM)

```bash
./mvnw -B clean package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

### Build (native — optional)

```bash
./mvnw package -Dnative
./target/devhub-be-1.0-SNAPSHOT-runner
```

---

## Deployment (Render)

1. Create a new **Web Service** on [render.com](https://render.com) pointing to this repo.
2. **Build command:** `./mvnw -B clean package -DskipTests`
3. **Start command:** `java -jar target/quarkus-app/quarkus-run.jar`
4. Add all environment variables from the table above.  
   `PORT` is injected automatically by Render — Quarkus picks it up via `quarkus.http.port=${PORT:8080}`.
5. After the frontend is deployed, update `FRONTEND_ORIGIN` to the Vercel URL and redeploy.

### Recommended free-tier services

| Service | Provider | Notes |
|---|---|---|
| PostgreSQL | [Neon](https://neon.tech) | Free tier, no expiry |
| Redis | [Upstash](https://upstash.com) | Free tier, TLS included |
| SMTP | Gmail App Password | Free |

---

## Environment Variables Reference

| Variable | Description |
|---|---|
| `PORT` | HTTP port (Render injects this automatically) |
| `FRONTEND_ORIGIN` | Allowed CORS origin (e.g. `https://yourapp.vercel.app`) |
| `JDBC_DATABASE_URL` | Full JDBC connection string (production only) |
| `POSTGRES_USER` | Database username |
| `POSTGRES_PASSWORD` | Database password |
| `POSTGRES_DB` | Database name (dev only) |
| `REDIS_HOST` | Redis connection URI (e.g. `rediss://host:port`) |
| `REDIS_PASSWORD` | Redis password |
| `MAIL_USERNAME` | Gmail address used as SMTP sender |
| `MAIL_PASSWORD` | Gmail App Password |
| `JWT_PUBLIC_KEY` | RSA public key PEM content (production) |
| `JWT_PRIVATE_KEY` | RSA private key PEM content (production) |

---

## Authors

Crafted by [Luca Mimmo](https://github.com/LucaMimmo05) × Claude (Anthropic).
