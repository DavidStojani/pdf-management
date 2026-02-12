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

- **PostgreSQL 15** (port 5432) — primary data store
- **Elasticsearch 8.13.0** (port 9200) — full-text search
- **Ollama** (port 11434) — LLM enrichment
- **Tesseract OCR** — must be installed on host or available in Docker image (language data: German + English)
