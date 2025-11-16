package org.papercloud.de.pdfservice.processor;

import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.common.events.EnrichmentEvent;
import reactor.core.publisher.Mono;

public interface DocumentEnrichmentProcessor {
    Mono<EnrichmentResultDTO> enrichDocument(EnrichmentEvent event);
}
