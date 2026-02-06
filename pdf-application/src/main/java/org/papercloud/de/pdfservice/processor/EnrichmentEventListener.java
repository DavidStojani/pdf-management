package org.papercloud.de.pdfservice.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.events.EnrichmentEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrichmentEventListener {

    private final DocumentEnrichmentProcessor enrichmentenrichmentProcessor;
    private final EnrichmentResultHandler enrichmentResultHandler;

    @Async
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentUploaded(EnrichmentEvent event) {
        //TODO: Set status Enrich_ON_PROGRESS
        log.info("EnrichmentEvent received event for docId {}", event.documentId());
        enrichmentenrichmentProcessor.enrichDocument(event)
                .flatMap(result -> Mono.fromRunnable(
                                () -> enrichmentResultHandler.handle(event.documentId(), result))
                        .thenReturn(result))
                .doOnSuccess(result -> log.info("Result found for DOC: {}", result)) //TODO: set status ENRICH_COMPLETED
                .doOnError(error -> log.error("Error while enriching document {}", event.documentId(), error)) //TODO: set status ENRICH_ERROR
                .subscribe();
    }
}
