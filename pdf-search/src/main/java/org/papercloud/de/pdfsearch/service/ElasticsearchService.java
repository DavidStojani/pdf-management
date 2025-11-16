package org.papercloud.de.pdfsearch.service;

import org.papercloud.de.common.dto.search.SearchRequestDTO;
import org.papercloud.de.common.dto.search.SearchResultDTO;
import org.papercloud.de.common.events.payload.IndexDocumentPayload;

public interface ElasticsearchService {
    void indexDocument(IndexDocumentPayload payload);
    SearchResultDTO search(SearchRequestDTO searchRequestDTO);
}
