# Security & Deployment Guide

> Written: 2026-02-21
> Audience: New developers joining the project
> Covers: how security was designed, what is in production today, and how each environment works

---

## Table of Contents

1. [Security Architecture Overview](#1-security-architecture-overview)
2. [PDF Encryption at Rest (AES-GCM)](#2-pdf-encryption-at-rest-aes-gcm)
3. [Authentication & JWT](#3-authentication--jwt)
4. [Password Hashing & User Roles](#4-password-hashing--user-roles)
5. [Rate Limiting](#5-rate-limiting)
6. [CORS Configuration](#6-cors-configuration)
7. [Secrets Management (.env)](#7-secrets-management-env)
8. [Deployment Architecture (Three Machines)](#8-deployment-architecture-three-machines)
9. [Docker Compose Files — What Each One Does](#9-docker-compose-files--what-each-one-does)
10. [Spring Profiles — Dev vs Prod](#10-spring-profiles--dev-vs-prod)
11. [Startup Runbooks](#11-startup-runbooks)
12. [Things to Do Before Going to Production](#12-things-to-do-before-going-to-production)
13. [Key Files Quick Reference](#13-key-files-quick-reference)

---

## 1. Security Architecture Overview

This application handles uploaded PDF files that can contain sensitive content. The security model has multiple independent layers:

| Layer | Technology | What it protects |
|---|---|---|
| PDF content at rest | AES-256-GCM | Raw PDF bytes in the database — unreadable without the key |
| API authentication | JWT (HS256) | Every `/api/documents/**` endpoint requires a valid token |
| Password storage | BCrypt | User passwords are never stored in plain text |
| Brute-force protection | Bucket4j (token bucket) | Login and register endpoints are rate-limited |
| Cross-origin access | Spring CORS config | Only the declared frontend origin can call the API |
| Secrets | Environment variables | No credentials in source code or committed config files |

None of these layers is a substitute for the others. They work together.

---

## 2. PDF Encryption at Rest (AES-GCM)

**Why this exists:** If the database is ever compromised (backup theft, SQL injection, DB admin access), the raw PDF bytes stored in it are useless without the encryption key.

**Implementation:** `pdf-outbound-database/src/main/java/org/papercloud/de/pdfdatabase/config/AESCryptoUtil.java`

**Algorithm:** AES/GCM/NoPadding — authenticated encryption. This means:
- The cipher detects any tampering with the ciphertext (integrity check built in)
- A fresh random 12-byte IV is generated for every single encryption call
- The output format is: `[12-byte IV][ciphertext+tag]` — the IV is prepended to the ciphertext so decryption always has what it needs

**Key material:**
- The key is read from the `PDF_AES_SECRET` environment variable
- It must be a base64-encoded 32-byte value (= AES-256)
- Generate with: `openssl rand -base64 32`

> **Important for new devs:** `PDF_AES_SECRET` must be identical on every machine that needs to read existing encrypted PDFs. If you rotate the key, all previously stored PDFs become unreadable. There is currently no key-rotation mechanism — plan for this before going multi-tenant.

**Data flow:**
```
Upload → controller receives bytes
       → adapter calls AESCryptoUtil.encrypt(bytes) before saving to DB
       → encrypted blob stored in DocumentPdfEntity

Download → fetch encrypted blob from DB
         → call AESCryptoUtil.decrypt(blob) in adapter
         → return plaintext bytes to controller
```

---

## 3. Authentication & JWT

**Implementation files:**
- `pdf-infrastructure-security/src/main/java/org/papercloud/de/pdfsecurity/service/AuthenticationJWTServiceImpl.java`
- `pdf-infrastructure-security/src/main/java/org/papercloud/de/pdfsecurity/filter/JwtRequestFilter.java`
- `pdf-infrastructure-security/src/main/java/org/papercloud/de/pdfsecurity/util/JwtUtil.java`

**How it works:**

1. Client calls `POST /api/auth/login` with `{ email, password }`
2. Spring Security verifies credentials against the DB (BCrypt comparison)
3. On success, a signed JWT is returned
4. Client sends the token in every subsequent request: `Authorization: Bearer <token>`
5. `JwtRequestFilter` intercepts every request, validates the token, and sets the Spring `SecurityContext`
6. If the token is missing or invalid, Spring returns a 401 JSON response (via `JwtAuthenticationEntryPoint`)

**JWT configuration (application.yml):**
```yaml
jwt:
  secret: ${JWT_SECRET}   # HS256 signing secret — must be a strong random value
  expiration: 1800         # 30 minutes — after this the client must log in again
```

**Key material:**
- `JWT_SECRET` env var — generate with: `openssl rand -base64 64`
- The secret is never exposed in responses or logs

> **For new devs:** Token expiry is 30 minutes with no refresh token mechanism yet. If you add a "remember me" or mobile client later, you will want to implement refresh tokens. The infrastructure for it does not exist yet — see `todo.md`.

**Public endpoints** (no JWT required):
- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/documents/ping`

Everything else requires authentication.

---

## 4. Password Hashing & User Roles

Passwords are encoded with BCrypt before being stored. The `PasswordEncoder` bean is configured in `SecurityConfig.java`:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

BCrypt automatically salts each hash — you never need to manage salts manually.

**Roles:** Users get a single role `ROLE_USER` assigned at registration. The role must exist in the `roles` table before the first user registers. Future admin functionality will require adding `ROLE_ADMIN` and protecting endpoints accordingly.

---

## 5. Rate Limiting

**Implementation:** `pdf-infrastructure-security/src/main/java/org/papercloud/de/pdfsecurity/filter/RateLimitingFilter.java`

**Library:** Bucket4j (token bucket algorithm)

**Current limits:** 10 requests per minute per IP address, applied only to:
- `POST /api/auth/login`
- `POST /api/auth/register`

Exceeding the limit returns `429 Too Many Requests`.

> **Why only auth endpoints:** These are the only endpoints vulnerable to brute-force attacks. Authenticated document endpoints are naturally protected by the fact that a valid token is required — an attacker without credentials cannot loop through document requests usefully.

---

## 6. CORS Configuration

**Implementation:** `SecurityConfig.java` — `corsConfigurationSource()` bean

**Current allowed origins:**
- The value of `${app.frontend-url}` (set per Spring profile — see section 10)
- `http://localhost:5173` (Vite default, always allowed for local dev)
- `http://localhost:5137` (alternate Vite port)

For production, `app.frontend-url` resolves to `${FRONTEND_URL}` which is the Synology NAS address (e.g. `http://192.168.2.108:3000`). Set this in your `.env` file on the Ubuntu server.

> **If you add a mobile app or a second frontend origin:** add it to the `setAllowedOrigins` list in `SecurityConfig.java`. Do not use `*` — `allowCredentials(true)` is incompatible with a wildcard origin.

---

## 7. Secrets Management (.env)

All secrets live in a `.env` file at the project root. This file is **gitignored** — it is never committed. Use `.env.example` as the template.

| Variable | Purpose | How to generate |
|---|---|---|
| `PDF_AES_SECRET` | AES-256 key for PDF encryption | `openssl rand -base64 32` |
| `JWT_SECRET` | JWT signing secret | `openssl rand -base64 64` |
| `DB_USERNAME` | Database username | Pick any value |
| `DB_PASSWORD` | Database password | Pick a strong value |
| `PGADMIN_EMAIL` | PGAdmin login (dev/infra only) | Any email |
| `PGADMIN_PASSWORD` | PGAdmin login (dev/infra only) | Any value |
| `FRONTEND_URL` | Allowed CORS origin + prod frontend URL | e.g. `http://192.168.2.108:3000` |

**On the Ubuntu server:** the `.env` is read by both `docker-compose-infra.yml` and `docker-compose-prod.yml`.
**On the laptop:** the same `.env` format is read by `docker-compose-dev.yml`.

Spring Boot also reads `.env` directly at startup via:
```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```
This means env vars work both when running in Docker and when running with `mvn spring-boot:run` locally.

> **Critical rule:** Never put real secrets in `application.yml`, `application-dev.yml`, or any file that gets committed. If you accidentally commit a secret, rotate it immediately.

---

## 8. Deployment Architecture (Three Machines)

```
SYNOLOGY NAS (192.168.2.108)
└── React frontend — npx serve dist, port 3000
    └── calls API → http://192.168.2.107:8080/api/...

UBUNTU SERVER (192.168.2.107)
├── docker-compose-infra.yml  (always running)
│   ├── elasticsearch   :9200  ─────────────┐
│   ├── ollama          :11434 ─────────────┤─ pdf-backend Docker network
│   ├── postgres        :5432  ─────────────┤  (name: pdf-backend, bridge)
│   └── pgadmin         :9090               │
│                                           │
└── docker-compose-prod.yml  (Spring Boot)  │
    └── springboot-app  :8080  ─────────────┘
        → MariaDB at 192.168.2.108:3306 (external, not in Docker)
        → elasticsearch + ollama via Docker hostname on pdf-backend network

LAPTOP (dev)
└── docker-compose-dev.yml  (self-contained)
    ├── springboot-app  :8080
    │   → postgres below via dev-net
    │   → Ollama optional at 192.168.2.107:11434
    │   → ES at localhost:9200 (nothing listening → search fails gracefully)
    └── postgres        (internal only, no exposed port)
        network: dev-net (bridge, isolated)
```

**Key design decisions:**

- **No nginx reverse proxy** — the frontend calls the backend directly by IP. CORS handles cross-origin. This is acceptable for a home network. Add a reverse proxy with TLS if you expose this to the internet.
- **No second Elasticsearch in dev** — search is simply non-functional in dev. OCR and LLM enrichment still work. This avoids running resource-heavy ES on a laptop.
- **MariaDB in production, PostgreSQL in dev** — the prod DB is on a separate NAS (192.168.2.108:3306), not in Docker. Dev uses a containerized Postgres. The JPA dialect is auto-detected; the only change is the JDBC URL and driver class in `application.yml`.
- **Same `.env` variable names across all machines** — only the values differ.

---

## 9. Docker Compose Files — What Each One Does

### `docker-compose-infra.yml` — Ubuntu server, always on
Runs the shared infrastructure that both the app and future services depend on. Creates the stable Docker bridge network named `pdf-backend`. The Spring Boot app (in `docker-compose-prod.yml`) joins this network and reaches ES and Ollama by container hostname.

### `docker-compose-prod.yml` — Ubuntu server, Spring Boot
Runs the production Spring Boot application. Joins the `pdf-backend` network as `external: true` — meaning the network must already exist (created by infra compose). Uses `Dockerfile.prod` which copies the pre-built JAR in with no debug agent and exposes only port 8080.

### `docker-compose-dev.yml` — Laptop, self-contained
Runs Spring Boot + its own Postgres on an isolated `dev-net`. The JAR is volume-mounted from the local `target/` directory — rebuild with `mvn clean package -DskipTests` then restart the container. No `--build` flag needed because the code comes in via the volume, not the image.

### Network topology summary

| Network | Created by | Who joins |
|---|---|---|
| `pdf-backend` (bridge, stable name) | `docker-compose-infra.yml` | infra services + prod Spring Boot |
| `dev-net` (bridge) | `docker-compose-dev.yml` | dev Spring Boot + dev Postgres |

---

## 10. Spring Profiles — Dev vs Prod

Profile is activated via `SPRING_PROFILES_ACTIVE` env var in each compose file.

### `dev` profile
```yaml
spring.datasource.url:  jdbc:postgresql://localhost:5432/pdf_management
                        # overridden by SPRING_DATASOURCE_URL in dev compose
                        # → jdbc:postgresql://postgres:5432/pdf_management

elasticsearch.url:      ${ELASTICSEARCH_URL:http://localhost:9200}
                        # dev compose sets ELASTICSEARCH_URL=http://localhost:9200
                        # nothing is listening → search fails gracefully at runtime

app.frontend-url:       ${FRONTEND_URL:http://localhost:4200}
```

### `prod` profile
```yaml
spring.datasource.url:  jdbc:mariadb://192.168.2.108:3306/pdf_management
spring.datasource.driver-class-name: org.mariadb.jdbc.Driver

elasticsearch.url:      ${ELASTICSEARCH_URL}
                        # prod compose sets: http://elasticsearch:9200
                        # resolves via pdf-backend Docker network

app.frontend-url:       ${FRONTEND_URL}
                        # set in .env on Ubuntu server
                        # e.g. http://192.168.2.108:3000
```

The MariaDB driver (`org.mariadb.jdbc:mariadb-java-client`) is already added to `pdf-outbound-database/pom.xml`. Hibernate dialect is auto-detected — no explicit setting needed.

---

## 11. Startup Runbooks

### Ubuntu server — one-time setup (manual, done once)

The server only needs Docker. No Java, no Maven, no full source code.

Copy these files to the server once:
```
~/pdf-management-app/
├── .env                                        ← prod secrets (fill from .env.example)
├── docker-compose-infra.yml
├── docker-compose-prod.yml
└── services/Dockerfiles/api/Dockerfile.prod
```

Then start the infrastructure:
```bash
cd ~/pdf-management-app
docker compose -f docker-compose-infra.yml up -d
```

### Deploying a new version (from your laptop)

Every subsequent deploy is one command from the project root on your laptop:
```bash
./scripts/deploy-prod.sh
# optionally: ./scripts/deploy-prod.sh user@192.168.2.107
```

The script does: build JAR → scp JAR to server → `docker compose up --build` on the server.

The server never needs source code or Maven.

### Synology NAS — frontend

```bash
# Install Node.js v22 via Synology Package Center first

# Build with the correct API URL pointing at the Ubuntu server
VITE_API_BASE_URL=http://192.168.2.107:8080 npm run build

# Serve (pick one)
npx serve dist -l 3000
# OR
npm run preview -- --host 0.0.0.0 --port 3000
```

The frontend must know where the backend is **at build time** (Vite bakes the URL into the bundle). Always rebuild after changing the API address.

### Laptop — dev

```bash
# Start infra (only postgres is needed; everything else is optional)
docker compose -f docker-compose-dev.yml up -d postgres

# Build the JAR
mvn clean package -DskipTests

# Start the app (volume-mounts the JAR you just built)
docker compose -f docker-compose-dev.yml up -d springboot-app

# After code changes: rebuild JAR, then restart the container
mvn clean package -DskipTests
docker compose -f docker-compose-dev.yml restart springboot-app
```

---

## 12. Things to Do Before Going to Production

These are known gaps — not bugs, but hardening steps skipped intentionally for now.

### Security hardening

- [ ] **TLS / HTTPS** — currently all traffic is plain HTTP on the home LAN. Before exposing the API to the internet, put Nginx or Traefik in front with a TLS certificate. The Spring Boot app itself does not need to handle TLS — let the proxy terminate it.
- [ ] **Refresh tokens** — JWT access tokens expire in 30 minutes with no refresh mechanism. Users get logged out. Implement a refresh token (long-lived, stored in DB, rotated on use) before releasing to end users.
- [ ] **Email verification** — `app.auth.email-verification.enabled=false` in dev. Flip to `true` in prod once SMTP is configured. The code path already exists.
- [ ] **DB user least privilege** — the database user currently likely has full permissions. Create a restricted user for the app: `SELECT, INSERT, UPDATE, DELETE` only on the app tables; no DDL rights in production. Change `ddl-auto` from `update` to `validate` once the schema is stable.
- [ ] **Elasticsearch access control** — ES runs with `xpack.security.enabled=false`. This is fine when ES is on an isolated Docker network. If you ever expose port 9200 externally, enable X-Pack security immediately.
- [ ] **Stack traces in error responses** — verify that 500 errors do not leak stack traces to clients in prod. Spring Boot hides them by default in prod profile but confirm it with a test.

### Operational

- [ ] **Ollama model pre-loaded** — after first `docker compose up`, the Ollama container is empty. SSH into the Ubuntu server and run: `docker exec -it ollama ollama pull <model-name>`. The model name must match what is configured in `application.yml`.
- [ ] **Tesseract language data** — `Dockerfile.prod` installs `tesseract-ocr-eng` and `tesseract-ocr-deu`. If you need additional languages, add them to the Dockerfile.
- [ ] **DB schema initialization** — first startup with `ddl-auto: update` creates tables automatically. When adding `NOT NULL` columns to existing tables, Hibernate will fail on existing rows. Run the column manually first with a `DEFAULT` value before deploying (see the 2026-02-14 session note for the exact pattern).
- [ ] **Elasticsearch index** — deleted for testing with `curl -X DELETE http://192.168.2.107:9200/documents`. The index is recreated on the next document indexing. Do not run this on a system with real data.

---

## 13. Key Files Quick Reference

| What you're looking for | File |
|---|---|
| AES encryption/decryption | `pdf-outbound-database/src/main/java/org/papercloud/de/pdfdatabase/config/AESCryptoUtil.java` |
| JWT filter (validates tokens on every request) | `pdf-infrastructure-security/.../filter/JwtRequestFilter.java` |
| JWT generation and parsing | `pdf-infrastructure-security/.../util/JwtUtil.java` |
| Login / register logic | `pdf-infrastructure-security/.../service/AuthenticationJWTServiceImpl.java` |
| Security rules (which endpoints are public) | `pdf-infrastructure-security/.../config/SecurityConfig.java` |
| CORS allowed origins | `SecurityConfig.java` → `corsConfigurationSource()` |
| Rate limiting | `pdf-infrastructure-security/.../filter/RateLimitingFilter.java` |
| All Spring profile config (dev / prod) | `pdf-inbound-api/src/main/resources/application.yml` |
| Infra compose (ES, Ollama, Postgres, PGAdmin) | `docker-compose-infra.yml` |
| Prod compose (Spring Boot on Ubuntu server) | `docker-compose-prod.yml` |
| Dev compose (Spring Boot + Postgres on laptop) | `docker-compose-dev.yml` |
| Prod Dockerfile (no debug agent, JAR copied in) | `services/Dockerfiles/api/Dockerfile.prod` |
| Dev Dockerfile (Maven + JDK, JAR via volume) | `services/Dockerfiles/api/Dockerfile.dev` |
| Secrets template | `.env.example` |
