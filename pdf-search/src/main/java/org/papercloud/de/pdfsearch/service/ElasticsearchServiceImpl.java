package org.papercloud.de.pdfsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.common.dto.search.IndexableDocumentDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    public void indexDocument(DocumentPdfEntity document, List<PagesPdfEntity> pages) {
        String combinedText = pages.stream()
                .map(PagesPdfEntity::getPageText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));

        IndexableDocumentDTO dto = IndexableDocumentDTO.builder()
                .id(document.getId())
                .fileName(document.getTitle())
                .contentType(document.getContentType())
                .tags(document.getTags()) // or transform tagEntities to List<String>
                .year(document.getDateOnDocument().getYear())
                .fullText(combinedText)
                .build();

        try {
            elasticsearchClient.index(i -> i
                    .index(INDEX_NAME)
                    .id(String.valueOf(document.getId()))
                    .document(dto)
            );
            log.info("Indexed document ID {} into Elasticsearch", document.getId());
        } catch (IOException e) {
            log.error("Failed to index document {}", document.getId(), e);
        }
    }
}
