package org.papercloud.de.pdfsearch.event;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.common.events.IndexDocumentEvent;
import org.papercloud.de.common.events.payload.IndexDocumentPayload;
import org.papercloud.de.pdfsearch.service.ElasticsearchService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class IndexDocumentListener {

    private final ElasticsearchService elasticsearchService;

    @Async
    @EventListener
    @Transactional
    public void handleIndexEvent(IndexDocumentEvent event) {
        IndexDocumentPayload payload = event.payload();
        elasticsearchService.indexDocument(payload);
    }
}
