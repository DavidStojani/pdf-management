package org.papercloud.de.pdfservice.processor;

import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.common.events.EnrichmentEvent;

public interface DocumentEnrichmentProcessor {
    EnrichmentResultDTO enrichDocument(EnrichmentEvent event);
}
