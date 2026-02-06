package org.papercloud.de.pdfservice.processor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.core.events.DocumentEnrichedEvent;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrichmentResultHandler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public void handle(Long documentId, EnrichmentResultDTO enrichmentResult) {
        transactionTemplate.executeWithoutResult(status -> {
            DocumentPdfEntity document = getDocumentById(documentId);
            updateDocumentWithEnrichmentResult(document, enrichmentResult);
            eventPublisher.publishEvent(new DocumentEnrichedEvent(document.getId()));
        });
    }

    private DocumentPdfEntity getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));
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
}
