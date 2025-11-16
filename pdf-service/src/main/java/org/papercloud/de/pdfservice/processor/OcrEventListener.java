package org.papercloud.de.pdfservice.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.common.events.EnrichmentEvent;
import org.papercloud.de.common.events.OcrEvent;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @EventListener
    @Async
    @Transactional
    public void handleOcrEvent(OcrEvent event) {
        Long docId = event.documentId();
        log.info("Received OCR event for document ID: {}", docId);

        DocumentPdfEntity document = documentRepository.findById(docId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found for ID: " + docId));

        try {
            List<String> pageTexts = ocrProcessor.extractTextFromPdf(event.pdfBytes());

            log.info("OCR completed for document ID: {}, total pages: {}", docId, pageTexts.size());

            savePages(document, pageTexts);

            // Optionally update document state here (e.g., set OCR_PROCESSED flag)

            // Trigger enrichment
            eventPublisher.publishEvent(new EnrichmentEvent(docId, pageTexts));

        } catch (IOException e) {
            log.error("OCR processing failed for document ID: {}", docId, e);
            // Optional: Set document status to FAILED, or publish failure event
        }
    }

    private void savePages(DocumentPdfEntity document, List<String> pageTexts) {
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
