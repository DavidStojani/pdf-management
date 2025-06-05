package org.papercloud.de.pdfservice.search;


import java.io.IOException;
import java.nio.file.AccessDeniedException;

import org.papercloud.de.common.dto.document.DocumentDTO;
import org.papercloud.de.common.dto.document.DocumentDownloadDTO;
import org.papercloud.de.common.dto.document.DocumentUploadDTO;

public interface DocumentService {

  DocumentDTO processDocument(DocumentUploadDTO file, String username) throws IOException;

  DocumentDownloadDTO downloadDocument(String username, Long id) throws AccessDeniedException;
}
