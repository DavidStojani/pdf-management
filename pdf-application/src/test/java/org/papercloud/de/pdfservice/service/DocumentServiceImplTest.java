package org.papercloud.de.pdfservice.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.domain.UploadSource;
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.core.dto.document.DocumentUploadDTO;
import org.papercloud.de.core.dto.search.SearchHitDTO;
import org.papercloud.de.core.dto.search.SearchRequestDTO;
import org.papercloud.de.core.dto.search.SearchResultDTO;
import org.papercloud.de.core.events.OcrEvent;
import org.papercloud.de.core.ports.outbound.SearchService;
import org.papercloud.de.core.dto.document.DocumentListItemDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserDocumentFavouriteEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.FavouriteRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.errors.DocumentUploadException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.papercloud.de.pdfservice.errors.UserAuthenticationException;
import org.papercloud.de.pdfservice.mapper.DocumentServiceMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DocumentServiceImpl.
 * Tests document upload processing, download, and validation logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentServiceImpl Tests")
class DocumentServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FavouriteRepository favouriteRepository;

    @Mock
    private DocumentServiceMapper documentMapper;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private SearchService searchService;

    @Mock
    private AuditService auditService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private UserEntity testUser;
    private DocumentPdfEntity testDocument;
    private DocumentDTO testDocumentDTO;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .build();

        testDocument = DocumentPdfEntity.builder()
                .id(1L)
                .filename("test.pdf")
                .contentType(MediaType.APPLICATION_PDF_VALUE)
                .pdfContent("test content".getBytes())
                .size(100L)
                .owner(testUser)
                .status(Document.Status.UPLOADED)
                .build();

        testDocumentDTO = DocumentDTO.builder()
                .id(1L)
                .fileName("test.pdf")
                .build();
    }

    @Nested
    @DisplayName("ProcessUpload Tests")
    class ProcessUploadTests {

        @Test
        @DisplayName("should throw exception when authentication is null")
        void should_throwException_when_authenticationIsNull() {
            // Arrange
            MultipartFile file = createValidPdfFile();

            // Act & Assert
            assertThatThrownBy(() -> documentService.processUpload(file, null, UploadSource.FILE_UPLOAD))
                    .isInstanceOf(UserAuthenticationException.class)
                    .hasMessageContaining("User must be authenticated");

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when authentication is not authenticated")
        void should_throwException_when_authenticationNotAuthenticated() {
            // Arrange
            MultipartFile file = createValidPdfFile();
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> documentService.processUpload(file, auth, UploadSource.FILE_UPLOAD))
                    .isInstanceOf(UserAuthenticationException.class)
                    .hasMessageContaining("User must be authenticated");

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when file is null")
        void should_throwException_when_fileIsNull() {
            // Arrange
            Authentication auth = createAuthenticatedUser();

            // Act & Assert
            assertThatThrownBy(() -> documentService.processUpload(null, auth, UploadSource.FILE_UPLOAD))
                    .isInstanceOf(InvalidDocumentException.class)
                    .hasMessageContaining("Uploaded file must not be empty");

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when file is empty")
        void should_throwException_when_fileIsEmpty() {
            // Arrange
            MultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);
            Authentication auth = createAuthenticatedUser();

            // Act & Assert
            assertThatThrownBy(() -> documentService.processUpload(file, auth, UploadSource.FILE_UPLOAD))
                    .isInstanceOf(InvalidDocumentException.class)
                    .hasMessageContaining("Uploaded file must not be empty");

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when content type is not PDF")
        void should_throwException_when_wrongContentType() {
            // Arrange
            MultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "content".getBytes());
            Authentication auth = createAuthenticatedUser();

            // Act & Assert
            assertThatThrownBy(() -> documentService.processUpload(file, auth, UploadSource.FILE_UPLOAD))
                    .isInstanceOf(InvalidDocumentException.class)
                    .hasMessageContaining("Invalid file format. Only PDF files are allowed");

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should successfully process upload with valid file and authentication")
        void should_successfullyProcessUpload_when_validFileAndAuth() throws IOException {
            // Arrange
            MultipartFile file = createValidPdfFile();
            Authentication auth = createAuthenticatedUser();

            when(userRepository.findByUsername("testuser"))
                    .thenReturn(Optional.of(testUser));
            when(documentRepository.save(any(DocumentPdfEntity.class)))
                    .thenReturn(testDocument);
            when(documentMapper.toDocumentDTO(any(DocumentPdfEntity.class)))
                    .thenReturn(testDocumentDTO);

            // Act
            DocumentDTO result = documentService.processUpload(file, auth, UploadSource.FILE_UPLOAD);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFileName()).isEqualTo("test.pdf");

            ArgumentCaptor<DocumentPdfEntity> docCaptor = ArgumentCaptor.forClass(DocumentPdfEntity.class);
            verify(documentRepository).save(docCaptor.capture());

            DocumentPdfEntity savedDoc = docCaptor.getValue();
            assertThat(savedDoc.getFilename()).isEqualTo("test.pdf");
            assertThat(savedDoc.getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
            assertThat(savedDoc.getStatus()).isEqualTo(Document.Status.UPLOADED);
            assertThat(savedDoc.getOwner()).isEqualTo(testUser);

            ArgumentCaptor<OcrEvent> eventCaptor = ArgumentCaptor.forClass(OcrEvent.class);
            verify(publisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().documentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw DocumentUploadException when file.getBytes throws IOException")
        void should_throwDocumentUploadException_when_iOExceptionOnGetBytes() throws IOException {
            // Arrange
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn(MediaType.APPLICATION_PDF_VALUE);
            when(file.getBytes()).thenThrow(new IOException("Failed to read file"));

            Authentication auth = createAuthenticatedUser();

            // Act & Assert
            assertThatThrownBy(() -> documentService.processUpload(file, auth, UploadSource.FILE_UPLOAD))
                    .isInstanceOf(DocumentUploadException.class)
                    .hasMessageContaining("Failed to read uploaded file");

            verify(documentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("ProcessDocument Tests")
    class ProcessDocumentTests {

        @Test
        @DisplayName("should throw exception when user not found")
        void should_throwException_when_userNotFound() {
            // Arrange
            DocumentUploadDTO uploadDTO = DocumentUploadDTO.builder()
                    .fileName("test.pdf")
                    .contentType(MediaType.APPLICATION_PDF_VALUE)
                    .inputPdfBytes("content".getBytes())
                    .size(100L)
                    .build();

            when(userRepository.findByUsername("nonexistent"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> documentService.processDocument(uploadDTO, "nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found: nonexistent");

            verify(documentRepository, never()).save(any());
            verify(publisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should successfully process document and publish OCR event")
        void should_successfullyProcessDocument_when_userExists() throws IOException {
            // Arrange
            DocumentUploadDTO uploadDTO = DocumentUploadDTO.builder()
                    .fileName("test.pdf")
                    .contentType(MediaType.APPLICATION_PDF_VALUE)
                    .inputPdfBytes("content".getBytes())
                    .size(100L)
                    .build();

            when(userRepository.findByUsername("testuser"))
                    .thenReturn(Optional.of(testUser));
            when(documentRepository.save(any(DocumentPdfEntity.class)))
                    .thenReturn(testDocument);
            when(documentMapper.toDocumentDTO(any(DocumentPdfEntity.class)))
                    .thenReturn(testDocumentDTO);

            // Act
            DocumentDTO result = documentService.processDocument(uploadDTO, "testuser");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);

            verify(documentRepository).save(any(DocumentPdfEntity.class));

            ArgumentCaptor<OcrEvent> eventCaptor = ArgumentCaptor.forClass(OcrEvent.class);
            verify(publisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().documentId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("DownloadDocument Tests")
    class DownloadDocumentTests {

        @Test
        @DisplayName("should throw exception when document not found")
        void should_throwException_when_documentNotFound() {
            // Arrange
            when(documentRepository.findById(999L))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> documentService.downloadDocument("testuser", 999L))
                    .isInstanceOf(DocumentNotFoundException.class)
                    .hasMessageContaining("Document not found with id: 999");
        }

        @Test
        @DisplayName("should throw AccessDeniedException when user is not the owner")
        void should_throwAccessDeniedException_when_wrongOwner() {
            // Arrange
            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));

            // Act & Assert
            assertThatThrownBy(() -> documentService.downloadDocument("wronguser", 1L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("You are not allowed to access this document");
        }

        @Test
        @DisplayName("should successfully download document when user is the owner")
        void should_successfullyDownloadDocument_when_userIsOwner() throws AccessDeniedException {
            // Arrange
            DocumentDownloadDTO downloadDTO = DocumentDownloadDTO.builder()
                    .id(1L)
                    .fileName("test.pdf")
                    .content("content".getBytes())
                    .build();

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(documentMapper.toDownloadDTO(testDocument))
                    .thenReturn(downloadDTO);

            // Act
            DocumentDownloadDTO result = documentService.downloadDocument("testuser", 1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFileName()).isEqualTo("test.pdf");

            verify(documentMapper).toDownloadDTO(testDocument);
        }
    }

    @Nested
    @DisplayName("SearchDocuments Tests")
    class SearchDocumentsTests {

        @Test
        @DisplayName("should use UPLOAD_# title when document has no enriched title")
        void should_useUploadTitle_when_noEnrichedTitle() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(42L).filename("report.pdf").title(null).owner(testUser).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = documentService.searchDocuments("testuser", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("UPLOAD_#42");
        }

        @Test
        @DisplayName("should use enriched title when available")
        void should_useEnrichedTitle_when_available() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L).filename("report.pdf").title("Tax Report 2024").owner(testUser).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = documentService.searchDocuments("testuser", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Tax Report 2024");
        }

        @Test
        @DisplayName("should populate page count from pages list")
        void should_populatePageCount() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L).filename("report.pdf").owner(testUser)
                    .pages(List.of(
                            PagesPdfEntity.builder().id(1L).pageNumber(1).build(),
                            PagesPdfEntity.builder().id(2L).pageNumber(2).build(),
                            PagesPdfEntity.builder().id(3L).pageNumber(3).build()
                    ))
                    .build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = documentService.searchDocuments("testuser", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPageCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return zero page count when pages is null")
        void should_returnZeroPageCount_when_pagesNull() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L).filename("report.pdf").owner(testUser).pages(null).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = documentService.searchDocuments("testuser", null);

            assertThat(result.get(0).getPageCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should filter by display title in fallback mode")
        void should_filterByDisplayTitle() {
            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenThrow(new RuntimeException("ES unavailable"));

            DocumentPdfEntity doc1 = DocumentPdfEntity.builder()
                    .id(42L).filename("a.pdf").title(null).owner(testUser).build();
            DocumentPdfEntity doc2 = DocumentPdfEntity.builder()
                    .id(99L).filename("b.pdf").title(null).owner(testUser).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc1, doc2));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = documentService.searchDocuments("testuser", "upload_#42");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("should filter by filename in fallback mode")
        void should_filterByFilename() {
            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenThrow(new RuntimeException("ES unavailable"));

            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L).filename("invoice-2024.pdf").title(null).owner(testUser).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = documentService.searchDocuments("testuser", "invoice");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should delegate to Elasticsearch when query is present")
        void should_delegateToElasticsearch_when_queryPresent() {
            SearchResultDTO esResult = SearchResultDTO.builder()
                    .hits(List.of(
                            SearchHitDTO.builder().documentId("2").documentName("Doc B").build(),
                            SearchHitDTO.builder().documentId("1").documentName("Doc A").build()
                    ))
                    .totalHits(2).totalPages(1).currentPage(0).build();

            when(searchService.search(any(SearchRequestDTO.class))).thenReturn(esResult);
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of(2L));

            DocumentPdfEntity doc1 = DocumentPdfEntity.builder()
                    .id(1L).filename("a.pdf").title("Doc A").owner(testUser).build();
            DocumentPdfEntity doc2 = DocumentPdfEntity.builder()
                    .id(2L).filename("b.pdf").title("Doc B").owner(testUser).build();
            when(documentRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(doc1, doc2));

            List<DocumentListItemDTO> result = documentService.searchDocuments("testuser", "some query");

            assertThat(result).hasSize(2);
            // Preserves ES relevance order: doc2 first, doc1 second
            assertThat(result.get(0).getId()).isEqualTo(2L);
            assertThat(result.get(0).getIsFavourite()).isTrue();
            assertThat(result.get(1).getId()).isEqualTo(1L);
            assertThat(result.get(1).getIsFavourite()).isFalse();

            verify(searchService).search(any(SearchRequestDTO.class));
        }

        @Test
        @DisplayName("should fall back to in-memory search when Elasticsearch fails")
        void should_fallBackToInMemory_when_elasticsearchFails() {
            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenThrow(new RuntimeException("ES connection failed"));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L).filename("invoice-2024.pdf").title("Invoice 2024").owner(testUser).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc));

            List<DocumentListItemDTO> result = documentService.searchDocuments("testuser", "invoice");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Invoice 2024");
        }

        @Test
        @DisplayName("should return empty list when Elasticsearch returns no hits")
        void should_returnEmptyList_when_noElasticsearchHits() {
            SearchResultDTO emptyResult = SearchResultDTO.builder()
                    .hits(List.of()).totalHits(0).totalPages(0).currentPage(0).build();
            when(searchService.search(any(SearchRequestDTO.class))).thenReturn(emptyResult);
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = documentService.searchDocuments("testuser", "nonexistent");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should skip documents not found in DB after ES search")
        void should_skipMissingDocuments_afterElasticsearchSearch() {
            SearchResultDTO esResult = SearchResultDTO.builder()
                    .hits(List.of(
                            SearchHitDTO.builder().documentId("1").documentName("Doc A").build(),
                            SearchHitDTO.builder().documentId("999").documentName("Deleted").build()
                    ))
                    .totalHits(2).totalPages(1).currentPage(0).build();

            when(searchService.search(any(SearchRequestDTO.class))).thenReturn(esResult);
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            DocumentPdfEntity doc1 = DocumentPdfEntity.builder()
                    .id(1L).filename("a.pdf").title("Doc A").owner(testUser).build();
            when(documentRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(doc1));

            List<DocumentListItemDTO> result = documentService.searchDocuments("testuser", "query");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should mark favourited documents in search results")
        void should_markFavouritedDocuments() {
            DocumentPdfEntity doc1 = DocumentPdfEntity.builder()
                    .id(1L).filename("a.pdf").owner(testUser).build();
            DocumentPdfEntity doc2 = DocumentPdfEntity.builder()
                    .id(2L).filename("b.pdf").owner(testUser).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc1, doc2));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of(1L));

            List<DocumentListItemDTO> result = documentService.searchDocuments("testuser", null);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getIsFavourite()).isTrue();
            assertThat(result.get(1).getIsFavourite()).isFalse();
        }
    }

    @Nested
    @DisplayName("Favourites Tests")
    class FavouritesTests {

        @Test
        @DisplayName("should add favourite idempotently")
        void should_addFavourite_idempotently() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(favouriteRepository.existsByUserIdAndDocumentId(1L, 1L)).thenReturn(true);

            documentService.addFavourite(1L, "testuser");

            verify(favouriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should save new favourite when not already favourited")
        void should_saveNewFavourite() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(favouriteRepository.existsByUserIdAndDocumentId(1L, 1L)).thenReturn(false);

            documentService.addFavourite(1L, "testuser");

            ArgumentCaptor<UserDocumentFavouriteEntity> captor = ArgumentCaptor.forClass(UserDocumentFavouriteEntity.class);
            verify(favouriteRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isEqualTo(testUser);
            assertThat(captor.getValue().getDocument()).isEqualTo(testDocument);
        }

        @Test
        @DisplayName("should remove favourite")
        void should_removeFavourite() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            documentService.removeFavourite(1L, "testuser");

            verify(favouriteRepository).deleteByUserIdAndDocumentId(1L, 1L);
        }

        @Test
        @DisplayName("should return favourite documents with isFavourite=true")
        void should_returnFavouriteDocs() {
            UserDocumentFavouriteEntity fav = UserDocumentFavouriteEntity.builder()
                    .id(1L).user(testUser).document(testDocument).build();
            when(favouriteRepository.findByUserUsernameWithDocument("testuser")).thenReturn(List.of(fav));

            List<DocumentListItemDTO> result = documentService.getFavourites("testuser");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsFavourite()).isTrue();
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw exception when addFavourite with unknown user")
        void should_throwException_when_addFavourite_userNotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.addFavourite(1L, "unknown"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found: unknown");

            verify(favouriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when addFavourite with unknown document")
        void should_throwException_when_addFavourite_documentNotFound() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.addFavourite(999L, "testuser"))
                    .isInstanceOf(DocumentNotFoundException.class)
                    .hasMessageContaining("Document not found with id: 999");

            verify(favouriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when removeFavourite with unknown user")
        void should_throwException_when_removeFavourite_userNotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.removeFavourite(1L, "unknown"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found: unknown");

            verify(favouriteRepository, never()).deleteByUserIdAndDocumentId(any(), any());
        }

        @Test
        @DisplayName("should return empty list when no favourites exist")
        void should_returnEmptyList_when_noFavourites() {
            when(favouriteRepository.findByUserUsernameWithDocument("testuser")).thenReturn(List.of());

            List<DocumentListItemDTO> result = documentService.getFavourites("testuser");

            assertThat(result).isEmpty();
        }
    }

    private MultipartFile createValidPdfFile() {
        return new MockMultipartFile(
                "file",
                "test.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );
    }

    private Authentication createAuthenticatedUser() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        lenient().when(auth.getName()).thenReturn("testuser");
        return auth;
    }
}
