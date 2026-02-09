package org.papercloud.de.pdfdatabase.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.domain.Page;
import org.papercloud.de.core.domain.User;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.entity.RoleEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DocumentPersistenceMapper using real MapStruct-generated implementation.
 * Tests mappings between domain objects and JPA entities.
 */
@DisplayName("DocumentPersistenceMapper Integration Tests")
class DocumentPersistenceMapperTest {

    private DocumentPersistenceMapper mapper;

    @BeforeEach
    void setUp() {
        // Instantiate the generated mapper implementation directly
        mapper = new DocumentPersistenceMapperImpl();
    }

    @Nested
    @DisplayName("Document Mapping Tests")
    class DocumentMappingTests {

        @Test
        @DisplayName("should map entity to domain with all fields")
        void toDomain_fullEntity_mapsAllFields() {
            // Arrange
            UserEntity owner = UserEntity.builder()
                    .id(1L)
                    .username("testuser")
                    .email("test@example.com")
                    .build();

            PagesPdfEntity page1 = PagesPdfEntity.builder()
                    .id(10L)
                    .pageNumber(1)
                    .pageText("Page 1 text")
                    .build();

            PagesPdfEntity page2 = PagesPdfEntity.builder()
                    .id(11L)
                    .pageNumber(2)
                    .pageText("Page 2 text")
                    .build();

            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(100L)
                    .title("Test Document")
                    .filename("test.pdf")
                    .contentType("application/pdf")
                    .size(2048L)
                    .status(Document.Status.UPLOADED)
                    .pdfContent("PDF bytes".getBytes())
                    .uploadedAt(LocalDateTime.of(2026, 2, 9, 10, 0))
                    .tags(Arrays.asList("tag1", "tag2"))
                    .dateOnDocument(LocalDate.of(2026, 1, 15))
                    .failedEnrichment(false)
                    .owner(owner)
                    .pages(Arrays.asList(page1, page2))
                    .build();

            // Act
            Document domain = mapper.toDomain(entity);

            // Assert
            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isEqualTo(100L);
            assertThat(domain.getTitle()).isEqualTo("Test Document");
            assertThat(domain.getFilename()).isEqualTo("test.pdf");
            assertThat(domain.getContentType()).isEqualTo("application/pdf");
            assertThat(domain.getSize()).isEqualTo(2048L);
            assertThat(domain.getStatus()).isEqualTo(Document.Status.UPLOADED);
            assertThat(domain.getPdfContent()).isEqualTo("PDF bytes".getBytes());
            assertThat(domain.getUploadedAt()).isEqualTo(LocalDateTime.of(2026, 2, 9, 10, 0));
            assertThat(domain.getTags()).containsExactly("tag1", "tag2");
            assertThat(domain.getDateOnDocument()).isEqualTo(LocalDate.of(2026, 1, 15));
            assertThat(domain.isFailedEnrichment()).isFalse();
            assertThat(domain.getOwner()).isNotNull();
            assertThat(domain.getOwner().getId()).isEqualTo(1L);
            assertThat(domain.getPages()).hasSize(2);
            assertThat(domain.getPages().get(0).getPageNumber()).isEqualTo(1);
            assertThat(domain.getPages().get(1).getPageNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle null entity")
        void toDomain_nullEntity_returnsNull() {
            // Act
            Document domain = mapper.toDomain(null);

            // Assert
            assertThat(domain).isNull();
        }

        @Test
        @DisplayName("should map domain to entity with all fields")
        void toEntity_fullDomain_mapsAllFields() {
            // Arrange
            User owner = User.builder()
                    .id(2L)
                    .username("owner")
                    .email("owner@example.com")
                    .build();

            Page page1 = Page.builder()
                    .id(20L)
                    .pageNumber(1)
                    .pageText("Domain page 1")
                    .build();

            Document domain = Document.builder()
                    .id(200L)
                    .title("Domain Document")
                    .filename("domain.pdf")
                    .contentType("application/pdf")
                    .size(4096L)
                    .status(Document.Status.OCR_COMPLETED)
                    .pdfContent("Domain PDF".getBytes())
                    .uploadedAt(LocalDateTime.of(2026, 2, 10, 14, 30))
                    .tags(Arrays.asList("important", "urgent"))
                    .dateOnDocument(LocalDate.of(2026, 2, 1))
                    .failedEnrichment(true)
                    .owner(owner)
                    .pages(List.of(page1))
                    .build();

            // Act
            DocumentPdfEntity entity = mapper.toEntity(domain);

            // Assert
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isEqualTo(200L);
            assertThat(entity.getTitle()).isEqualTo("Domain Document");
            assertThat(entity.getFilename()).isEqualTo("domain.pdf");
            assertThat(entity.getContentType()).isEqualTo("application/pdf");
            assertThat(entity.getSize()).isEqualTo(4096L);
            assertThat(entity.getStatus()).isEqualTo(Document.Status.OCR_COMPLETED);
            assertThat(entity.getPdfContent()).isEqualTo("Domain PDF".getBytes());
            assertThat(entity.getUploadedAt()).isEqualTo(LocalDateTime.of(2026, 2, 10, 14, 30));
            assertThat(entity.getTags()).containsExactly("important", "urgent");
            assertThat(entity.getDateOnDocument()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(entity.isFailedEnrichment()).isTrue();
            assertThat(entity.getOwner()).isNotNull();
            assertThat(entity.getOwner().getId()).isEqualTo(2L);
            assertThat(entity.getPages()).hasSize(1);
        }

        @Test
        @DisplayName("should handle null domain")
        void toEntity_nullDomain_returnsNull() {
            // Act
            DocumentPdfEntity entity = mapper.toEntity(null);

            // Assert
            assertThat(entity).isNull();
        }

        @Test
        @DisplayName("should handle entity with all statuses")
        void toDomain_allStatuses_mapsCorrectly() {
            // Test each status enum value
            Document.Status[] statuses = Document.Status.values();

            for (Document.Status status : statuses) {
                // Arrange
                DocumentPdfEntity entity = DocumentPdfEntity.builder()
                        .id(1L)
                        .filename("test.pdf")
                        .status(status)
                        .build();

                // Act
                Document domain = mapper.toDomain(entity);

                // Assert
                assertThat(domain.getStatus()).isEqualTo(status);
            }
        }

        @Test
        @DisplayName("should handle entity with empty tags list")
        void toDomain_emptyTags_mapsToEmptyList() {
            // Arrange
            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(1L)
                    .filename("test.pdf")
                    .tags(Collections.emptyList())
                    .build();

            // Act
            Document domain = mapper.toDomain(entity);

            // Assert
            assertThat(domain.getTags()).isEmpty();
        }

        @Test
        @DisplayName("should handle entity with null tags")
        void toDomain_nullTags_mapsToNull() {
            // Arrange
            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(1L)
                    .filename("test.pdf")
                    .tags(null)
                    .build();

            // Act
            Document domain = mapper.toDomain(entity);

            // Assert
            // MapStruct creates an empty list for null collections by default
            assertThat(domain.getTags()).isEmpty();
        }

        @Test
        @DisplayName("should handle entity with null owner")
        void toDomain_nullOwner_mapsToNull() {
            // Arrange
            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(1L)
                    .filename("test.pdf")
                    .owner(null)
                    .build();

            // Act
            Document domain = mapper.toDomain(entity);

            // Assert
            assertThat(domain.getOwner()).isNull();
        }

        @Test
        @DisplayName("should handle entity with null pages")
        void toDomain_nullPages_mapsToNull() {
            // Arrange
            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(1L)
                    .filename("test.pdf")
                    .pages(null)
                    .build();

            // Act
            Document domain = mapper.toDomain(entity);

            // Assert
            assertThat(domain.getPages()).isNull();
        }
    }

    @Nested
    @DisplayName("Document List Mapping Tests")
    class DocumentListMappingTests {

        @Test
        @DisplayName("should map list of entities to list of domains")
        void toDomainList_multipleEntities_mapsAllEntities() {
            // Arrange
            List<DocumentPdfEntity> entities = Arrays.asList(
                    DocumentPdfEntity.builder().id(1L).filename("doc1.pdf").build(),
                    DocumentPdfEntity.builder().id(2L).filename("doc2.pdf").build(),
                    DocumentPdfEntity.builder().id(3L).filename("doc3.pdf").build()
            );

            // Act
            List<Document> domains = mapper.toDomainList(entities);

            // Assert
            assertThat(domains).hasSize(3);
            assertThat(domains.get(0).getId()).isEqualTo(1L);
            assertThat(domains.get(0).getFilename()).isEqualTo("doc1.pdf");
            assertThat(domains.get(1).getId()).isEqualTo(2L);
            assertThat(domains.get(2).getId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("should handle null list")
        void toDomainList_nullList_returnsNull() {
            // Act
            List<Document> domains = mapper.toDomainList(null);

            // Assert
            assertThat(domains).isNull();
        }

        @Test
        @DisplayName("should handle empty list")
        void toDomainList_emptyList_returnsEmptyList() {
            // Arrange
            List<DocumentPdfEntity> entities = Collections.emptyList();

            // Act
            List<Document> domains = mapper.toDomainList(entities);

            // Assert
            assertThat(domains).isEmpty();
        }
    }

    @Nested
    @DisplayName("Page Mapping Tests")
    class PageMappingTests {

        @Test
        @DisplayName("should map page entity to page domain")
        void toPageDomain_fullEntity_mapsAllFields() {
            // Arrange
            PagesPdfEntity entity = PagesPdfEntity.builder()
                    .id(50L)
                    .pageNumber(5)
                    .pageText("Page content")
                    .build();

            // Act
            Page domain = mapper.toPageDomain(entity);

            // Assert
            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isEqualTo(50L);
            assertThat(domain.getPageNumber()).isEqualTo(5);
            assertThat(domain.getPageText()).isEqualTo("Page content");
            assertThat(domain.getDocument()).isNull(); // ignored in mapping
        }

        @Test
        @DisplayName("should handle null page entity")
        void toPageDomain_nullEntity_returnsNull() {
            // Act
            Page domain = mapper.toPageDomain(null);

            // Assert
            assertThat(domain).isNull();
        }

        @Test
        @DisplayName("should map page domain to page entity")
        void toPageEntity_fullDomain_mapsAllFields() {
            // Arrange
            Page domain = Page.builder()
                    .id(60L)
                    .pageNumber(6)
                    .pageText("Domain page content")
                    .build();

            // Act
            PagesPdfEntity entity = mapper.toPageEntity(domain);

            // Assert
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isEqualTo(60L);
            assertThat(entity.getPageNumber()).isEqualTo(6);
            assertThat(entity.getPageText()).isEqualTo("Domain page content");
            assertThat(entity.getDocument()).isNull(); // ignored in mapping
        }

        @Test
        @DisplayName("should handle null page domain")
        void toPageEntity_nullDomain_returnsNull() {
            // Act
            PagesPdfEntity entity = mapper.toPageEntity(null);

            // Assert
            assertThat(entity).isNull();
        }

        @Test
        @DisplayName("should map page domain list to page entity list")
        void toPageEntityList_multiplePages_mapsAllPages() {
            // Arrange
            List<Page> domains = Arrays.asList(
                    Page.builder().id(1L).pageNumber(1).pageText("Page 1").build(),
                    Page.builder().id(2L).pageNumber(2).pageText("Page 2").build()
            );

            // Act
            List<PagesPdfEntity> entities = mapper.toPageEntityList(domains);

            // Assert
            assertThat(entities).hasSize(2);
            assertThat(entities.get(0).getId()).isEqualTo(1L);
            assertThat(entities.get(0).getPageNumber()).isEqualTo(1);
            assertThat(entities.get(1).getId()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle null page domain list")
        void toPageEntityList_nullList_returnsNull() {
            // Act
            List<PagesPdfEntity> entities = mapper.toPageEntityList(null);

            // Assert
            assertThat(entities).isNull();
        }

        @Test
        @DisplayName("should map page entity list to page domain list")
        void toPageDomainList_multiplePages_mapsAllPages() {
            // Arrange
            List<PagesPdfEntity> entities = Arrays.asList(
                    PagesPdfEntity.builder().id(10L).pageNumber(1).pageText("Entity page 1").build(),
                    PagesPdfEntity.builder().id(11L).pageNumber(2).pageText("Entity page 2").build()
            );

            // Act
            List<Page> domains = mapper.toPageDomainList(entities);

            // Assert
            assertThat(domains).hasSize(2);
            assertThat(domains.get(0).getId()).isEqualTo(10L);
            assertThat(domains.get(0).getPageText()).isEqualTo("Entity page 1");
            assertThat(domains.get(1).getId()).isEqualTo(11L);
        }

        @Test
        @DisplayName("should handle null page entity list")
        void toPageDomainList_nullList_returnsNull() {
            // Act
            List<Page> domains = mapper.toPageDomainList(null);

            // Assert
            assertThat(domains).isNull();
        }

        @Test
        @DisplayName("should handle empty page lists")
        void toPageDomainList_emptyList_returnsEmptyList() {
            // Arrange
            List<PagesPdfEntity> entities = Collections.emptyList();

            // Act
            List<Page> domains = mapper.toPageDomainList(entities);

            // Assert
            assertThat(domains).isEmpty();
        }
    }

    @Nested
    @DisplayName("User Mapping Tests")
    class UserMappingTests {

        @Test
        @DisplayName("should map user entity to user domain with all fields")
        void toUserDomain_fullEntity_mapsAllFields() {
            // Arrange
            RoleEntity role1 = new RoleEntity();
            role1.setId(1L);
            role1.setName("ROLE_USER");

            RoleEntity role2 = new RoleEntity();
            role2.setId(2L);
            role2.setName("ROLE_ADMIN");

            UserEntity entity = UserEntity.builder()
                    .id(100L)
                    .username("testuser")
                    .email("test@example.com")
                    .password("hashed-password")
                    .enabled(true)
                    .folderPath("/home/testuser/docs")
                    .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                    .lastLoginAt(LocalDateTime.of(2026, 2, 9, 9, 0))
                    .roles(new HashSet<>(Arrays.asList(role1, role2)))
                    .build();

            // Act
            User domain = mapper.toUserDomain(entity);

            // Assert
            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isEqualTo(100L);
            assertThat(domain.getUsername()).isEqualTo("testuser");
            assertThat(domain.getEmail()).isEqualTo("test@example.com");
            assertThat(domain.getPassword()).isEqualTo("hashed-password");
            assertThat(domain.isEnabled()).isTrue();
            assertThat(domain.getFolderPath()).isEqualTo("/home/testuser/docs");
            assertThat(domain.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
            assertThat(domain.getLastLoginAt()).isEqualTo(LocalDateTime.of(2026, 2, 9, 9, 0));
            assertThat(domain.getRoles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("should handle null user entity")
        void toUserDomain_nullEntity_returnsNull() {
            // Act
            User domain = mapper.toUserDomain(null);

            // Assert
            assertThat(domain).isNull();
        }

        @Test
        @DisplayName("should map user domain to user entity with all fields")
        void toUserEntity_fullDomain_mapsAllFields() {
            // Arrange
            User domain = User.builder()
                    .id(200L)
                    .username("domainuser")
                    .email("domain@example.com")
                    .password("domain-password")
                    .enabled(false)
                    .folderPath("/var/data")
                    .createdAt(LocalDateTime.of(2026, 2, 1, 12, 0))
                    .lastLoginAt(LocalDateTime.of(2026, 2, 8, 15, 30))
                    .roles(Set.of("ROLE_USER"))
                    .build();

            // Act
            UserEntity entity = mapper.toUserEntity(domain);

            // Assert
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isEqualTo(200L);
            assertThat(entity.getUsername()).isEqualTo("domainuser");
            assertThat(entity.getEmail()).isEqualTo("domain@example.com");
            assertThat(entity.getPassword()).isEqualTo("domain-password");
            assertThat(entity.isEnabled()).isFalse();
            assertThat(entity.getFolderPath()).isEqualTo("/var/data");
            assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 2, 1, 12, 0));
            assertThat(entity.getLastLoginAt()).isEqualTo(LocalDateTime.of(2026, 2, 8, 15, 30));
            assertThat(entity.getRoles()).isNull(); // roles ignored in toUserEntity mapping
        }

        @Test
        @DisplayName("should handle null user domain")
        void toUserEntity_nullDomain_returnsNull() {
            // Act
            UserEntity entity = mapper.toUserEntity(null);

            // Assert
            assertThat(entity).isNull();
        }

        @Test
        @DisplayName("should convert role entities to role strings")
        void rolesToStrings_multipleRoles_convertsCorrectly() {
            // Arrange
            RoleEntity role1 = new RoleEntity();
            role1.setId(1L);
            role1.setName("ROLE_USER");

            RoleEntity role2 = new RoleEntity();
            role2.setId(2L);
            role2.setName("ROLE_ADMIN");

            RoleEntity role3 = new RoleEntity();
            role3.setId(3L);
            role3.setName("ROLE_MODERATOR");

            Set<RoleEntity> roles = new HashSet<>(Arrays.asList(role1, role2, role3));

            // Act
            Set<String> roleStrings = mapper.rolesToStrings(roles);

            // Assert
            assertThat(roleStrings).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_MODERATOR");
        }

        @Test
        @DisplayName("should handle null roles set")
        void rolesToStrings_nullRoles_returnsEmptySet() {
            // Act
            Set<String> roleStrings = mapper.rolesToStrings(null);

            // Assert
            assertThat(roleStrings).isEmpty();
        }

        @Test
        @DisplayName("should handle empty roles set")
        void rolesToStrings_emptyRoles_returnsEmptySet() {
            // Arrange
            Set<RoleEntity> roles = Collections.emptySet();

            // Act
            Set<String> roleStrings = mapper.rolesToStrings(roles);

            // Assert
            assertThat(roleStrings).isEmpty();
        }

        @Test
        @DisplayName("should handle user entity with single role")
        void toUserDomain_singleRole_convertsSingleRole() {
            // Arrange
            RoleEntity role = new RoleEntity();
            role.setId(1L);
            role.setName("ROLE_USER");

            UserEntity entity = UserEntity.builder()
                    .id(1L)
                    .username("user")
                    .email("user@example.com")
                    .password("pass")
                    .enabled(true)
                    .roles(Set.of(role))
                    .build();

            // Act
            User domain = mapper.toUserDomain(entity);

            // Assert
            assertThat(domain.getRoles()).containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("should handle user entity with null roles")
        void toUserDomain_nullRoles_returnsEmptyRolesSet() {
            // Arrange
            UserEntity entity = UserEntity.builder()
                    .id(1L)
                    .username("user")
                    .email("user@example.com")
                    .password("pass")
                    .enabled(true)
                    .roles(null)
                    .build();

            // Act
            User domain = mapper.toUserDomain(entity);

            // Assert
            assertThat(domain.getRoles()).isEmpty();
        }

        @Test
        @DisplayName("should handle user with null email")
        void toUserDomain_nullEmail_mapsToNull() {
            // Arrange
            UserEntity entity = UserEntity.builder()
                    .id(1L)
                    .username("user")
                    .email(null)
                    .password("pass")
                    .enabled(true)
                    .build();

            // Act
            User domain = mapper.toUserDomain(entity);

            // Assert
            assertThat(domain.getEmail()).isNull();
        }

        @Test
        @DisplayName("should handle user with null timestamps")
        void toUserDomain_nullTimestamps_mapsToNull() {
            // Arrange
            UserEntity entity = UserEntity.builder()
                    .id(1L)
                    .username("user")
                    .email("user@example.com")
                    .password("pass")
                    .enabled(true)
                    .createdAt(null)
                    .lastLoginAt(null)
                    .build();

            // Act
            User domain = mapper.toUserDomain(entity);

            // Assert
            assertThat(domain.getCreatedAt()).isNull();
            assertThat(domain.getLastLoginAt()).isNull();
        }
    }
}
