package org.papercloud.de.pdfservice.search;

import org.papercloud.de.core.dto.search.SearchHitDTO;

public interface DocumentSearchService {

  SearchHitDTO searchDocumentByText(String keyword);

}
