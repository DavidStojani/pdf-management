package org.papercloud.de.common.util;

import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;
import reactor.core.publisher.Mono;

public interface DocumentEnrichmentService {
    Mono<EnrichmentResultDTO> enrichTextAsync(String plainText);
}
