package org.papercloud.de.pdfsearch.event;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.core.events.IndexDocumentEvent;
import org.papercloud.de.core.events.payload.IndexDocumentPayload;
import org.papercloud.de.core.ports.outbound.SearchService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class IndexDocumentListener {

    private final SearchService searchService;

    @Async
    @EventListener
    @Transactional
    public void handleIndexEvent(IndexDocumentEvent event) {
        IndexDocumentPayload payload = event.payload();
        searchService.indexDocument(payload.toDto());
    }
}
