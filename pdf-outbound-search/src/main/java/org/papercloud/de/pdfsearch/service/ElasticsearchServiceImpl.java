package org.papercloud.de.pdfsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.dto.search.IndexableDocumentDTO;
import org.papercloud.de.core.dto.search.SearchHitDTO;
import org.papercloud.de.core.dto.search.SearchRequestDTO;
import org.papercloud.de.core.dto.search.SearchResultDTO;
import org.papercloud.de.core.ports.outbound.SearchService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Elasticsearch implementation of the SearchService port.
 * This adapter handles document indexing and full-text search using Elasticsearch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchServiceImpl implements SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME = "documents";

    @Override
    public void indexDocument(IndexableDocumentDTO dto) {
        try {
            elasticsearchClient.index(i -> i
                    .index(INDEX_NAME)
                    .id(String.valueOf(dto.getId()))
                    .document(dto)
            );
            log.info("Indexed document ID {} into Elasticsearch", dto.getId());
        } catch (IOException e) {
            log.error("Failed to index document {}", dto.getId(), e);
        }
    }

    @Override
    public SearchResultDTO search(SearchRequestDTO req) {
        String query = req.getQuery();
        String username = req.getUsername();
        int page = req.getPage() != null ? req.getPage() : 0;
        int size = req.getSize() != null ? req.getSize() : 10;

        try {
            // Build the bool query
            BoolQuery.Builder boolQ = QueryBuilders.bool();

            // 1. Full-text match
            if (query != null && !query.isBlank()) {
                boolQ.must(m -> m.match(t -> t
                        .field("fullText")
                        .query(query)
                ));
            }

            // 2. Restrict to this user
            if (username != null) {
                boolQ.filter(f -> f.term(t -> t
                        .field("username.keyword")
                        .value(username)
                ));
            }

            // Execute the search
            SearchResponse<IndexableDocumentDTO> resp = elasticsearchClient.search(s -> s
                            .index(INDEX_NAME)
                            .from(page * size)
                            .size(size)
                            .query(q -> q.bool(boolQ.build())),
                    IndexableDocumentDTO.class
            );

            // Map to SearchHitDTO
            List<SearchHitDTO> hits = resp.hits().hits().stream()
                    .map(hit -> {
                        IndexableDocumentDTO d = hit.source();
                        return SearchHitDTO.builder()
                                .documentId(d.getId().toString())
                                .documentName(d.getFileName())
                                .pageNumber(0)
                                .textSnippet(
                                        d.getFullText() == null ? ""
                                                : d.getFullText().substring(0, Math.min(200, d.getFullText().length()))
                                )
                                .build();
                    })
                    .toList();

            long total = resp.hits().total().value();
            int totalPages = (int) Math.ceil((double) total / size);

            return SearchResultDTO.builder()
                    .hits(hits)
                    .totalHits(total)
                    .totalPages(totalPages)
                    .currentPage(page)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to search documents", e);
        }
    }

    @Override
    public void deleteDocument(Long documentId) {
        try {
            elasticsearchClient.delete(d -> d
                    .index(INDEX_NAME)
                    .id(String.valueOf(documentId))
            );
            log.info("Deleted document ID {} from Elasticsearch", documentId);
        } catch (IOException e) {
            log.error("Failed to delete document {}", documentId, e);
        }
    }
}
