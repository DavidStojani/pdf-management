# Architecture Desicion:

### Event-Driven vs State-Machine

- Instead of relying solely on events to carry data,
you rely on the Database as the Source of Truth and the Event as a "Poke."

##### How it works: 
1. Add *status* Column to the DocPdfEntity 
   2. UPLOADED
   3. OCR_IN_PROGRESS
   4. OCR_COMPLETED
   5. ENRICHING
   6. COMPLETED
7. The Event only carries the ID, not the bytes
8. The Listener checks the Status before doing anything

##### Why this is better:
 
 **Idempotency:** 
 - If the LLM listener gets triggered twice, the first thing it does is check: if (doc.getStatus() == ENRICHING) return;. This instantly kills the second "ghost" call.
 
**Restartability**:
- If your server crashes halfway through, you can run a "cleanup" job that looks for any documents stuck in OCR_IN_PROGRESS and re-triggers them.

 **Memory Efficiency**: 
 - You aren't passing massive strings (pageTexts) through the event bus.
---
# Hexagonal Architecture Refactor Plan

## Problem

`pdf-common` depends on `pdf-database` (DIP violation). The `DocumentMapper` imports JPA entities, coupling shared code to infrastructure.

## Goal

Pluggable architecture supporting:
- Multiple databases (Postgres, Mongo)
- Multiple LLM providers (Ollama, OpenAI)
- Multiple search backends (Elasticsearch, OpenSearch)
- Easy unit testing via interface mocking

## Target Structure

```
pdf-core (no internal deps)
    ├── domain/         # Plain domain objects (no JPA)
    ├── ports/outbound/ # Repository & service interfaces
    ├── dto/            # All DTOs
    └── events/         # Domain events

pdf-adapter-postgres    → implements DocumentRepository
pdf-adapter-ollama      → implements EnrichmentService
pdf-adapter-elasticsearch → implements SearchService
pdf-adapter-tesseract   → implements TextExtractionService

pdf-service             → depends only on pdf-core (uses ports)
pdf-api                 → depends on pdf-core, pdf-service
```

## Port Interfaces (in pdf-core)

```java
public interface DocumentRepository {
    Document save(Document document);
    Optional<Document> findById(Long id);
    List<Document> findByUserId(Long userId);
}

public interface EnrichmentService {
    Mono<EnrichmentResultDTO> enrich(String text);
}

public interface SearchService {
    SearchResultDTO search(String query, int page, int size);
    void index(IndexableDocumentDTO document);
}
```

## Adapter Example (pdf-adapter-postgres)

```java
@Repository
public class PostgresDocumentRepository implements DocumentRepository {
    private final DocumentJpaRepository jpaRepo;
    private final DocumentPersistenceMapper mapper;

    public Document save(Document doc) {
        return mapper.toDomain(jpaRepo.save(mapper.toEntity(doc)));
    }
}
```

## Migration Steps

- [x] 1. Create `pdf-core` module (no internal dependencies)
- [x] 2. Define port interfaces in `pdf-core/ports/outbound/`
- [x] 3. Move DTOs and events from `pdf-common` → `pdf-core`
- [x] 4. Create domain objects in `pdf-core/domain/` (no JPA annotations)
- [x] 5. Add adapter layer to `pdf-database` implementing `DocumentRepository`
- [x] 6. Update `pdf-llm` to implement `EnrichmentService` port
- [x] 7. Update `pdf-search` to implement `SearchService` port
- [x] 8. Update `pdf-service` to depend on `pdf-core`
- [x] 9. Remove `pdf-common` from build

## What Was Created

### pdf-core module
- `domain/` - Document, Page, User (plain POJOs)
- `ports/outbound/` - DocumentRepository, UserRepository, EnrichmentService, SearchService, TextExtractionService
- `ports/inbound/` - AuthenticationService, PasswordService
- `dto/` - All DTOs moved from pdf-common
- `events/` - OcrEvent, EnrichmentEvent, IndexDocumentEvent

### Adapter Implementations
- `pdf-database/adapter/PostgresDocumentRepository` - implements DocumentRepository
- `pdf-database/adapter/PostgresUserRepository` - implements UserRepository
- `pdf-llm/OllamaEnrichmentServiceImp` - implements EnrichmentService
- `pdf-search/ElasticsearchServiceImpl` - implements SearchService

## Dependency Flow

```
pdf-api
   └──> pdf-service
           └──> pdf-core <──┬── pdf-adapter-postgres
                            ├── pdf-adapter-ollama
                            ├── pdf-adapter-elasticsearch
                            └── pdf-adapter-tesseract
```

## Swapping Implementations

```yaml
# application.yml
app:
  database: postgres  # or mongo
  llm: ollama         # or openai
  search: elasticsearch
```

```java
@Bean
@ConditionalOnProperty(name = "app.llm", havingValue = "ollama")
EnrichmentService ollama() { return new OllamaEnrichmentService(); }

@Bean
@ConditionalOnProperty(name = "app.llm", havingValue = "openai")
EnrichmentService openai() { return new OpenAiEnrichmentService(); }
```

## Testing Benefit

```java
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    @Mock DocumentRepository repository;  // No DB needed
    @Mock EnrichmentService enrichment;   // No LLM needed
    @InjectMocks DocumentServiceImpl service;
}
```
