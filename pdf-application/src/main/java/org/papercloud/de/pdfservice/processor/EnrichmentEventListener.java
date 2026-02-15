package org.papercloud.de.pdfservice.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.events.DocumentEnrichedEvent;
import org.papercloud.de.core.events.EnrichmentEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrichmentEventListener {

    private final DocumentEnrichmentProcessor enrichmentProcessor;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentUploaded(EnrichmentEvent event) {
        Long docId = event.documentId();
        log.info("EnrichmentEvent received event for docId {}", docId);
        try {
            enrichmentProcessor.enrichDocument(docId);
            eventPublisher.publishEvent(new DocumentEnrichedEvent(docId));
            log.info("Published DocumentEnrichedEvent for docId {}", docId);
        } catch (Exception e) {
            log.error("Enrichment processing failed for docId {}", docId, e);
        }
    }
}
