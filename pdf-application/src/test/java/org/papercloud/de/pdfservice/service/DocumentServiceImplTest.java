package org.papercloud.de.pdfservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.domain.UploadSource;
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.core.dto.document.DocumentListItemDTO;
import org.papercloud.de.core.dto.document.DocumentUploadDTO;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DocumentServiceImpl delegation.
 * Verifies that each method routes to the correct sub-service.
 * Detailed logic tests live in the individual service impl test classes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentServiceImpl Delegation Tests")
class DocumentServiceImplTest {

    @Mock private DocumentUploadService documentUploadService;
    @Mock private DocumentDownloadService documentDownloadService;
    @Mock private DocumentSearchService documentSearchService;
    @Mock private DocumentFavouriteService documentFavouriteService;

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Test
    @DisplayName("processUpload delegates to DocumentUploadService")
    void processUpload_delegatesToUploadService() {
        var file = mock(org.springframework.web.multipart.MultipartFile.class);
        var auth = mock(Authentication.class);
        var expected = DocumentDTO.builder().id(1L).build();
        when(documentUploadService.processUpload(file, auth, UploadSource.FILE_UPLOAD)).thenReturn(expected);

        DocumentDTO result = documentService.processUpload(file, auth, UploadSource.FILE_UPLOAD);

        assertThat(result).isSameAs(expected);
        verify(documentUploadService).processUpload(file, auth, UploadSource.FILE_UPLOAD);
    }

    @Test
    @DisplayName("processDocument delegates to DocumentUploadService")
    void processDocument_delegatesToUploadService() throws IOException {
        var dto = DocumentUploadDTO.builder().fileName("test.pdf").build();
        var expected = DocumentDTO.builder().id(2L).build();
        when(documentUploadService.processDocument(dto, "user")).thenReturn(expected);

        DocumentDTO result = documentService.processDocument(dto, "user");

        assertThat(result).isSameAs(expected);
        verify(documentUploadService).processDocument(dto, "user");
    }

    @Test
    @DisplayName("downloadDocument delegates to DocumentDownloadService")
    void downloadDocument_delegatesToDownloadService() throws AccessDeniedException {
        var expected = DocumentDownloadDTO.builder().id(3L).build();
        when(documentDownloadService.downloadDocument("user", 3L)).thenReturn(expected);

        DocumentDownloadDTO result = documentService.downloadDocument("user", 3L);

        assertThat(result).isSameAs(expected);
        verify(documentDownloadService).downloadDocument("user", 3L);
    }

    @Test
    @DisplayName("searchDocuments delegates to DocumentSearchService")
    void searchDocuments_delegatesToSearchService() {
        var expected = List.of(DocumentListItemDTO.builder().id(4L).build());
        when(documentSearchService.searchDocuments("user", "query")).thenReturn(expected);

        List<DocumentListItemDTO> result = documentService.searchDocuments("user", "query");

        assertThat(result).isSameAs(expected);
        verify(documentSearchService).searchDocuments("user", "query");
    }

    @Test
    @DisplayName("getFavourites delegates to DocumentFavouriteService")
    void getFavourites_delegatesToFavouriteService() {
        var expected = List.of(DocumentListItemDTO.builder().id(5L).build());
        when(documentFavouriteService.getFavourites("user")).thenReturn(expected);

        List<DocumentListItemDTO> result = documentService.getFavourites("user");

        assertThat(result).isSameAs(expected);
        verify(documentFavouriteService).getFavourites("user");
    }

    @Test
    @DisplayName("addFavourite delegates to DocumentFavouriteService")
    void addFavourite_delegatesToFavouriteService() {
        documentService.addFavourite(6L, "user");

        verify(documentFavouriteService).addFavourite(6L, "user");
    }

    @Test
    @DisplayName("removeFavourite delegates to DocumentFavouriteService")
    void removeFavourite_delegatesToFavouriteService() {
        documentService.removeFavourite(7L, "user");

        verify(documentFavouriteService).removeFavourite(7L, "user");
    }
}
