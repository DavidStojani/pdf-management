package org.papercloud.de.pdfservice.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.common.events.EnrichmentEvent;
import org.papercloud.de.common.events.IndexDocumentEvent;
import org.papercloud.de.common.util.DocumentEnrichmentService;
import org.papercloud.de.common.util.OcrTextCleaningService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfservice.errors.DocumentEnrichmentException;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentEnrichmentProcessorImpl implements DocumentEnrichmentProcessor {

    private final DocumentEnrichmentService documentEnrichmentService;
    private final OcrTextCleaningService ocrTextCleaningService;
    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    @Retryable
    public EnrichmentResultDTO enrichDocument(EnrichmentEvent event) {
        log.info("Starting document enrichment process for document ID: {}", event.getDocumentId());

        DocumentPdfEntity document = getDocumentById(event.getDocumentId());
        validatePageTexts(event.getPageTexts());

        String cleanedText = cleanFirstPageText(event.getPageTexts());
        EnrichmentResultDTO enrichmentResult = performEnrichment(cleanedText);

        updateDocumentWithEnrichmentResult(document, enrichmentResult);
        publishIndexEvent(document.getId());

        log.info("Successfully completed enrichment for document ID: {}", document.getId());
        return enrichmentResult;
    }

    @Recover
    public void recover(Exception exception, EnrichmentEvent event) {
        log.error("Document enrichment failed for document ID: {}. Marking as failed.",
                event.getDocumentId(), exception);

        markDocumentAsFailedEnrichment(event.getDocumentId());
    }

    private DocumentPdfEntity getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));
    }

    private void validatePageTexts(List<String> pageTexts) {
        if (pageTexts == null || pageTexts.isEmpty()) {
            throw new InvalidDocumentException("Page texts cannot be null or empty");
        }
    }

    private String cleanFirstPageText(List<String> pageTexts) {
        return ocrTextCleaningService.cleanOcrText(pageTexts.get(0));
    }

    private EnrichmentResultDTO performEnrichment(String cleanedText) {
        try {
            long startTime = System.nanoTime();

            CompletableFuture<EnrichmentResultDTO> enrichmentFuture =
                    documentEnrichmentService.enrichTextAsync(cleanedText).toFuture();

            EnrichmentResultDTO result = enrichmentFuture.get();

            logEnrichmentDuration(startTime);
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DocumentEnrichmentException("Document enrichment was interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new DocumentEnrichmentException("Failed to enrich document: " + cause.getMessage(), cause);
        }
    }

    private void logEnrichmentDuration(long startTime) {
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        log.debug("Document enrichment completed in {} ms", durationMs);
    }

    private void updateDocumentWithEnrichmentResult(DocumentPdfEntity document, EnrichmentResultDTO enrichmentResult) {
        document.setTitle(enrichmentResult.getTitle());
        document.setDateOnDocument(parseDocumentDate(enrichmentResult.getDate_sent()));
        document.setTags(enrichmentResult.getTagNames());

        documentRepository.save(document);
        log.debug("Document updated and saved with ID: {}", document.getId());
    }

    private LocalDate parseDocumentDate(String dateString) {
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse document date: {}. Using current date as fallback.", dateString);
            return LocalDate.now();
        }
    }

    private void publishIndexEvent(Long documentId) {
        eventPublisher.publishEvent(new IndexDocumentEvent(documentId));
        log.debug("Published IndexDocumentEvent for document ID: {}", documentId);
    }

    private void markDocumentAsFailedEnrichment(Long documentId) {
        try {
            DocumentPdfEntity document = getDocumentById(documentId);
            document.setFailedEnrichment(true);
            documentRepository.save(document);
            log.info("Marked document ID: {} as failed enrichment", documentId);
        } catch (Exception e) {
            log.error("Failed to mark document as failed enrichment for ID: {}", documentId, e);
        }
    }
}