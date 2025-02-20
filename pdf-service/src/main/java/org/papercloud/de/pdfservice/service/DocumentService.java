package org.papercloud.de.pdfservice.service;


import java.io.IOException;
import org.papercloud.de.common.dto.DocumentDTO;
import org.papercloud.de.common.dto.DocumentUploadDTO;

public interface DocumentService {

  DocumentDTO processDocument(DocumentUploadDTO file) throws IOException;

  DocumentDTO getDocument(Long id);

  byte[] getDocumentContent(Long id);
}
