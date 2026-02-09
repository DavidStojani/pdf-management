package org.papercloud.de.pdfapi.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.core.dto.document.FolderPathDTO;
import org.papercloud.de.pdfservice.service.DocumentService;
import org.papercloud.de.pdfservice.textutils.FolderScannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for DocumentController using MockMvc.
 * Tests REST endpoints for PDF document upload, download, and folder management.
 */
@WebMvcTest(controllers = DocumentController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DocumentController Integration Tests")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private FolderScannerService folderScannerService;

    private DocumentDTO sampleDocumentDTO;
    private DocumentDownloadDTO sampleDownloadDTO;

    @BeforeEach
    void setUp() {
        sampleDocumentDTO = DocumentDTO.builder()
                .id(1L)
                .fileName("test-document.pdf")
                .size(1024L)
                .uploadedAt(LocalDateTime.of(2026, 2, 9, 10, 0))
                .build();

        sampleDownloadDTO = DocumentDownloadDTO.builder()
                .id(1L)
                .fileName("test-document.pdf")
                .size(1024L)
                .content("PDF content".getBytes())
                .contentType("application/pdf")
                .build();
    }

    @Nested
    @DisplayName("Upload Endpoint Tests")
    class UploadEndpointTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should successfully upload valid PDF file")
        void uploadPdf_validPdfFile_returnsSuccessResponse() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.pdf",
                    "application/pdf",
                    "PDF content".getBytes()
            );

            when(documentService.processUpload(any(), any(Authentication.class)))
                    .thenReturn(sampleDocumentDTO);

            // Act & Assert
            mockMvc.perform(multipart("/api/documents/upload")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Document uploaded successfully"))
                    .andExpect(jsonPath("$.documentId").value("1"));

            verify(documentService).processUpload(any(), any(Authentication.class));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should handle upload with non-PDF content type")
        void uploadPdf_nonPdfContentType_processesFile() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "document.txt",
                    "text/plain",
                    "Some text content".getBytes()
            );

            when(documentService.processUpload(any(), any(Authentication.class)))
                    .thenReturn(sampleDocumentDTO);

            // Act & Assert
            mockMvc.perform(multipart("/api/documents/upload")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Document uploaded successfully"));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should handle empty file upload")
        void uploadPdf_emptyFile_processesRequest() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "empty.pdf",
                    "application/pdf",
                    new byte[0]
            );

            DocumentDTO emptyDocDTO = DocumentDTO.builder()
                    .id(2L)
                    .fileName("empty.pdf")
                    .size(0L)
                    .build();

            when(documentService.processUpload(any(), any(Authentication.class)))
                    .thenReturn(emptyDocDTO);

            // Act & Assert
            mockMvc.perform(multipart("/api/documents/upload")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.documentId").value("2"));
        }
    }

    @Nested
    @DisplayName("Download Endpoint Tests")
    class DownloadEndpointTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should successfully download existing document")
        void downloadDocument_existingDocument_returnsDocumentWithHeaders() throws Exception {
            // Arrange
            when(documentService.downloadDocument(eq("testuser"), eq(1L)))
                    .thenReturn(sampleDownloadDTO);

            // Act & Assert
            mockMvc.perform(get("/api/documents/1/download")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"test-document.pdf\""))
                    .andExpect(header().string("Content-Type", "application/pdf"))
                    .andExpect(content().bytes("PDF content".getBytes()));

            verify(documentService).downloadDocument("testuser", 1L);
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should return 404 when document not found")
        void downloadDocument_nonExistentDocument_returns404() throws Exception {
            // Arrange
            when(documentService.downloadDocument(anyString(), eq(999L)))
                    .thenReturn(null);

            // Act & Assert
            mockMvc.perform(get("/api/documents/999/download")
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            verify(documentService).downloadDocument("testuser", 999L);
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should return 403 when access denied")
        void downloadDocument_accessDenied_returns403() throws Exception {
            // Arrange
            when(documentService.downloadDocument(eq("testuser"), eq(1L)))
                    .thenThrow(new AccessDeniedException("Access denied"));

            // Act & Assert
            mockMvc.perform(get("/api/documents/1/download")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "anotheruser")
        @DisplayName("should use authenticated username for download")
        void downloadDocument_differentUser_usesAuthenticatedUsername() throws Exception {
            // Arrange
            DocumentDownloadDTO userDocDTO = DocumentDownloadDTO.builder()
                    .id(5L)
                    .fileName("user-doc.pdf")
                    .size(512L)
                    .content("User content".getBytes())
                    .contentType("application/pdf")
                    .build();

            when(documentService.downloadDocument(eq("anotheruser"), eq(5L)))
                    .thenReturn(userDocDTO);

            // Act & Assert
            mockMvc.perform(get("/api/documents/5/download")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"user-doc.pdf\""));

            verify(documentService).downloadDocument("anotheruser", 5L);
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should handle document with special characters in filename")
        void downloadDocument_specialCharactersInFilename_returnsCorrectHeaders() throws Exception {
            // Arrange
            DocumentDownloadDTO specialNameDTO = DocumentDownloadDTO.builder()
                    .id(3L)
                    .fileName("report (2024-01-15).pdf")
                    .size(2048L)
                    .content("Content".getBytes())
                    .contentType("application/pdf")
                    .build();

            when(documentService.downloadDocument(eq("testuser"), eq(3L)))
                    .thenReturn(specialNameDTO);

            // Act & Assert
            mockMvc.perform(get("/api/documents/3/download")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"report (2024-01-15).pdf\""));
        }
    }

    @Nested
    @DisplayName("Ping Endpoint Tests")
    class PingEndpointTests {

        @Test
        @DisplayName("should respond to ping without authentication")
        void ping_noAuthentication_returnsPong() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/documents/ping"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Pong! Server is running"));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should respond to ping with authentication")
        void ping_withAuthentication_returnsPong() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/documents/ping")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Pong! Server is running"));
        }
    }

    @Nested
    @DisplayName("Folder Management Endpoint Tests")
    class FolderManagementTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should successfully set user folder path")
        void setUserFolder_validFolderPath_returnsSuccessResponse() throws Exception {
            // Arrange
            String folderPath = "/home/testuser/documents";
            String requestBody = "{\"folderPath\":\"" + folderPath + "\"}";

            // Act & Assert
            mockMvc.perform(post("/api/documents/folder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(folderPath));

            verify(folderScannerService).scanUserFolder("testuser", folderPath);
        }

        @Test
        @WithMockUser(username = "admin")
        @DisplayName("should handle folder path for different user")
        void setUserFolder_differentUser_scansCorrectUserFolder() throws Exception {
            // Arrange
            String folderPath = "/var/data/pdfs";
            String requestBody = "{\"folderPath\":\"" + folderPath + "\"}";

            // Act & Assert
            mockMvc.perform(post("/api/documents/folder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(folderPath));

            verify(folderScannerService).scanUserFolder("admin", folderPath);
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should handle Windows-style folder path")
        void setUserFolder_windowsPath_processesCorrectly() throws Exception {
            // Arrange
            String folderPath = "C:\\\\Users\\\\testuser\\\\Documents";
            String requestBody = "{\"folderPath\":\"" + folderPath + "\"}";

            // Act & Assert
            mockMvc.perform(post("/api/documents/folder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(folderScannerService).scanUserFolder(eq("testuser"), anyString());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should handle empty folder path")
        void setUserFolder_emptyPath_processesRequest() throws Exception {
            // Arrange
            String requestBody = "{\"folderPath\":\"\"}";

            // Act & Assert
            mockMvc.perform(post("/api/documents/folder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(""));

            verify(folderScannerService).scanUserFolder("testuser", "");
        }
    }
}
