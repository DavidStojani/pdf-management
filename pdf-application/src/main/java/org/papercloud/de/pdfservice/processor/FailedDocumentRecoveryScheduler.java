package org.papercloud.de.pdfservice.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.events.DocumentIndexingEvent;
import org.papercloud.de.core.events.EnrichmentEvent;
import org.papercloud.de.core.events.OcrEvent;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FailedDocumentRecoveryScheduler {

    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.recovery.retry.enabled:true}")
    private boolean retryEnabled;

    @Value("${app.recovery.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.recovery.retry.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.recovery.retry.fixed-delay-ms:900000}")
    public void retryFailedDocuments() {
        if (!retryEnabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        PageRequest pageRequest = PageRequest.of(0, batchSize);

        List<DocumentPdfEntity> ocrCandidates =
                documentRepository.findRetryableOcrDocuments(now, maxAttempts, pageRequest);
        List<DocumentPdfEntity> enrichmentCandidates =
                documentRepository.findRetryableEnrichmentDocuments(now, maxAttempts, pageRequest);
        List<DocumentPdfEntity> indexingCandidates =
                documentRepository.findRetryableIndexingDocuments(now, maxAttempts, pageRequest);

        if (ocrCandidates.isEmpty() && enrichmentCandidates.isEmpty() && indexingCandidates.isEmpty()) {
            log.debug("Retry scheduler found no eligible documents");
            return;
        }

        log.info(
                "Retry scheduler dispatching {} OCR retries, {} enrichment retries, and {} indexing retries",
                ocrCandidates.size(),
                enrichmentCandidates.size(),
                indexingCandidates.size()
        );

        ocrCandidates.forEach(doc -> eventPublisher.publishEvent(new OcrEvent(doc.getId())));
        enrichmentCandidates.forEach(doc -> eventPublisher.publishEvent(new EnrichmentEvent(doc.getId())));
        indexingCandidates.forEach(doc -> eventPublisher.publishEvent(new DocumentIndexingEvent(doc.getId())));
    }
}
