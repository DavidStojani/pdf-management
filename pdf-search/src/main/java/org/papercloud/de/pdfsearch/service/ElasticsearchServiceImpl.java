package org.papercloud.de.pdfsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.common.dto.search.IndexableDocumentDTO;
import org.papercloud.de.common.dto.search.SearchHitDTO;
import org.papercloud.de.common.dto.search.SearchRequestDTO;
import org.papercloud.de.common.dto.search.SearchResultDTO;
import org.papercloud.de.common.events.payload.IndexDocumentPayload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchServiceImpl implements ElasticsearchService {

    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME = "documents";

    @Override
    public void indexDocument(IndexDocumentPayload payload) {
        IndexableDocumentDTO dto = payload.toDto();

        try {
            elasticsearchClient.index(i -> i
                    .index(INDEX_NAME)
                    .id(String.valueOf(payload.id()))
                    .document(dto)
            );
            log.info("Indexed document ID {} into Elasticsearch", payload.id());
        } catch (IOException e) {
            log.error("Failed to index document {}", payload.id(), e);
        }
    }
    @Override
    public SearchResultDTO search(SearchRequestDTO req) {
        String query = req.getQuery();
        String username = req.getUsername();
        Integer year = req.getYear();
        List<String> tags = req.getTags();
        int page = req.getPage();
        int size = req.getSize();

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
            boolQ.filter(f -> f.term(t -> t
                    .field("username.keyword")
                    .value(username)
            ));

            /*/ 3. Year filter
            if (year != null) {
                boolQ.filter(f -> f.term(t -> t
                        .field("year")
                        .value(year)
                ));
            }

            // 4. Tags filter
            if (tags != null && !tags.isEmpty()) {
                boolQ.filter(f -> f.terms(t -> t
                        .field("tags.keyword")
                        .terms(terms -> terms
                                .value(tags.stream().map(FieldValue::of).toList())
                        )
                ));
            }

             */

            // Execute the search
            SearchResponse<IndexableDocumentDTO> resp = elasticsearchClient.search(s -> s
                            .index("documents")         // your index name

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

}
