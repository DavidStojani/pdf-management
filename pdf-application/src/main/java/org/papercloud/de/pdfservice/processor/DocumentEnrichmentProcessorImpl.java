package org.papercloud.de.pdfservice.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.core.events.EnrichmentEvent;
import org.papercloud.de.core.events.IndexDocumentEvent;
import org.papercloud.de.core.events.payload.IndexDocumentPayload;
import org.papercloud.de.core.ports.outbound.EnrichmentService;
import org.papercloud.de.core.ports.outbound.OcrTextCleaningService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfservice.errors.DocumentEnrichmentException;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentEnrichmentProcessorImpl implements DocumentEnrichmentProcessor {

    private final EnrichmentService enrichmentService;
    private final OcrTextCleaningService textCleaningService;
    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public Mono<EnrichmentResultDTO> enrichDocument(EnrichmentEvent event) {
        log.info("Starting document enrichment process for document ID: {}", event.documentId());

        //ToDo: set status Enrich_ON_PROGRESS

        validatePageTexts(event.pageTexts());

        String cleanedText = cleanFirstPageText(event.pageTexts());
        long startTime = System.nanoTime();

        return enrichmentService.enrichTextAsync(cleanedText)
                .switchIfEmpty(Mono.just(getFallbackResult()))
                .onErrorMap(ex -> new DocumentEnrichmentException("Enrichment failed", ex))
                .map(result -> result == null ? getFallbackResult() : result)
                //.map(result -> persistAndPublish(event, result))
                .doOnSuccess(result -> log.info("Successfully completed enrichment for document ID: {}", event.documentId()))
                .doOnError(ex -> log.error("Enrichment failed for document {}", event.documentId(), ex))
                .doFinally(signalType -> logEnrichmentDuration(startTime))
                .subscribeOn(Schedulers.boundedElastic());
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
        // validatePageTexts already ensures index 0 exists
        return textCleaningService.cleanOcrText(pageTexts.get(0));
    }

    private EnrichmentResultDTO getFallbackResult() {
        return EnrichmentResultDTO.builder()
                .title("Unknown Title")
                .date_sent("01.01.2000")
                .tags(List.of())
                .flagFailedEnrichment(true)
                .build();
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
        document.setFailedEnrichment(enrichmentResult.isFlagFailedEnrichment());

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

    private EnrichmentResultDTO persistAndPublish(EnrichmentEvent event, EnrichmentResultDTO enrichmentResult) {
        EnrichmentResultDTO persisted = transactionTemplate.execute(status -> {
            DocumentPdfEntity document = getDocumentById(event.documentId());
            updateDocumentWithEnrichmentResult(document, enrichmentResult);
            publishIndexEvent(document, event.pageTexts());
            return enrichmentResult;
        });

        return persisted == null ? enrichmentResult : persisted;
    }

    private void publishIndexEvent(DocumentPdfEntity document, List<String> pageTexts) {
        IndexDocumentPayload payload = new IndexDocumentPayload(
                document.getId(),
                document.getTitle(),
                document.getContentType(),
                document.getTags(),
                document.getDateOnDocument() != null ? document.getDateOnDocument().getYear() : LocalDate.now().getYear(),
                String.join("\n", pageTexts)
        );

        eventPublisher.publishEvent(new IndexDocumentEvent(payload));
        log.debug("Published IndexDocumentEvent for document ID: {}", document.getId());
    }
}
