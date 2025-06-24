package org.papercloud.de.pdfsearch.event;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.common.events.IndexDocumentEvent;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfsearch.service.ElasticsearchService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IndexDocumentListener {

    private final DocumentRepository documentRepository;
    private final PageRepository pageRepository;
    private final ElasticsearchService elasticsearchService;

    @Async
    @EventListener
    public void handleIndexEvent(IndexDocumentEvent event) {
        Long docId = event.getDocumentId();
        DocumentPdfEntity doc = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalStateException("Document not found for indexing: " + docId));

        List<PagesPdfEntity> pages = pageRepository.findByDocumentId(docId);

        elasticsearchService.indexDocument(doc, pages);
    }
}
