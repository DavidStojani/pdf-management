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
import org.papercloud.de.core.domain.UploadSource;
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentUploadDTO;
import org.papercloud.de.core.events.OcrEvent;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.pdfservice.errors.DocumentUploadException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.papercloud.de.pdfservice.errors.UserAuthenticationException;
import org.papercloud.de.pdfservice.mapper.DocumentServiceMapper;
import org.papercloud.de.pdfservice.util.ClientInfoExtractor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentUploadServiceImpl Tests")
class DocumentUploadServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentServiceMapper documentMapper;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private AuditService auditService;
    @Mock private ClientInfoExtractor clientInfoExtractor;

    @InjectMocks
    private DocumentUploadServiceImpl uploadService;

    private UserEntity testUser;
    private DocumentPdfEntity testDocument;
    private DocumentDTO testDocumentDTO;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder().id(1L).username("testuser").build();

        testDocument = DocumentPdfEntity.builder()
                .id(1L)
                .filename("test.pdf")
                .contentType(MediaType.APPLICATION_PDF_VALUE)
                .pdfContent("test content".getBytes())
                .size(100L)
                .owner(testUser)
                .status(Document.Status.UPLOADED)
                .build();

        testDocumentDTO = DocumentDTO.builder().id(1L).fileName("test.pdf").build();
    }

    @Nested
    @DisplayName("ProcessUpload Tests")
    class ProcessUploadTests {

        @Test
        @DisplayName("should throw exception when authentication is null")
        void should_throwException_when_authenticationIsNull() {
            MultipartFile file = createValidPdfFile();

            assertThatThrownBy(() -> uploadService.processUpload(file, null, UploadSource.FILE_UPLOAD))
                    .isInstanceOf(UserAuthenticationException.class)
                    .hasMessageContaining("User must be authenticated");

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when authentication is not authenticated")
        void should_throwException_when_authenticationNotAuthenticated() {
            MultipartFile file = createValidPdfFile();
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(false);

            assertThatThrownBy(() -> uploadService.processUpload(file, auth, UploadSource.FILE_UPLOAD))
                    .isInstanceOf(UserAuthenticationException.class)
                    .hasMessageContaining("User must be authenticated");

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when file is null")
        void should_throwException_when_fileIsNull() {
            Authentication auth = createAuthenticatedUser();

            assertThatThrownBy(() -> uploadService.processUpload(null, auth, UploadSource.FILE_UPLOAD))
                    .isInstanceOf(InvalidDocumentException.class)
                    .hasMessageContaining("Uploaded file must not be empty");

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when file is empty")
        void should_throwException_when_fileIsEmpty() {
            MultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);
            Authentication auth = createAuthenticatedUser();

            assertThatThrownBy(() -> uploadService.processUpload(file, auth, UploadSource.FILE_UPLOAD))
                    .isInstanceOf(InvalidDocumentException.class)
                    .hasMessageContaining("Uploaded file must not be empty");

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when content type is not PDF")
        void should_throwException_when_wrongContentType() {
            MultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "content".getBytes());
            Authentication auth = createAuthenticatedUser();

            assertThatThrownBy(() -> uploadService.processUpload(file, auth, UploadSource.FILE_UPLOAD))
                    .isInstanceOf(InvalidDocumentException.class)
                    .hasMessageContaining("Invalid file format. Only PDF files are allowed");

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should successfully process upload with valid file and authentication")
        void should_successfullyProcessUpload_when_validFileAndAuth() throws IOException {
            MultipartFile file = createValidPdfFile();
            Authentication auth = createAuthenticatedUser();

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(documentRepository.save(any(DocumentPdfEntity.class))).thenReturn(testDocument);
            when(documentMapper.toDocumentDTO(any(DocumentPdfEntity.class))).thenReturn(testDocumentDTO);

            DocumentDTO result = uploadService.processUpload(file, auth, UploadSource.FILE_UPLOAD);

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
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn(MediaType.APPLICATION_PDF_VALUE);
            when(file.getBytes()).thenThrow(new IOException("Failed to read file"));

            Authentication auth = createAuthenticatedUser();

            assertThatThrownBy(() -> uploadService.processUpload(file, auth, UploadSource.FILE_UPLOAD))
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
        void should_throwException_when_userNotFound() throws IOException {
            DocumentUploadDTO uploadDTO = DocumentUploadDTO.builder()
                    .fileName("test.pdf")
                    .contentType(MediaType.APPLICATION_PDF_VALUE)
                    .inputPdfBytes("content".getBytes())
                    .size(100L)
                    .build();

            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> uploadService.processDocument(uploadDTO, "nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found: nonexistent");

            verify(documentRepository, never()).save(any());
            verify(publisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should successfully process document and publish OCR event")
        void should_successfullyProcessDocument_when_userExists() throws IOException {
            DocumentUploadDTO uploadDTO = DocumentUploadDTO.builder()
                    .fileName("test.pdf")
                    .contentType(MediaType.APPLICATION_PDF_VALUE)
                    .inputPdfBytes("content".getBytes())
                    .size(100L)
                    .build();

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(documentRepository.save(any(DocumentPdfEntity.class))).thenReturn(testDocument);
            when(documentMapper.toDocumentDTO(any(DocumentPdfEntity.class))).thenReturn(testDocumentDTO);

            DocumentDTO result = uploadService.processDocument(uploadDTO, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);

            verify(documentRepository).save(any(DocumentPdfEntity.class));

            ArgumentCaptor<OcrEvent> eventCaptor = ArgumentCaptor.forClass(OcrEvent.class);
            verify(publisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().documentId()).isEqualTo(1L);
        }
    }

    private MultipartFile createValidPdfFile() {
        return new MockMultipartFile(
                "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes());
    }

    private Authentication createAuthenticatedUser() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        lenient().when(auth.getName()).thenReturn("testuser");
        return auth;
    }
}
