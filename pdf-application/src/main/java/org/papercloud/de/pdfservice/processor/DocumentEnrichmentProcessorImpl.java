package org.papercloud.de.pdfservice.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.core.ports.outbound.EnrichmentService;
import org.papercloud.de.core.ports.outbound.OcrTextCleaningService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfservice.errors.DocumentEnrichmentException;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.papercloud.de.pdfservice.service.DocumentStatusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentEnrichmentProcessorImpl implements DocumentEnrichmentProcessor {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final EnrichmentService enrichmentService;
    private final OcrTextCleaningService textCleaningService;
    private final DocumentRepository documentRepository;
    private final PageRepository pageRepository;
    private final DocumentStatusService documentStatusService;

    @Override
    public void enrichDocument(Long documentId) throws Exception {

        log.info("Starting document enrichment process for document ID: {}", documentId);
        long startTime = System.nanoTime();

        try {
            String cleanedText = prepareForEnrichment(documentId);

            log.info("Cleaned text for document : {}", cleanedText);
            EnrichmentResultDTO result = enrichmentService.enrichTextAsync(cleanedText)
                    .block(Duration.ofSeconds(60));
            if (result == null) {
                documentStatusService.updateStatus(documentId, Document.Status.ENRICHMENT_ERROR);
            }

            saveEnrichmentResult(documentId, result);
            log.info("Successfully completed enrichment for document ID: {}", documentId);
        } catch (Exception ex) {
            markEnrichmentFailed(documentId, ex);
            throw ex instanceof DocumentEnrichmentException
                    ? ex
                    : new DocumentEnrichmentException("Enrichment failed", ex);
        } finally {
            logEnrichmentDuration(startTime);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected String prepareForEnrichment(Long documentId) {
        DocumentPdfEntity document = getDocumentById(documentId);
        if (document.getStatus() != Document.Status.OCR_COMPLETED) {
            throw new DocumentEnrichmentException("Document status is not OCR_COMPLETED for ID: " + documentId);
        }

        document.setStatus(Document.Status.ENRICHMENT_IN_PROGRESS);
        documentRepository.saveAndFlush(document);

        List<String> pageTexts = pageRepository.findByDocumentIdOrderByPageNumber(documentId).stream()
                .map(PagesPdfEntity::getPageText)
                .toList();
        validatePageTexts(pageTexts);

        return cleanFirstPageText(pageTexts);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void saveEnrichmentResult(Long documentId, EnrichmentResultDTO enrichmentResult) {
        DocumentPdfEntity document = getDocumentById(documentId);
        document.setTitle(enrichmentResult.getTitle());
        document.setDateOnDocument(parseDocumentDate(enrichmentResult.getDate_sent()));
        document.setTags(enrichmentResult.getTagNames());
        document.setStatus(Document.Status.ENRICHMENT_COMPLETED);
        documentRepository.save(document);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markEnrichmentFailed(Long documentId, Exception ex) {
        log.error("Enrichment failed for document {}", documentId, ex);
        documentStatusService.updateStatus(documentId, Document.Status.ENRICHMENT_ERROR);
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


    private LocalDate parseDocumentDate(String dateString) {
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse document date: {}. Using current date as fallback.", dateString);
            return LocalDate.now();
        }
    }

    private void logEnrichmentDuration(long startTime) {
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        log.debug("Document enrichment completed in {} ms", durationMs);
    }
}
