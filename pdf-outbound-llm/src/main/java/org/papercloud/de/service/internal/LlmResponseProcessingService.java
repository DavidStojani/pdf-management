package org.papercloud.de.service.internal;

import org.papercloud.de.core.dto.llm.EnrichmentResultDTO;

import java.util.Optional;

/**
 * Internal service for processing LLM responses.
 * This is an implementation detail of the pdf-llm module.
 */
public interface LlmResponseProcessingService {
    String extractJsonResponse(String responseBody);
    String extractEmbeddedJson(String text);
    Optional<EnrichmentResultDTO> parseEnrichmentResult(String json);
}
