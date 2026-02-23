# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
mvn clean package                          # Build all modules
mvn test                                   # Run all unit tests
mvn -pl <module> test                      # Run tests for a single module
mvn -pl pdf-inbound-api spring-boot:run    # Run the API locally (port 8080)
docker compose -f docker-compose-infra.yml up -d  # Start infrastructure (PostgreSQL, Elasticsearch, Ollama)
mvn verify                                 # Build + generate aggregate JaCoCo coverage report
```

To run a single test class: `mvn -pl <module> -Dtest=ClassName test`
To run a single test method: `mvn -pl <module> -Dtest=ClassName#methodName test`

## Architecture

This is a **multi-module Maven** Spring Boot 3.5.0 application (Java 21) using **hexagonal architecture** (ports & adapters). The system ingests PDFs, runs OCR, enriches text via an LLM, and indexes results for search.

### Modules

| Module | Role |
|--------|------|
| `pdf-core` | Pure domain: models (`Document`, `Page`, `User`), DTOs, events, port interfaces. No Spring dependencies. |
| `pdf-application` | Business logic: service orchestration, async event processors, text extraction/cleaning, MapStruct mappers, custom exceptions |
| `pdf-inbound-api` | REST controllers (`DocumentController`, `AuthController`), global exception handler, Swagger config. **Entry point**: `PdfApiApplication.java` |
| `pdf-infrastructure-security` | Spring Security with stateless JWT auth, BCrypt passwords, token services |
| `pdf-outbound-database` | JPA entities, Spring Data repositories, PostgreSQL adapters implementing core ports. AES encryption for PDF content at rest |
| `pdf-outbound-ocr` | Tesseract OCR adapter (Tess4J) implementing `TextExtractionService` port |
| `pdf-outbound-llm` | Ollama LLM adapter (reactive WebClient) implementing `EnrichmentService` port |
| `pdf-outbound-search` | Elasticsearch adapter implementing `SearchService` port |

### Hexagonal Pattern

- **Outbound ports** are interfaces in `pdf-core/ports/outbound/` (e.g., `DocumentRepository`, `EnrichmentService`, `SearchService`, `TextExtractionService`)
- **Inbound ports** are in `pdf-core/ports/inbound/` (e.g., `AuthenticationService`, `PasswordService`)
- **Adapters** live in the `pdf-outbound-*` and `pdf-infrastructure-*` modules, implementing the core port interfaces
- Package prefix: `org.papercloud.de`

### Document Processing Pipeline (Event-Driven, Async)

```
Upload → save to DB (UPLOADED) → publish OcrEvent
  → OcrEventListener (async) → extract text → save pages (OCR_COMPLETED) → publish EnrichmentEvent
    → EnrichmentEventListener (async) → call LLM → save title/date/tags (ENRICHMENT_COMPLETED) → publish DocumentEnrichedEvent
      → DocumentIndexingListener (async) → index in Elasticsearch
```

- Events are Java records carrying only document IDs; listeners fetch full state from DB
- Status enum on `DocumentPdfEntity`: `UPLOADED → OCR_IN_PROGRESS → OCR_COMPLETED → ENRICHMENT_IN_PROGRESS → ENRICHMENT_COMPLETED` (error states: `OCR_ERROR`, `ENRICHMENT_ERROR`)
- Listeners check document status before processing for idempotency

### Key Conventions

- Implementation classes use `*Impl` suffix
- MapStruct for compile-time DTO/entity mapping (configured alongside Lombok in `maven-compiler-plugin` annotation processors)
- Event publishing via Spring's `ApplicationEventPublisher` with `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`

## Testing

- **Unit tests**: Mockito-based, no Spring context
- **Slice tests**: `@DataJpaTest`, MockMvc with mocked services, custom `@Import` configs for event listeners
- **Integration tests**: Testcontainers for PostgreSQL and Elasticsearch
- **Test config**: `application-test.yml` in `pdf-inbound-api/src/test/resources/` (H2 in-memory DB)
- Detailed strategy in `TESTING_STRATEGY.md`

## Infrastructure Dependencies

- **PostgreSQL 15** (port 5432) — primary data store (dev only)
- **MariaDB 10** (192.168.2.108:3306) — production database (external, not in Docker)
- **Elasticsearch 8.13.0** (port 9200) — full-text search
- **Ollama** (port 11434) — LLM enrichment
- **Tesseract OCR** — must be installed on host or available in Docker image (language data: German + English)

## Production Environment

### Machine Layout

| Machine | IP | Role |
|---|---|---|
| Ubuntu server | 192.168.2.107 | Spring Boot API + ES + Ollama + PGAdmin |
| Synology NAS | 192.168.2.108 | React frontend (Web Station, port 80) |
| MariaDB NAS | 192.168.2.108:3306 | Production database (external) |
| Laptop | — | Development only |

### Deploy the Backend (from laptop)

```bash
./scripts/deploy-prod.sh
```

This builds the JAR locally, SCPs it to the Ubuntu server, and runs `docker compose up --build` remotely.

**Manual steps on the Ubuntu server (if needed):**
```bash
docker compose -f docker-compose-infra.yml up -d   # start infra (creates pdf-backend network)
docker compose -f docker-compose-prod.yml up -d --build   # start app
docker compose -f docker-compose-prod.yml logs -f  # view logs
docker compose -f docker-compose-prod.yml restart  # restart after .env change
```

### Deploy the Frontend (on Synology via SSH)

```bash
cd ~/apps/pdf-management-app/pdf-frontend
git pull
npm run build
cp -r dist/* /volume1/web/
```

Web Station (Apache/Nginx) serves `/volume1/web/` on port 80 automatically — no restart needed.

### Required Files on Ubuntu Server

```
~/pdf-management-app/
├── .env                                         # prod secrets (never committed)
├── docker-compose-infra.yml
├── docker-compose-prod.yml
└── services/Dockerfiles/api/Dockerfile.prod
```

No source code or Maven needed on the server — only the built JAR is shipped.

### Required Files on Synology

```
~/apps/pdf-management-app/pdf-frontend/
├── .env.production.local    # VITE_API_BASE_URL=http://192.168.2.107:8080
├── src/, package.json ...   # full frontend source (git clone)
/volume1/web/
└── index.html + assets/     # built dist files (copied after npm run build)
```

### Environment Variables (.env on Ubuntu server)

| Variable | Description |
|---|---|
| `PDF_AES_SECRET` | AES-256 key — `openssl rand -base64 32` |
| `JWT_SECRET` | JWT signing secret — `openssl rand -base64 64` |
| `DB_USERNAME` | MariaDB username |
| `DB_PASSWORD` | MariaDB password |
| `FRONTEND_URL` | `http://192.168.2.108` (Synology, port 80) — used for CORS |

### Spring Profiles

- `dev` — PostgreSQL in Docker, ES disabled (graceful fail), local Ollama optional
- `prod` — MariaDB external, ES + Ollama via Docker network `pdf-backend`, `ddl-auto: validate`

### Key Production Behaviours

- `ddl-auto: validate` in prod — schema changes must be applied manually to MariaDB before deploying
- ES search fails gracefully if unavailable — uploads and OCR still work
- CORS allows only `FRONTEND_URL` + localhost dev origins
- JWT expires after 30 min — no refresh token yet
- Full guide: `docs/security-and-deployment-guide.md`
