package integration.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import integration.base.BaseIntegrationTest;
import integration.builders.DocumentTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.pdfservice.search.DocumentService;
import org.papercloud.de.pdfservice.textutils.FolderScannerService;
import org.papercloud.de.pdfservice.errors.DocumentUploadException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Document Controller Integration Tests")
class DocumentControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private FolderScannerService folderScannerService;

    @Nested
    @DisplayName("PDF Upload Tests")
    class PdfUploadTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should successfully upload valid PDF")
        void shouldUploadValidPdf() throws Exception {
            // Given
            MockMultipartFile file = DocumentTestDataBuilder.createValidPdfFile();
            DocumentDTO mockResponse = createMockDocumentDTO();

            when(documentService.processUpload(any(MultipartFile.class), any(Authentication.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(multipart("/api/documents/upload")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Document uploaded successfully"))
                    .andExpect(jsonPath("$.documentId").value("1"));

            verify(documentService).processUpload(any(MultipartFile.class), any(Authentication.class));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should reject invalid file format")
        void shouldRejectInvalidFileFormat() throws Exception {
            // Given
            MockMultipartFile file = DocumentTestDataBuilder.createInvalidFile();

            when(documentService.processUpload(any(MultipartFile.class), any(Authentication.class)))
                    .thenThrow(new InvalidDocumentException("Invalid file format. Only PDF files are allowed."));

            // When & Then
            mockMvc.perform(multipart("/api/documents/upload")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid file format. Only PDF files are allowed."));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should reject empty file")
        void shouldRejectEmptyFile() throws Exception {
            // Given
            MockMultipartFile file = DocumentTestDataBuilder.createEmptyFile();

            when(documentService.processUpload(any(MultipartFile.class), any(Authentication.class)))
                    .thenThrow(new InvalidDocumentException("Uploaded file must not be empty."));

            // When & Then
            mockMvc.perform(multipart("/api/documents/upload")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Uploaded file must not be empty."));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should handle service exceptions gracefully")
        void shouldHandleServiceExceptions() throws Exception {
            // Given
            MockMultipartFile file = DocumentTestDataBuilder.createValidPdfFile();

            when(documentService.processUpload(any(MultipartFile.class), any(Authentication.class)))
                    .thenThrow(new DocumentUploadException("Database connection failed"));

            // When & Then
            mockMvc.perform(multipart("/api/documents/upload")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Database connection failed"));
        }
    }

    @Nested
    @DisplayName("Document Download Tests")
    class DocumentDownloadTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should successfully download existing document")
        void shouldDownloadExistingDocument() throws Exception {
            Long documentId = 1L;
            DocumentDownloadDTO mockDocument = createMockDownloadDTO();

            when(documentService.downloadDocument("testuser", documentId)).thenReturn(mockDocument);

            mockMvc.perform(get("/api/documents/{id}/download", documentId))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(content().bytes(mockDocument.getContent()));

            verify(documentService).downloadDocument("testuser", documentId);
        }


        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should return 404 for non-existent document")
        void shouldReturn404ForNonExistentDocument() throws Exception {
            // Given
            Long documentId = 999L;

            when(documentService.downloadDocument("testuser", documentId))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(get("/api/documents/{id}/download", documentId))
                    .andExpect(status().isNotFound());

            verify(documentService).downloadDocument("testuser", documentId);
        }
    }

        @Nested
        @DisplayName("Folder Management Tests")
        class FolderManagementTests {

            @Test
            @WithMockUser(username = "testuser")
            @DisplayName("Should successfully set user folder")
            void shouldSetUserFolder() throws Exception {
                // Given
                String folderPath = "/home/testuser/documents";
                Map<String, String> request = Map.of("folderPath", folderPath);

                doNothing().when(folderScannerService)
                        .scanUserFolder("testuser", folderPath);

                // When & Then
                mockMvc.perform(post("/api/documents/folder")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(folderPath));

                verify(folderScannerService).scanUserFolder("testuser", folderPath);
            }
        }

        @Nested
        @DisplayName("Health Check Tests")
        class HealthCheckTests {

            @Test
            @DisplayName("Should respond to ping endpoint")
            void shouldRespondToPing() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/documents/ping"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("Pong! Server is running"));
            }
        }

        // ==================== HELPER METHODS ====================

        private DocumentDTO createMockDocumentDTO() {
            return DocumentDTO.builder()
                    .id(1L)
                    .fileName("test-document.pdf")
                    .size(1024L)
                    .build();
        }

        private DocumentDownloadDTO createMockDownloadDTO() {
            return DocumentDownloadDTO.builder()
                    .fileName("test-document.pdf")
                    .contentType("application/pdf")
                    .size(1024L)
                    .content("PDF content".getBytes())
                    .build();
        }
    }