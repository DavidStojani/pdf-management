package org.papercloud.de.pdfsearch.service;

import org.papercloud.de.common.dto.search.IndexableDocumentDTO;
import org.papercloud.de.common.dto.search.SearchRequestDTO;
import org.papercloud.de.common.dto.search.SearchResultDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;

import java.util.List;

public interface ElasticsearchService {
    void indexDocument(DocumentPdfEntity document, List<PagesPdfEntity> pages);
    SearchResultDTO search(SearchRequestDTO searchRequestDTO);
}
