package org.papercloud.de.pdfservice.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.common.events.EnrichmentEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrichmentEventListener {

    private final DocumentEnrichmentProcessor enrichmentenrichmentProcessor;

    @Async
    @EventListener
    public void handleDocumentUploaded(EnrichmentEvent event) {
        log.info("EnrichmentEventListener received event for docId {}", event.getDocumentId());
        var result = enrichmentenrichmentProcessor.enrichDocument(event);

        log.info("Result found for DOC: {}", result);
    }
}