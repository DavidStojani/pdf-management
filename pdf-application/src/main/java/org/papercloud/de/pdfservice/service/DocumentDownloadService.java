package org.papercloud.de.pdfservice.service;

import org.papercloud.de.core.dto.document.DocumentDownloadDTO;

import java.nio.file.AccessDeniedException;

public interface DocumentDownloadService {

    DocumentDownloadDTO downloadDocument(String username, Long id) throws AccessDeniedException;
}
