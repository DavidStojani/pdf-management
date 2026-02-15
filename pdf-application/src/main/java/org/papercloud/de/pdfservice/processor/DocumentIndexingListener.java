package org.papercloud.de.pdfservice.processor;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.dto.search.IndexableDocumentDTO;
import org.papercloud.de.core.events.DocumentEnrichedEvent;
import org.papercloud.de.core.events.DocumentIndexingEvent;
import org.papercloud.de.core.ports.outbound.SearchService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.service.DocumentStatusService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentIndexingListener {

    private final DocumentRepository documentRepository;
    private final PageRepository pageRepository;
    private final SearchService searchService;
    private final DocumentStatusService documentStatusService;

    @Async
    @EventListener
    @Transactional
    public void handleDocumentEnriched(DocumentEnrichedEvent event) {
        indexDocument(event.documentId());
    }

    @Async
    @EventListener
    @Transactional
    public void handleDocumentIndexingEvent(DocumentIndexingEvent event) {
        indexDocument(event.documentId());
    }

    private void indexDocument(Long documentId) {
        DocumentPdfEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));

        documentStatusService.updateStatus(documentId, Document.Status.INDEXING_IN_PROGRESS);

        try {
            List<PagesPdfEntity> pages = pageRepository.findByDocumentIdOrderByPageNumber(document.getId());
            String fullText = pages.stream()
                    .map(PagesPdfEntity::getPageText)
                    .filter(text -> text != null && !text.isBlank())
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");

            String fileName = document.getTitle() != null ? document.getTitle() : document.getFilename();

            IndexableDocumentDTO dto = IndexableDocumentDTO.builder()
                    .id(document.getId())
                    .fileName(fileName)
                    .contentType(document.getContentType())
                    .tags(document.getTags() == null ? List.of() : document.getTags())
                    .year(document.getDateOnDocument() != null ? document.getDateOnDocument().getYear() : LocalDate.now().getYear())
                    .fullText(fullText)
                    .username(document.getOwner().getUsername())
                    .build();

            searchService.indexDocument(dto);

            documentStatusService.updateStatus(documentId, Document.Status.INDEXING_COMPLETED);
            documentStatusService.resetIndexingRetry(documentId);
            log.debug("Indexed document ID {} via SearchService", document.getId());
        } catch (Exception e) {
            log.error("Failed to index document ID {}: {}", documentId, e.getMessage(), e);
            documentStatusService.markIndexingFailure(documentId, e.getMessage());
        }
    }
}
