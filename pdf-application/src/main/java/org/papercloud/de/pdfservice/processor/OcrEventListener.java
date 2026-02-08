package org.papercloud.de.pdfservice.processor;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.events.EnrichmentEvent;
import org.papercloud.de.core.events.OcrEvent;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.service.DocumentStatusService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrEventListener {

    private final DocumentOcrProcessor ocrProcessor;
    private final ApplicationEventPublisher eventPublisher;
    private final DocumentRepository documentRepository;
    private final PageRepository pageRepository;
    private final DocumentStatusService documentStatusService;

    @EventListener
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOcrEvent(OcrEvent event) {
        Long docId = event.documentId();
        log.info("Received OCR event for document ID: {}", docId);

        DocumentPdfEntity document = documentRepository.findById(docId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found for ID: " + docId));

        documentStatusService.updateStatus(document.getId(), Document.Status.OCR_IN_PROGRESS);

        try {
            byte[] pdfBytes = document.getPdfContent();
            if (pdfBytes == null || pdfBytes.length == 0) {
                log.error("No PDF content found for document ID: {}", docId);
                documentStatusService.updateStatus(document.getId(), Document.Status.OCR_ERROR);
                return;
            }

            List<String> pageTexts = ocrProcessor.extractTextFromPdf(pdfBytes);

            log.info("OCR completed for document ID: {}, total pages: {}", docId, pageTexts.size());
            documentStatusService.updateStatus(document.getId(), Document.Status.OCR_COMPLETED);
            savePages(document, pageTexts);

            // Trigger enrichment
            eventPublisher.publishEvent(new EnrichmentEvent(docId));

        } catch (IOException e) {
            log.error("OCR processing failed for document ID: {}", docId, e);
            documentStatusService.updateStatus(document.getId(), Document.Status.OCR_ERROR);
        }
    }

    @Transactional
    protected void savePages(DocumentPdfEntity document, List<String> pageTexts) {
        List<PagesPdfEntity> pages = IntStream.range(0, pageTexts.size())
                .mapToObj(i -> PagesPdfEntity.builder()
                        .document(document)
                        .pageNumber(i + 1)
                        .pageText(pageTexts.get(i))
                        .build())
                .toList();

        pageRepository.saveAll(pages);
        log.info("Saved {} pages for document ID: {}", pages.size(), document.getId());
    }
}
