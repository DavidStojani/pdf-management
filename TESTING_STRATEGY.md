# Testing strategy recommendations

This document summarizes the current state of the test suites and proposes a layered strategy for unit, slice, and integration tests. The goal is to keep fast feedback for core logic while reserving full-stack tests for cross-module wiring.

## Current layout (observations)
- **API integration duplication.** There are two controller integration suites: the MockMvc-based `integration/controller/DocumentControllerIntegrationTest` that relies on `MockBean` services and a reusable `BaseIntegrationTest`, and the older `org.papercloud.de.pdfapi.controller.DocumentControllerIntegrationTest` that starts a PostgreSQL Testcontainer and seeds users manually. Keeping both creates overlap and slows execution.
- **Heavy service integration test.** `pdfservice/processor/DocumentEnrichmentProcessorImplTest` is annotated with `@SpringBootTest` but still mocks most collaborators; it also mixes transactional JPA state with ad-hoc threads and an embedded event listener defined on the test class itself. The context startup cost outweighs the unit-level assertions.
- **Unit coverage concentrated in a few services.** Tests like `pdfservice/search/DocumentServiceImplTest` use Mockito effectively for ownership checks, while other services (OCR, enrichment orchestration, folder scanning, Elasticsearch indexing) have no unit-level coverage.

## Recommended layers
1. **Pure unit tests (no Spring):**
   - Target business rules and branching logic. Example candidates: `DocumentServiceImpl` download/authorization logic; OCR text cleaners; LLM prompt builders; enrichment result mappers.
   - Use `@ExtendWith(MockitoExtension.class)` and construct classes directly. Avoid starting the application context for these.

2. **Spring slices for adapters:**
   - **Web layer:** Keep a single MockMvc-based suite under `pdf-api` (e.g., `integration/controller/DocumentControllerIntegrationTest`). Remove or migrate the older Testcontainers-based controller test to reuse the same base class.
   - **Data layer:** For repository behavior and JPA mappings, use `@DataJpaTest` with Testcontainers/H2. Example: a small suite that persists `DocumentPdfEntity` with tags/ownership and verifies converters.
   - **Event handlers:** Instead of full `@SpringBootTest`, load only the listener plus mocked collaborators using `@Import` inside a slice configuration. Use `ApplicationEvents` or a synchronous `ApplicationEventMulticaster` for deterministic assertions.

3. **Targeted integration tests (with infrastructure):**
   - **Elasticsearch:** Use a dedicated Elasticsearch Testcontainer started in a test base class. Seed the index with a few `IndexableDocumentDTO` fixtures via the `ElasticsearchClient` before each test, then call `ElasticsearchServiceImpl.search` directly. This avoids booting the API or creating users/files while still exercising the real client and mappings.
   - **Pipeline happy path:** A single end-to-end test per bounded context (e.g., OCR ➜ enrichment ➜ index) that runs behind a profile or tag, using containerized dependencies if needed. Keep this minimal to avoid slowing the suite.

## Concrete next steps
- Consolidate to one controller integration suite and delete/disable the legacy Testcontainers variant.
- Refactor `DocumentEnrichmentProcessorImplTest` into two tests: a fast unit test for enrichment/error handling (construct `DocumentEnrichmentProcessorImpl` with mocks) and a small `@DataJpaTest` that verifies persistence effects without manual threads.
- Add focused unit tests for currently untested adapters (OCR strategies, folder scanning orchestration, search DTO validation) to increase coverage without extra infrastructure.
- Introduce an `ElasticsearchIntegrationTest` base that spins up a container, seeds documents, and exercises `ElasticsearchServiceImpl.search` and `indexDocument` in isolation from the API.

## Actionable task list

Use the following backlog to implement the recommended testing strategy:

1. **Controller integration tests**
   - Remove or migrate `org.papercloud.de.pdfapi.controller.DocumentControllerIntegrationTest` to the MockMvc-based base class used under `integration/controller`.
   - Keep only the MockMvc + `@MockBean` approach for HTTP-layer verification.

2. **Enrichment processor coverage**
   - Create a pure unit test for `DocumentEnrichmentProcessorImpl` using Mockito to verify retry/recover behavior and event publishing.
   - Add a `@DataJpaTest` for persistence side effects (status updates, page persistence) without manual thread management.

3. **Adapter/unit gaps**
   - Add unit tests for OCR strategies (`PdfBoxExtractStrategyImpl`, OCR cleaners) and folder scanning orchestration (ensure only new files are ingested, errors are logged but do not stop the poller).
   - Validate search DTO pagination defaults and error handling in `SearchController`/`ElasticsearchServiceImpl` without starting the full app.

4. **Elasticsearch integration**
   - Introduce an `ElasticsearchIntegrationTest` base that starts an Elasticsearch Testcontainer once per suite.
   - Seed the index directly via `ElasticsearchClient` with a few `IndexableDocumentDTO` fixtures in `@BeforeEach`.
   - Invoke `ElasticsearchServiceImpl.search` and `indexDocument` against the container to verify mappings and queries, skipping API/user setup.

5. **Pipeline smoke test (optional/tagged)**
   - Create a minimal end-to-end happy-path test that runs behind a profile/tag, wiring OCR ➜ enrichment ➜ indexing with containerized dependencies.
   - Limit to one or two scenarios so it does not dominate CI time.
