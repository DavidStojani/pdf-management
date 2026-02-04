package org.papercloud.de.core.ports.outbound;

import org.papercloud.de.core.dto.search.SearchRequestDTO;
import org.papercloud.de.core.dto.search.SearchResultDTO;
import org.papercloud.de.core.dto.search.IndexableDocumentDTO;

/**
 * Port interface for document search and indexing.
 * Implementations may use Elasticsearch, OpenSearch, or any other search engine.
 */
public interface SearchService {

    /**
     * Indexes a document for full-text search.
     *
     * @param document the document to index
     */
    void indexDocument(IndexableDocumentDTO document);

    /**
     * Searches for documents matching the given request.
     *
     * @param request the search request with query and pagination
     * @return the search results
     */
    SearchResultDTO search(SearchRequestDTO request);

    /**
     * Deletes a document from the search index.
     *
     * @param documentId the ID of the document to delete
     */
    void deleteDocument(Long documentId);
}
