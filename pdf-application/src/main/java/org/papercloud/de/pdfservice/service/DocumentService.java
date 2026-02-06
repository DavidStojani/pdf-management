package org.papercloud.de.pdfservice.service;


import java.io.IOException;
import java.nio.file.AccessDeniedException;

import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.core.dto.document.DocumentUploadDTO;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

  DocumentDTO processUpload(MultipartFile file, Authentication authentication);

  DocumentDTO processDocument(DocumentUploadDTO file, String username) throws IOException;

  DocumentDownloadDTO downloadDocument(String username, Long id) throws AccessDeniedException;
}
