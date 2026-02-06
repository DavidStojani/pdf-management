package org.papercloud.de.pdfservice.service;

import org.papercloud.de.core.dto.search.SearchHitDTO;

public interface DocumentSearchService {

  SearchHitDTO searchDocumentByText(String keyword);

}
