package org.papercloud.de.pdfservice.service;

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
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.core.dto.document.DocumentUploadDTO;
import org.papercloud.de.core.events.OcrEvent;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
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
import java.util.Optional;

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
    private DocumentServiceMapper documentMapper;

    @Mock
    private ApplicationEventPublisher publisher;

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
            assertThatThrownBy(() -> documentService.processUpload(file, null))
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
            assertThatThrownBy(() -> documentService.processUpload(file, auth))
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
            assertThatThrownBy(() -> documentService.processUpload(null, auth))
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
            assertThatThrownBy(() -> documentService.processUpload(file, auth))
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
            assertThatThrownBy(() -> documentService.processUpload(file, auth))
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
            DocumentDTO result = documentService.processUpload(file, auth);

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
            assertThatThrownBy(() -> documentService.processUpload(file, auth))
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
