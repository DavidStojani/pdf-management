# PDF Management Platform

A modular Spring Boot application for ingesting, processing, and searching PDF documents. Uploads are persisted securely, run through OCR and text-cleaning, enriched via an LLM adapter, and prepared for downstream indexing and search.

## Architecture at a glance
- **pdf-api** – REST edge for upload/download and folder-scanning hooks (see `DocumentController`).
- **pdf-service** – Orchestrates ingestion, OCR, enrichment, and emits index events to search.
- **pdf-database** – JPA entities/repositories for users, documents, and per-page text storage.
- **pdf-common** – Shared DTOs, mappers, events, and utility interfaces used across modules.
- **pdf-security** – Spring Security configuration and models for authentication.
- **pdf-llm** – Adapters for LLM-backed enrichment of OCR text.
- **pdf-search** – Placeholder for Elasticsearch integration fed by index events.

## Core document flow
1. **Upload** – `DocumentServiceImpl.processUpload` validates the authenticated user and PDF, persists the file, and publishes an `OcrEvent` with the raw bytes for downstream processing.
2. **OCR** – `OcrEventListener` invokes `DocumentOcrProcessorImpl` to extract page text, saves per-page results, and emits an `EnrichmentEvent` containing the OCR output.
3. **Enrichment** – `DocumentEnrichmentProcessorImpl` cleans the first page, calls the enrichment service asynchronously, persists title/date/tags back onto the document, and publishes an `IndexDocumentEvent` for search indexing.
4. **Download** – `DocumentServiceImpl.downloadDocument` enforces ownership and streams the stored PDF bytes back to the caller.

## Running locally
1. **Prerequisites** – Java 21, Maven 3.9+, and Docker if you want optional infrastructure (databases/search) via `docker-compose.yml`.
2. **Build** – From the repo root, run `mvn clean package` to compile all modules. (If Maven Central access is restricted, configure a mirror or local cache.)
3. **Start services** – Launch dependencies with `docker compose up -d` and then run `pdf-api` with `mvn spring-boot:run -pl pdf-api` to expose REST endpoints on port 8080.
4. **Upload & process** – POST to `/api/documents/upload` with a PDF file; OCR and enrichment run asynchronously once the upload is accepted.

## Testing
The project follows a layered testing strategy: fast Mockito unit tests for processors/adapters, Spring `@DataJpaTest` slices for persistence and event publication, and targeted container-based integration tests for search. See `TESTING_STRATEGY.md` for the detailed testing backlog and conventions.
