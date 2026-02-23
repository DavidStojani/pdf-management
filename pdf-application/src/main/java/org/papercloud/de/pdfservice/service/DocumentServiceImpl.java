package org.papercloud.de.pdfservice.service;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.core.domain.UploadSource;
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.core.dto.document.DocumentListItemDTO;
import org.papercloud.de.core.dto.document.DocumentUploadDTO;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentUploadService documentUploadService;
    private final DocumentDownloadService documentDownloadService;
    private final DocumentSearchService documentSearchService;
    private final DocumentFavouriteService documentFavouriteService;

    @Override
    public DocumentDTO processUpload(MultipartFile file, Authentication authentication, UploadSource uploadSource) {
        return documentUploadService.processUpload(file, authentication, uploadSource);
    }

    @Override
    public DocumentDTO processDocument(DocumentUploadDTO file, String username) throws IOException {
        return documentUploadService.processDocument(file, username);
    }

    @Override
    public DocumentDownloadDTO downloadDocument(String username, Long id) throws AccessDeniedException {
        return documentDownloadService.downloadDocument(username, id);
    }

    @Override
    public List<DocumentListItemDTO> searchDocuments(String username, String query) {
        return documentSearchService.searchDocuments(username, query);
    }

    @Override
    public List<DocumentListItemDTO> getFavourites(String username) {
        return documentFavouriteService.getFavourites(username);
    }

    @Override
    public void addFavourite(Long documentId, String username) {
        documentFavouriteService.addFavourite(documentId, username);
    }

    @Override
    public void removeFavourite(Long documentId, String username) {
        documentFavouriteService.removeFavourite(documentId, username);
    }
}
