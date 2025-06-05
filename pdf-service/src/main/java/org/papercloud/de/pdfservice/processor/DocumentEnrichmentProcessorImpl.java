package org.papercloud.de.pdfservice.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.common.util.DocumentEnrichmentService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;

import org.papercloud.de.pdfservice.errors.DocumentEnrichmentException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.papercloud.de.pdfservice.textutils.TextCleaningService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentEnrichmentProcessorImpl implements DocumentEnrichmentProcessor {

    private final DocumentEnrichmentService documentEnrichmentService;
    private final TextCleaningService textCleaningService;

    @Override
    public EnrichmentResultDTO enrichDocument(DocumentPdfEntity document, List<String> pageTexts) {
        validateInput(pageTexts);

        String cleanedText = textCleaningService.cleanOcrText(pageTexts.get(0));
        log.info("Starting document enrichment process for document ID: {}", document.getId());

        return performEnrichment(cleanedText);
    }

    private void validateInput(List<String> pageTexts) {
        if (pageTexts == null || pageTexts.isEmpty()) {
            throw new InvalidDocumentException("Page texts cannot be null or empty");
        }
    }

    private EnrichmentResultDTO performEnrichment(String cleanedText) {
        try {
            CompletableFuture<EnrichmentResultDTO> enrichmentFuture =
                    documentEnrichmentService.enrichTextAsync(cleanedText).toFuture();
            return enrichmentFuture.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DocumentEnrichmentException("Document enrichment was interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new DocumentEnrichmentException("Failed to enrich document: " + cause.getMessage(), cause);
        }
    }
}