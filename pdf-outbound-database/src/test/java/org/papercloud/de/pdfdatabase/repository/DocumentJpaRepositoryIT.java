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
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DocumentJpaRepository using real PostgreSQL database via TestContainers.
 * Tests repository operations, query methods, and entity relationships including cascade behavior.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("DocumentJpaRepository Integration Tests")
class DocumentJpaRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private DocumentJpaRepository documentJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UserEntity testUser;

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
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("Save and findById operations")
    class SaveAndFindByIdTests {

        @Test
        @DisplayName("should save document and retrieve by id")
        void saveAndFindById_validDocument_shouldSucceed() {
            // Arrange
            DocumentPdfEntity document = createTestDocument("test-document.pdf", "Test Document");

            // Act
            DocumentPdfEntity savedDocument = documentJpaRepository.save(document);
            entityManager.flush();
            entityManager.clear();
            Optional<DocumentPdfEntity> foundDocument = documentJpaRepository.findById(savedDocument.getId());

            // Assert
            assertThat(foundDocument).isPresent();
            assertThat(foundDocument.get().getId()).isEqualTo(savedDocument.getId());
            assertThat(foundDocument.get().getFilename()).isEqualTo("test-document.pdf");
            assertThat(foundDocument.get().getTitle()).isEqualTo("Test Document");
            assertThat(foundDocument.get().getContentType()).isEqualTo("application/pdf");
            assertThat(foundDocument.get().getStatus()).isEqualTo(Document.Status.UPLOADED);
            assertThat(foundDocument.get().getOwner().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should save document with encrypted PDF content")
        void save_documentWithPdfContent_shouldEncryptContent() {
            // Arrange
            byte[] pdfContent = "Sample PDF content".getBytes();
            DocumentPdfEntity document = createTestDocument("encrypted.pdf", "Encrypted Document");
            document.setPdfContent(pdfContent);

            // Act
            DocumentPdfEntity savedDocument = documentJpaRepository.save(document);
            entityManager.flush();
            entityManager.clear();
            Optional<DocumentPdfEntity> foundDocument = documentJpaRepository.findById(savedDocument.getId());

            // Assert
            assertThat(foundDocument).isPresent();
            assertThat(foundDocument.get().getPdfContent()).isEqualTo(pdfContent);
        }

        @Test
        @DisplayName("should save document with all fields populated")
        void save_documentWithAllFields_shouldPersistAllData() {
            // Arrange
            DocumentPdfEntity document = DocumentPdfEntity.builder()
                    .filename("complete-document.pdf")
                    .title("Complete Document")
                    .contentType("application/pdf")
                    .size(1024L)
                    .status(Document.Status.ENRICHMENT_COMPLETED)
                    .pdfContent("PDF bytes".getBytes())
                    .uploadedAt(LocalDateTime.now())
                    .owner(testUser)
                    .tags(List.of("tag1", "tag2", "tag3"))
                    .dateOnDocument(LocalDate.of(2024, 1, 15))
                    .failedEnrichment(false)
                    .build();

            // Act
            DocumentPdfEntity savedDocument = documentJpaRepository.save(document);
            entityManager.flush();
            entityManager.clear();
            Optional<DocumentPdfEntity> foundDocument = documentJpaRepository.findById(savedDocument.getId());

            // Assert
            assertThat(foundDocument).isPresent();
            DocumentPdfEntity retrieved = foundDocument.get();
            assertThat(retrieved.getFilename()).isEqualTo("complete-document.pdf");
            assertThat(retrieved.getTitle()).isEqualTo("Complete Document");
            assertThat(retrieved.getSize()).isEqualTo(1024L);
            assertThat(retrieved.getStatus()).isEqualTo(Document.Status.ENRICHMENT_COMPLETED);
            assertThat(retrieved.getTags()).containsExactly("tag1", "tag2", "tag3");
            assertThat(retrieved.getDateOnDocument()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(retrieved.isFailedEnrichment()).isFalse();
        }

        @Test
        @DisplayName("should return empty optional for non-existent id")
        void findById_nonExistentId_shouldReturnEmpty() {
            // Act
            Optional<DocumentPdfEntity> foundDocument = documentJpaRepository.findById(99999L);

            // Assert
            assertThat(foundDocument).isEmpty();
        }

        @Test
        @DisplayName("should save document with minimal required fields")
        void save_documentWithMinimalFields_shouldSucceed() {
            // Arrange
            DocumentPdfEntity document = DocumentPdfEntity.builder()
                    .filename("minimal.pdf")
                    .owner(testUser)
                    .uploadedAt(LocalDateTime.now())
                    .status(Document.Status.UPLOADED)
                    .build();

            // Act
            DocumentPdfEntity savedDocument = documentJpaRepository.save(document);
            entityManager.flush();
            entityManager.clear();
            Optional<DocumentPdfEntity> foundDocument = documentJpaRepository.findById(savedDocument.getId());

            // Assert
            assertThat(foundDocument).isPresent();
            assertThat(foundDocument.get().getFilename()).isEqualTo("minimal.pdf");
        }
    }

    @Nested
    @DisplayName("findByOwnerUsername operations")
    class FindByOwnerUsernameTests {

        @Test
        @DisplayName("should find all documents for a user")
        void findByOwnerUsername_existingUser_shouldReturnAllDocuments() {
            // Arrange
            DocumentPdfEntity doc1 = createAndSaveDocument("document1.pdf", "Document 1");
            DocumentPdfEntity doc2 = createAndSaveDocument("document2.pdf", "Document 2");
            DocumentPdfEntity doc3 = createAndSaveDocument("document3.pdf", "Document 3");

            // Act
            List<DocumentPdfEntity> documents = documentJpaRepository.findByOwnerUsername("testuser");

            // Assert
            assertThat(documents).hasSize(3);
            assertThat(documents).extracting(DocumentPdfEntity::getFilename)
                    .containsExactlyInAnyOrder("document1.pdf", "document2.pdf", "document3.pdf");
        }

        @Test
        @DisplayName("should return empty list for user with no documents")
        void findByOwnerUsername_userWithNoDocuments_shouldReturnEmptyList() {
            // Arrange
            UserEntity anotherUser = UserEntity.builder()
                    .username("emptyuser")
                    .email("empty@example.com")
                    .password("password")
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            userJpaRepository.save(anotherUser);

            // Act
            List<DocumentPdfEntity> documents = documentJpaRepository.findByOwnerUsername("emptyuser");

            // Assert
            assertThat(documents).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for non-existent user")
        void findByOwnerUsername_nonExistentUser_shouldReturnEmptyList() {
            // Act
            List<DocumentPdfEntity> documents = documentJpaRepository.findByOwnerUsername("nonexistent");

            // Assert
            assertThat(documents).isEmpty();
        }

        @Test
        @DisplayName("should return documents only for the specified user")
        void findByOwnerUsername_multipleUsers_shouldReturnCorrectDocuments() {
            // Arrange
            UserEntity user2 = UserEntity.builder()
                    .username("user2")
                    .email("user2@example.com")
                    .password("password")
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            user2 = userJpaRepository.save(user2);

            createAndSaveDocument("doc1-user1.pdf", "Doc 1");
            createAndSaveDocument("doc2-user1.pdf", "Doc 2");

            DocumentPdfEntity doc3 = createTestDocument("doc1-user2.pdf", "Doc 3");
            doc3.setOwner(user2);
            documentJpaRepository.save(doc3);

            // Act
            List<DocumentPdfEntity> user1Docs = documentJpaRepository.findByOwnerUsername("testuser");
            List<DocumentPdfEntity> user2Docs = documentJpaRepository.findByOwnerUsername("user2");

            // Assert
            assertThat(user1Docs).hasSize(2);
            assertThat(user1Docs).extracting(DocumentPdfEntity::getFilename)
                    .containsExactlyInAnyOrder("doc1-user1.pdf", "doc2-user1.pdf");
            assertThat(user2Docs).hasSize(1);
            assertThat(user2Docs).extracting(DocumentPdfEntity::getFilename)
                    .containsExactly("doc1-user2.pdf");
        }
    }

    @Nested
    @DisplayName("existsByFilenameAndOwnerUsername operations")
    class ExistsByFilenameAndOwnerUsernameTests {

        @Test
        @DisplayName("should return true when document exists for user")
        void existsByFilenameAndOwnerUsername_existingDocument_shouldReturnTrue() {
            // Arrange
            createAndSaveDocument("existing.pdf", "Existing Document");

            // Act
            boolean exists = documentJpaRepository.existsByFilenameAndOwnerUsername("existing.pdf", "testuser");

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when document does not exist")
        void existsByFilenameAndOwnerUsername_nonExistentDocument_shouldReturnFalse() {
            // Act
            boolean exists = documentJpaRepository.existsByFilenameAndOwnerUsername("nonexistent.pdf", "testuser");

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("should return false when filename exists for different user")
        void existsByFilenameAndOwnerUsername_differentUser_shouldReturnFalse() {
            // Arrange
            UserEntity user2 = UserEntity.builder()
                    .username("user2")
                    .email("user2@example.com")
                    .password("password")
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            user2 = userJpaRepository.save(user2);

            DocumentPdfEntity doc = createTestDocument("shared-name.pdf", "Document");
            doc.setOwner(user2);
            documentJpaRepository.save(doc);

            // Act
            boolean exists = documentJpaRepository.existsByFilenameAndOwnerUsername("shared-name.pdf", "testuser");

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("should return true for exact filename match only")
        void existsByFilenameAndOwnerUsername_exactMatch_shouldReturnTrueOnlyForExactMatch() {
            // Arrange
            createAndSaveDocument("document.pdf", "Document");

            // Act
            boolean exactMatch = documentJpaRepository.existsByFilenameAndOwnerUsername("document.pdf", "testuser");
            boolean partialMatch = documentJpaRepository.existsByFilenameAndOwnerUsername("document", "testuser");
            boolean caseVariant = documentJpaRepository.existsByFilenameAndOwnerUsername("Document.pdf", "testuser");

            // Assert
            assertThat(exactMatch).isTrue();
            assertThat(partialMatch).isFalse();
            assertThat(caseVariant).isFalse();
        }
    }

    @Nested
    @DisplayName("Cascade operations with pages")
    class CascadeOperationsTests {

        @Test
        @DisplayName("should cascade save pages when saving document")
        void save_documentWithPages_shouldCascadeSavePages() {
            // Arrange
            DocumentPdfEntity document = createTestDocument("doc-with-pages.pdf", "Document with Pages");

            List<PagesPdfEntity> pages = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                PagesPdfEntity page = PagesPdfEntity.builder()
                        .pageNumber(i)
                        .pageText("Page " + i + " content")
                        .document(document)
                        .build();
                pages.add(page);
            }
            document.setPages(pages);

            // Act
            DocumentPdfEntity savedDocument = documentJpaRepository.save(document);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<DocumentPdfEntity> foundDocument = documentJpaRepository.findById(savedDocument.getId());
            assertThat(foundDocument).isPresent();
            assertThat(foundDocument.get().getPages()).hasSize(3);
            assertThat(foundDocument.get().getPages())
                    .extracting(PagesPdfEntity::getPageNumber)
                    .containsExactlyInAnyOrder(1, 2, 3);
        }

        @Test
        @DisplayName("should cascade delete pages when deleting document")
        void delete_documentWithPages_shouldCascadeDeletePages() {
            // Arrange
            DocumentPdfEntity document = createTestDocument("doc-to-delete.pdf", "Document to Delete");

            List<PagesPdfEntity> pages = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                PagesPdfEntity page = PagesPdfEntity.builder()
                        .pageNumber(i)
                        .pageText("Page " + i + " content")
                        .document(document)
                        .build();
                pages.add(page);
            }
            document.setPages(pages);

            DocumentPdfEntity savedDocument = documentJpaRepository.save(document);
            Long documentId = savedDocument.getId();
            entityManager.flush();
            entityManager.clear();

            // Act
            documentJpaRepository.deleteById(documentId);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<DocumentPdfEntity> foundDocument = documentJpaRepository.findById(documentId);
            List<PagesPdfEntity> remainingPages = pageRepository.findByDocumentId(documentId);

            assertThat(foundDocument).isEmpty();
            assertThat(remainingPages).isEmpty();
        }

        @Test
        @DisplayName("should orphan remove pages when clearing pages collection")
        void update_documentRemovePages_shouldOrphanRemovePages() {
            // Arrange
            DocumentPdfEntity document = createTestDocument("doc-orphan.pdf", "Document with Orphans");

            List<PagesPdfEntity> pages = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                PagesPdfEntity page = PagesPdfEntity.builder()
                        .pageNumber(i)
                        .pageText("Page " + i + " content")
                        .document(document)
                        .build();
                pages.add(page);
            }
            document.setPages(pages);

            DocumentPdfEntity savedDocument = documentJpaRepository.save(document);
            Long documentId = savedDocument.getId();
            entityManager.flush();
            entityManager.clear();

            // Act - clear pages collection
            DocumentPdfEntity managedDocument = documentJpaRepository.findById(documentId).orElseThrow();
            managedDocument.getPages().clear();
            documentJpaRepository.save(managedDocument);
            entityManager.flush();
            entityManager.clear();

            // Assert
            List<PagesPdfEntity> remainingPages = pageRepository.findByDocumentId(documentId);
            assertThat(remainingPages).isEmpty();
        }

        @Test
        @DisplayName("should update pages when modifying pages collection")
        void update_documentModifyPages_shouldUpdatePages() {
            // Arrange
            DocumentPdfEntity document = createTestDocument("doc-update-pages.pdf", "Document Update Pages");

            List<PagesPdfEntity> pages = new ArrayList<>();
            PagesPdfEntity page1 = PagesPdfEntity.builder()
                    .pageNumber(1)
                    .pageText("Original content")
                    .document(document)
                    .build();
            pages.add(page1);
            document.setPages(pages);

            DocumentPdfEntity savedDocument = documentJpaRepository.save(document);
            Long documentId = savedDocument.getId();
            entityManager.flush();
            entityManager.clear();

            // Act - modify existing page and add new page
            DocumentPdfEntity managedDocument = documentJpaRepository.findById(documentId).orElseThrow();
            managedDocument.getPages().get(0).setPageText("Updated content");

            PagesPdfEntity page2 = PagesPdfEntity.builder()
                    .pageNumber(2)
                    .pageText("New page content")
                    .document(managedDocument)
                    .build();
            managedDocument.getPages().add(page2);

            documentJpaRepository.save(managedDocument);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<DocumentPdfEntity> foundDocument = documentJpaRepository.findById(documentId);
            assertThat(foundDocument).isPresent();
            assertThat(foundDocument.get().getPages()).hasSize(2);
            assertThat(foundDocument.get().getPages())
                    .extracting(PagesPdfEntity::getPageText)
                    .containsExactlyInAnyOrder("Updated content", "New page content");
        }
    }

    @Nested
    @DisplayName("Document status transitions")
    class StatusTransitionsTests {

        @Test
        @DisplayName("should update document status")
        void update_documentStatus_shouldPersistNewStatus() {
            // Arrange
            DocumentPdfEntity document = createAndSaveDocument("status-test.pdf", "Status Test");
            Long documentId = document.getId();
            entityManager.clear();

            // Act
            DocumentPdfEntity managedDocument = documentJpaRepository.findById(documentId).orElseThrow();
            managedDocument.setStatus(Document.Status.ENRICHMENT_COMPLETED);
            documentJpaRepository.save(managedDocument);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<DocumentPdfEntity> foundDocument = documentJpaRepository.findById(documentId);
            assertThat(foundDocument).isPresent();
            assertThat(foundDocument.get().getStatus()).isEqualTo(Document.Status.ENRICHMENT_COMPLETED);
        }

        @Test
        @DisplayName("should handle all document status values")
        void save_documentsWithDifferentStatuses_shouldPersistAllStatuses() {
            // Arrange & Act
            DocumentPdfEntity uploadedDoc = createTestDocument("uploaded.pdf", "Uploaded");
            uploadedDoc.setStatus(Document.Status.UPLOADED);
            uploadedDoc = documentJpaRepository.save(uploadedDoc);

            DocumentPdfEntity indexedDoc = createTestDocument("indexed.pdf", "Indexed");
            indexedDoc.setStatus(Document.Status.OCR_COMPLETED);
            indexedDoc = documentJpaRepository.save(indexedDoc);

            DocumentPdfEntity enrichedDoc = createTestDocument("enriched.pdf", "Enriched");
            enrichedDoc.setStatus(Document.Status.ENRICHMENT_COMPLETED);
            enrichedDoc = documentJpaRepository.save(enrichedDoc);

            entityManager.flush();
            entityManager.clear();

            // Assert
            assertThat(documentJpaRepository.findById(uploadedDoc.getId()).get().getStatus())
                    .isEqualTo(Document.Status.UPLOADED);
            assertThat(documentJpaRepository.findById(indexedDoc.getId()).get().getStatus())
                    .isEqualTo(Document.Status.OCR_COMPLETED);
            assertThat(documentJpaRepository.findById(enrichedDoc.getId()).get().getStatus())
                    .isEqualTo(Document.Status.ENRICHMENT_COMPLETED);
        }
    }

    // Helper methods

    private DocumentPdfEntity createTestDocument(String filename, String title) {
        return DocumentPdfEntity.builder()
                .filename(filename)
                .title(title)
                .contentType("application/pdf")
                .size(1024L)
                .status(Document.Status.UPLOADED)
                .uploadedAt(LocalDateTime.now())
                .owner(testUser)
                .build();
    }

    private DocumentPdfEntity createAndSaveDocument(String filename, String title) {
        DocumentPdfEntity document = createTestDocument(filename, title);
        return documentJpaRepository.save(document);
    }
}
