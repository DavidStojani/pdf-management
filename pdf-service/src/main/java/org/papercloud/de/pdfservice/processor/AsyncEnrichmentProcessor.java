package org.papercloud.de.pdfservice.processor;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AsyncEnrichmentProcessor {

    private final DocumentEnrichmentProcessor enrichmentProcessor;
    private final DocumentRepository documentRepository;

    @Async
    public void enrichAndPersistDocument(Long documentId, List<String> pages) {
        DocumentPdfEntity doc = documentRepository.findById(documentId).orElseThrow();
        EnrichmentResultDTO enrichment = enrichmentProcessor.enrichDocument(doc, pages);

        doc.setTitle(enrichment.getTitle());
        doc.setDateOnDocument(enrichment.getDate_sent());
        doc.setTags(enrichment.getTagNames());
        documentRepository.save(doc);
    }
}
