# Java Test Writer Agent Memory

## Project: PDF Management App

### Key Findings - pdf-outbound-database Module

**Encryption Limitations with Search**
- The PagesPdfEntity.pageText field uses `EncryptedStringConverter` for at-rest encryption
- Database LIKE queries (`findByExtractedTextContaining`) do NOT work on encrypted text
- The encryption happens at the JPA converter level, so database queries see encrypted values
- Search functionality would need to decrypt all records or use a different approach (e.g., search index)
- Integration tests must account for this limitation

**Spring Data JPA Null Handling**
- `findByEmail(null)` WILL match records where email IS NULL (not return empty)
- This is standard Spring Data JPA behavior for null parameters
- Tests should reflect actual JPA behavior, not ideal expectations

**Document.Status Enum Values**
- Available statuses: UPLOADED, OCR_IN_PROGRESS, OCR_COMPLETED, OCR_ERROR, ENRICHMENT_IN_PROGRESS, ENRICHMENT_COMPLETED, ENRICHMENT_ERROR
- NOT: INDEXED, ENRICHED (these don't exist in the domain)

**TestContainers Configuration**
- Module needs `@SpringBootApplication` test config class for `@DataJpaTest` to work
- Created TestConfig.java in src/test/java
- application-test.properties configured with TestContainers JDBC URL
- Tests use PostgreSQL 16 via TestContainers

**Entity Relationships**
- DocumentPdfEntity → PagesPdfEntity: OneToMany with CASCADE.ALL and orphanRemoval=true
- UserEntity → RoleEntity: ManyToMany with EAGER fetch
- DocumentPdfEntity → UserEntity: ManyToOne with LAZY fetch

### Key Findings - pdf-application Module

**MapStruct Mapper Testing**
- Can instantiate MapperImpl classes directly: `new DocumentServiceMapperImpl()`
- No Spring context needed for mapper testing
- Null collections map to empty lists (not null) - this is MapStruct default behavior
- Array fields (like pdfContent byte[]) are copied, not referenced

### Key Findings - pdf-inbound-api Module (Controllers)

**@WebMvcTest Challenges**
- Main application class uses @ComponentScan across all packages, @EnableJpaRepositories, @EntityScan
- @WebMvcTest tries to load full application context
- Solution: Use `excludeAutoConfiguration` to skip DataSource and JPA
- `@MockBean` is deprecated but still functional (warnings in compile)

**SearchHitDTO Structure (Important!)**
- documentId: String (not Long)
- documentName: String
- pageNumber: int
- textSnippet: String
- NO fields named: title, score, fileName

**Phase 2 Tests Status**
- DocumentServiceMapperTest: PASSING (pdf-application)
- DocumentPersistenceMapperTest: PASSING (pdf-outbound-database)
- DocumentControllerTest: ApplicationContext load failure
- AuthControllerTest: ApplicationContext load failure
- SearchControllerTest: ApplicationContext load failure

**Controller Test Issue**
- Complex bean dependency graph prevents ApplicationContext from loading
- Need to investigate alternative approaches (@SpringBootTest, custom test config, etc.)
