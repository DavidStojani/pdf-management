package org.papercloud.de.core.ports.outbound;

import org.papercloud.de.core.dto.llm.EnrichmentResultDTO;
import reactor.core.publisher.Mono;

/**
 * Port interface for document enrichment via LLM.
 * Implementations may use Ollama, OpenAI, or any other LLM provider.
 */
public interface EnrichmentService {

    /**
     * Enriches the given text asynchronously using an LLM.
     * Extracts metadata like title, date, and tags.
     *
     * @param plainText the text to enrich
     * @return a Mono containing the enrichment result
     */
    Mono<EnrichmentResultDTO> enrichTextAsync(String plainText);
}
