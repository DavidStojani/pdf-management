# Repository Guidelines

## Project Structure & Module Organization
This is a multi-module Maven build. Active modules are listed in the root `pom.xml`:
- `pdf-core`: shared domain models, DTOs, and events.
- `pdf-application`: orchestration/business logic (ingestion, OCR, enrichment).
- `pdf-inbound-api`: REST API and web-layer configuration.
- `pdf-infrastructure-security`: Spring Security configuration.
- `pdf-outbound-database`: persistence adapters and repositories.
- `pdf-outbound-ocr`: OCR adapters and strategies.
- `pdf-outbound-llm`: LLM enrichment adapters.
- `pdf-outbound-search`: Elasticsearch/search adapters.

Standard Maven layout is used (`src/main/java`, `src/test/java`, `src/test/resources`). Docker configs live at the repo root (`docker-compose*.yml`).

## Build, Test, and Development Commands
- `mvn clean package`: build all modules.
- `mvn test`: run unit tests across modules.
- `mvn -pl pdf-inbound-api spring-boot:run`: run the API module locally.
- `docker compose up -d`: start infrastructure dependencies defined in `docker-compose.yml`.

## Coding Style & Naming Conventions
- Java 21, Spring Boot 3.x.
- Follow standard Java conventions: 4-space indentation, `UpperCamelCase` for classes, `lowerCamelCase` for methods/fields.
- Package prefix is `org.papercloud.de` (e.g., `org.papercloud.de.pdfapi`).
- Implementation classes commonly use `*Impl` (e.g., `DocumentEnrichmentProcessorImpl`).
- No repo-wide formatter is configured; keep style consistent with nearby code.

## Testing Guidelines
- Primary frameworks: JUnit 5, Spring Boot Test, Mockito, and Testcontainers.
- Use `src/test/resources` for fixtures (see `pdf-inbound-api/src/test/resources`).
- Layered testing strategy is documented in `TESTING_STRATEGY.md` (unit first, slice tests, targeted integration).
- Coverage goals are discussed in `arch-notes.txt` but not enforced by tooling.

## Commit & Pull Request Guidelines
- Commit history shows short, descriptive messages without a strict convention. Keep messages brief and action-oriented (e.g., "Refactor enrichment pipeline").
- PRs should include:
  - A clear summary of changes and affected modules.
  - Testing notes (`mvn test`, or module-specific tests).
  - Any config or infrastructure updates (Docker services, environment variables).

## Security & Configuration Tips
- Test configuration lives in `application-test.yml` under relevant modules.
- Treat OCR/LLM credentials and external endpoints as secrets; avoid hard-coding them in source.
