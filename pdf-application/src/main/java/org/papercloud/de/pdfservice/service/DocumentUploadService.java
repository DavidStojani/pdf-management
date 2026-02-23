package org.papercloud.de.pdfservice.service;

import org.papercloud.de.core.domain.UploadSource;
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentUploadDTO;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface DocumentUploadService {

    DocumentDTO processUpload(MultipartFile file, Authentication authentication, UploadSource source);

    DocumentDTO processDocument(DocumentUploadDTO dto, String username) throws IOException;
}
