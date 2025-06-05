package org.papercloud.de.pdfservice.processor;

import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;

import java.util.List;

public interface DocumentEnrichmentProcessor {
    EnrichmentResultDTO enrichDocument(DocumentPdfEntity document, List<String> pageTexts);
}
