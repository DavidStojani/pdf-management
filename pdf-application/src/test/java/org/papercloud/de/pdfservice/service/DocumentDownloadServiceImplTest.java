package org.papercloud.de.pdfservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.mapper.DocumentServiceMapper;
import org.papercloud.de.pdfservice.util.ClientInfoExtractor;
import org.springframework.http.MediaType;

import java.nio.file.AccessDeniedException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentDownloadServiceImpl Tests")
class DocumentDownloadServiceImplTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentServiceMapper documentMapper;
    @Mock private AuditService auditService;
    @Mock private ClientInfoExtractor clientInfoExtractor;

    @InjectMocks
    private DocumentDownloadServiceImpl downloadService;

    private UserEntity testUser;
    private DocumentPdfEntity testDocument;

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
    }

    @Nested
    @DisplayName("DownloadDocument Tests")
    class DownloadDocumentTests {

        @Test
        @DisplayName("should throw exception when document not found")
        void should_throwException_when_documentNotFound() {
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> downloadService.downloadDocument("testuser", 999L))
                    .isInstanceOf(DocumentNotFoundException.class)
                    .hasMessageContaining("Document not found with id: 999");
        }

        @Test
        @DisplayName("should throw AccessDeniedException when user is not the owner")
        void should_throwAccessDeniedException_when_wrongOwner() {
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

            assertThatThrownBy(() -> downloadService.downloadDocument("wronguser", 1L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("You are not allowed to access this document");
        }

        @Test
        @DisplayName("should successfully download document when user is the owner")
        void should_successfullyDownloadDocument_when_userIsOwner() throws AccessDeniedException {
            DocumentDownloadDTO downloadDTO = DocumentDownloadDTO.builder()
                    .id(1L)
                    .fileName("test.pdf")
                    .content("content".getBytes())
                    .build();

            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(documentMapper.toDownloadDTO(testDocument)).thenReturn(downloadDTO);

            DocumentDownloadDTO result = downloadService.downloadDocument("testuser", 1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFileName()).isEqualTo("test.pdf");

            verify(documentMapper).toDownloadDTO(testDocument);
        }

        @Test
        @DisplayName("should record audit on successful download")
        void should_recordAudit_on_successfulDownload() throws AccessDeniedException {
            DocumentDownloadDTO downloadDTO = DocumentDownloadDTO.builder()
                    .id(1L).fileName("test.pdf").content("content".getBytes()).build();

            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(documentMapper.toDownloadDTO(testDocument)).thenReturn(downloadDTO);
            when(clientInfoExtractor.getClientIp()).thenReturn("127.0.0.1");
            when(clientInfoExtractor.getClientUserAgent()).thenReturn("TestAgent/1.0");

            downloadService.downloadDocument("testuser", 1L);

            verify(auditService).recordAction(
                    org.mockito.ArgumentMatchers.eq(1L),
                    org.mockito.ArgumentMatchers.eq("testuser"),
                    org.mockito.ArgumentMatchers.eq(org.papercloud.de.core.domain.AuditActionType.DOWNLOADED),
                    org.mockito.ArgumentMatchers.eq("127.0.0.1"),
                    org.mockito.ArgumentMatchers.eq("TestAgent/1.0"),
                    org.mockito.ArgumentMatchers.isNull()
            );
        }
    }
}
