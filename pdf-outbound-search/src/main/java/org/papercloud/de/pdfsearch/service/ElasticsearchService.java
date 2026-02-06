package org.papercloud.de.pdfsearch.service;

import org.papercloud.de.core.dto.search.SearchRequestDTO;
import org.papercloud.de.core.dto.search.SearchResultDTO;
import org.papercloud.de.core.events.payload.IndexDocumentPayload;

public interface ElasticsearchService {
    void indexDocument(IndexDocumentPayload payload);
    SearchResultDTO search(SearchRequestDTO searchRequestDTO);
}
