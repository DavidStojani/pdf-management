package org.papercloud.de.pdfservice.processor;

import org.papercloud.de.common.events.DocumentUploadedEvent;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EnrichmentEventListener {

    private final DocumentRepository documentRepository;
    private final DocumentEnrichmentProcessor enrichmentenrichmentProcessor;

    public EnrichmentEventListener(DocumentRepository documentRepository,
                                   DocumentEnrichmentProcessor enrichmentService) {
        this.documentRepository = documentRepository;
        this.enrichmentenrichmentProcessor = enrichmentService;
    }

    @Async
    @Retryable(
            value = Exception.class,
            maxAttempts = 2,
            backoff = @Backoff(delay = 5000)
    )
    @EventListener
    public void handleDocumentUploaded(DocumentUploadedEvent event) {
        Long id = event.getDocumentId();
        List<String> pages = event.getPageTexts();

        var doc = documentRepository.findById(id).orElseThrow();
        var result = enrichmentenrichmentProcessor.enrichDocument(doc, pages);

        doc.setTitle(result.getTitle());
        doc.setDateOnDocument(result.getDate_sent());
        doc.setTags(result.getTagNames());
        documentRepository.save(doc);
    }

    @Recover
    public void recover(Exception e, DocumentUploadedEvent event) {
        var doc = documentRepository.findById(event.getDocumentId()).orElseThrow();
        doc.setFailedEnrichment(true);
        documentRepository.save(doc);
        // log failure
    }
}