package org.papercloud.de.pdfservice.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.core.events.EnrichmentEvent;
import org.papercloud.de.core.ports.outbound.EnrichmentService;
import org.papercloud.de.core.ports.outbound.OcrTextCleaningService;
import org.papercloud.de.pdfservice.errors.DocumentEnrichmentException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentEnrichmentProcessorImpl implements DocumentEnrichmentProcessor {

    private final EnrichmentService enrichmentService;
    private final OcrTextCleaningService textCleaningService;

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
                .doOnSuccess(result -> log.info("Successfully completed enrichment for document ID: {}", event.documentId()))
                .doOnError(ex -> log.error("Enrichment failed for document {}", event.documentId(), ex))
                .doFinally(signalType -> logEnrichmentDuration(startTime))
                .subscribeOn(Schedulers.boundedElastic());
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
}
