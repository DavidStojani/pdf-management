package org.papercloud.de.pdfdatabase.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PageRepository using real PostgreSQL database via TestContainers.
 * Tests repository operations, query methods, ordering, and search functionality with encrypted text.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("PageRepository Integration Tests")
class PageRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private DocumentJpaRepository documentJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UserEntity testUser;
    private DocumentPdfEntity testDocument;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
        testUser = userJpaRepository.save(testUser);

        testDocument = DocumentPdfEntity.builder()
                .filename("test-document.pdf")
                .title("Test Document")
                .contentType("application/pdf")
                .size(1024L)
                .status(Document.Status.UPLOADED)
                .uploadedAt(LocalDateTime.now())
                .owner(testUser)
                .build();
        testDocument = documentJpaRepository.save(testDocument);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("Save pages linked to document")
    class SavePagesTests {

        @Test
        @DisplayName("should save page linked to document")
        void save_pageLinkedToDocument_shouldSucceed() {
            // Arrange
            PagesPdfEntity page = PagesPdfEntity.builder()
                    .pageNumber(1)
                    .pageText("This is page 1 content")
                    .document(testDocument)
                    .build();

            // Act
            PagesPdfEntity savedPage = pageRepository.save(page);
            entityManager.flush();
            entityManager.clear();

            // Assert
            PagesPdfEntity foundPage = pageRepository.findById(savedPage.getId()).orElseThrow();
            assertThat(foundPage.getPageNumber()).isEqualTo(1);
            assertThat(foundPage.getPageText()).isEqualTo("This is page 1 content");
            assertThat(foundPage.getDocument().getId()).isEqualTo(testDocument.getId());
        }

        @Test
        @DisplayName("should save multiple pages for same document")
        void save_multiplePagesForDocument_shouldSucceed() {
            // Arrange
            PagesPdfEntity page1 = createTestPage(1, "First page content");
            PagesPdfEntity page2 = createTestPage(2, "Second page content");
            PagesPdfEntity page3 = createTestPage(3, "Third page content");

            // Act
            pageRepository.save(page1);
            pageRepository.save(page2);
            pageRepository.save(page3);
            entityManager.flush();
            entityManager.clear();

            // Assert
            List<PagesPdfEntity> pages = pageRepository.findByDocumentId(testDocument.getId());
            assertThat(pages).hasSize(3);
        }

        @Test
        @DisplayName("should save page with encrypted text")
        void save_pageWithEncryptedText_shouldEncryptAndDecryptCorrectly() {
            // Arrange
            String originalText = "This is sensitive content that needs encryption";
            PagesPdfEntity page = createTestPage(1, originalText);

            // Act
            PagesPdfEntity savedPage = pageRepository.save(page);
            entityManager.flush();
            entityManager.clear();

            // Assert
            PagesPdfEntity foundPage = pageRepository.findById(savedPage.getId()).orElseThrow();
            assertThat(foundPage.getPageText()).isEqualTo(originalText);
        }

        @Test
        @DisplayName("should save page with empty text")
        void save_pageWithEmptyText_shouldSucceed() {
            // Arrange
            PagesPdfEntity page = createTestPage(1, "");

            // Act
            PagesPdfEntity savedPage = pageRepository.save(page);
            entityManager.flush();
            entityManager.clear();

            // Assert
            PagesPdfEntity foundPage = pageRepository.findById(savedPage.getId()).orElseThrow();
            assertThat(foundPage.getPageText()).isEmpty();
        }

        @Test
        @DisplayName("should save page with null text")
        void save_pageWithNullText_shouldSucceed() {
            // Arrange
            PagesPdfEntity page = PagesPdfEntity.builder()
                    .pageNumber(1)
                    .pageText(null)
                    .document(testDocument)
                    .build();

            // Act
            PagesPdfEntity savedPage = pageRepository.save(page);
            entityManager.flush();
            entityManager.clear();

            // Assert
            PagesPdfEntity foundPage = pageRepository.findById(savedPage.getId()).orElseThrow();
            assertThat(foundPage.getPageText()).isNull();
        }

        @Test
        @DisplayName("should save page with Unicode text")
        void save_pageWithUnicodeText_shouldPreserveUnicode() {
            // Arrange
            String unicodeText = "Hello 世界 🌍 Привет مرحبا Special chars: ñ, ü, é";
            PagesPdfEntity page = createTestPage(1, unicodeText);

            // Act
            PagesPdfEntity savedPage = pageRepository.save(page);
            entityManager.flush();
            entityManager.clear();

            // Assert
            PagesPdfEntity foundPage = pageRepository.findById(savedPage.getId()).orElseThrow();
            assertThat(foundPage.getPageText()).isEqualTo(unicodeText);
        }

        @Test
        @DisplayName("should save page with large text content")
        void save_pageWithLargeText_shouldSucceed() {
            // Arrange
            StringBuilder largeText = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                largeText.append("Line ").append(i).append(": This is a long line of text with various content. ");
            }
            PagesPdfEntity page = createTestPage(1, largeText.toString());

            // Act
            PagesPdfEntity savedPage = pageRepository.save(page);
            entityManager.flush();
            entityManager.clear();

            // Assert
            PagesPdfEntity foundPage = pageRepository.findById(savedPage.getId()).orElseThrow();
            assertThat(foundPage.getPageText()).isEqualTo(largeText.toString());
            assertThat(foundPage.getPageText().length()).isGreaterThan(50000);
        }
    }

    @Nested
    @DisplayName("findByDocumentIdOrderByPageNumber operations")
    class FindByDocumentIdOrderByPageNumberTests {

        @Test
        @DisplayName("should find pages ordered by page number")
        void findByDocumentIdOrderByPageNumber_multiplePages_shouldReturnOrderedPages() {
            // Arrange
            createAndSavePage(3, "Page 3");
            createAndSavePage(1, "Page 1");
            createAndSavePage(5, "Page 5");
            createAndSavePage(2, "Page 2");
            createAndSavePage(4, "Page 4");

            // Act
            List<PagesPdfEntity> pages = pageRepository.findByDocumentIdOrderByPageNumber(testDocument.getId());

            // Assert
            assertThat(pages).hasSize(5);
            assertThat(pages).extracting(PagesPdfEntity::getPageNumber)
                    .containsExactly(1, 2, 3, 4, 5);
            assertThat(pages).extracting(PagesPdfEntity::getPageText)
                    .containsExactly("Page 1", "Page 2", "Page 3", "Page 4", "Page 5");
        }

        @Test
        @DisplayName("should return empty list for document with no pages")
        void findByDocumentIdOrderByPageNumber_documentWithNoPages_shouldReturnEmptyList() {
            // Arrange
            DocumentPdfEntity emptyDocument = DocumentPdfEntity.builder()
                    .filename("empty.pdf")
                    .title("Empty Document")
                    .contentType("application/pdf")
                    .status(Document.Status.UPLOADED)
                    .uploadedAt(LocalDateTime.now())
                    .owner(testUser)
                    .build();
            emptyDocument = documentJpaRepository.save(emptyDocument);
            entityManager.flush();

            // Act
            List<PagesPdfEntity> pages = pageRepository.findByDocumentIdOrderByPageNumber(emptyDocument.getId());

            // Assert
            assertThat(pages).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for non-existent document")
        void findByDocumentIdOrderByPageNumber_nonExistentDocument_shouldReturnEmptyList() {
            // Act
            List<PagesPdfEntity> pages = pageRepository.findByDocumentIdOrderByPageNumber(99999L);

            // Assert
            assertThat(pages).isEmpty();
        }

        @Test
        @DisplayName("should handle single page document")
        void findByDocumentIdOrderByPageNumber_singlePage_shouldReturnSinglePage() {
            // Arrange
            createAndSavePage(1, "Only page");

            // Act
            List<PagesPdfEntity> pages = pageRepository.findByDocumentIdOrderByPageNumber(testDocument.getId());

            // Assert
            assertThat(pages).hasSize(1);
            assertThat(pages.get(0).getPageNumber()).isEqualTo(1);
            assertThat(pages.get(0).getPageText()).isEqualTo("Only page");
        }

        @Test
        @DisplayName("should handle pages with duplicate page numbers")
        void findByDocumentIdOrderByPageNumber_duplicatePageNumbers_shouldReturnAllPages() {
            // Arrange - This is an edge case, normally shouldn't happen but the DB allows it
            createAndSavePage(1, "First page 1");
            createAndSavePage(1, "Second page 1");
            createAndSavePage(2, "Page 2");

            // Act
            List<PagesPdfEntity> pages = pageRepository.findByDocumentIdOrderByPageNumber(testDocument.getId());

            // Assert
            assertThat(pages).hasSize(3);
            assertThat(pages).extracting(PagesPdfEntity::getPageNumber)
                    .containsExactly(1, 1, 2);
        }
    }

    @Nested
    @DisplayName("findByExtractedTextContaining operations - Note: searches on encrypted data")
    class FindByExtractedTextContainingTests {

        @BeforeEach
        void setUpSearchData() {
            createAndSavePage(1, "The quick brown fox jumps over the lazy dog");
            createAndSavePage(2, "Lorem ipsum dolor sit amet consectetur adipiscing elit");
            createAndSavePage(3, "Java programming language is powerful and versatile");
            createAndSavePage(4, "Spring Boot makes Java development easier");
            createAndSavePage(5, "The lazy cat sleeps all day");

            // Create another document with pages
            DocumentPdfEntity document2 = DocumentPdfEntity.builder()
                    .filename("document2.pdf")
                    .title("Second Document")
                    .contentType("application/pdf")
                    .status(Document.Status.UPLOADED)
                    .uploadedAt(LocalDateTime.now())
                    .owner(testUser)
                    .build();
            document2 = documentJpaRepository.save(document2);

            PagesPdfEntity page = PagesPdfEntity.builder()
                    .pageNumber(1)
                    .pageText("Another document with Java content")
                    .document(document2)
                    .build();
            pageRepository.save(page);

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("should return no results when searching encrypted text (limitation)")
        void findByExtractedTextContaining_encryptedText_returnsNoResults() {
            // Note: pageText is encrypted at rest using EncryptedStringConverter.
            // Database LIKE queries search the encrypted ciphertext, not plaintext.
            // This means text searches will not find matches in the encrypted data.
            // A search index (e.g., Elasticsearch) would be needed for full-text search.

            // Act
            List<PagesPdfEntity> pages = pageRepository.findByExtractedTextContaining("lazy");

            // Assert - no matches because we're searching encrypted text
            assertThat(pages).isEmpty();
        }

        @Test
        @DisplayName("should verify query method exists and returns results format")
        void findByExtractedTextContaining_methodExists_returnsListFormat() {
            // This test verifies the query method is properly defined,
            // even though it won't find matches in encrypted text.
            // In a production system, text search would use a separate search index.

            // Act
            List<PagesPdfEntity> pages = pageRepository.findByExtractedTextContaining("any search term");

            // Assert - method works but returns empty due to encryption
            assertThat(pages).isNotNull();
            assertThat(pages).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByExtractedTextContaining with pagination - Note: limited by encryption")
    class FindByExtractedTextContainingWithPaginationTests {

        @Test
        @DisplayName("should support pagination API even though encryption limits search")
        void findByExtractedTextContaining_pagination_methodSignatureWorks() {
            // Note: This verifies the pagination API exists and works structurally,
            // even though encrypted text search won't return matches.

            // Arrange
            for (int i = 1; i <= 5; i++) {
                createAndSavePage(i, "Page " + i + " test content");
            }
            entityManager.flush();
            entityManager.clear();

            Pageable pageable = PageRequest.of(0, 3);

            // Act
            Page<PagesPdfEntity> pageResult = pageRepository.findByExtractedTextContaining("test", pageable);

            // Assert - method works, returns empty due to encryption
            assertThat(pageResult).isNotNull();
            assertThat(pageResult.getContent()).isEmpty();
            assertThat(pageResult.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("countPageMatchesPerDocument operations - Note: limited by encryption")
    class CountPageMatchesPerDocumentTests {

        @Test
        @DisplayName("should execute count query even though encryption prevents matches")
        void countPageMatchesPerDocument_encryptedData_returnsEmpty() {
            // Arrange
            createAndSavePage(1, "This is an important document");
            createAndSavePage(2, "More important details");
            entityManager.flush();
            entityManager.clear();

            // Act
            List<Object[]> results = pageRepository.countPageMatchesPerDocument("important");

            // Assert - query executes but finds no matches due to encryption
            assertThat(results).isEmpty();
        }
    }

    // Helper methods

    private PagesPdfEntity createTestPage(int pageNumber, String pageText) {
        return PagesPdfEntity.builder()
                .pageNumber(pageNumber)
                .pageText(pageText)
                .document(testDocument)
                .build();
    }

    private PagesPdfEntity createAndSavePage(int pageNumber, String pageText) {
        PagesPdfEntity page = createTestPage(pageNumber, pageText);
        return pageRepository.save(page);
    }
}
