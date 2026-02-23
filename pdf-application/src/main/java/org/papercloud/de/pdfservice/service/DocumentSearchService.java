package org.papercloud.de.pdfservice.service;

import org.papercloud.de.core.dto.document.DocumentListItemDTO;

import java.util.List;

public interface DocumentSearchService {

    List<DocumentListItemDTO> searchDocuments(String username, String query);
}
