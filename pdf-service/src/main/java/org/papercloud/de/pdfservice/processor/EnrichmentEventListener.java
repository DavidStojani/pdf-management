package org.papercloud.de.pdfservice.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.common.events.EnrichmentEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrichmentEventListener {

    private final DocumentEnrichmentProcessor enrichmentenrichmentProcessor;

    @Async
    @EventListener
    @Transactional
    public void handleDocumentUploaded(EnrichmentEvent event) {
        log.info("EnrichmentEventListener received event for docId {}", event.documentId());
        enrichmentenrichmentProcessor.enrichDocument(event)
                .doOnSuccess(result -> log.info("Result found for DOC: {}", result))
                .doOnError(error -> log.error("Error while enriching document {}", event.documentId(), error))
                .subscribe();
    }
}