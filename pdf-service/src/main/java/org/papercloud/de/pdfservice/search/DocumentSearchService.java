package org.papercloud.de.pdfservice.search;

import org.papercloud.de.common.dto.search.SearchHitDTO;

public interface DocumentSearchService {

  SearchHitDTO searchDocumentByText(String keyword);

}
