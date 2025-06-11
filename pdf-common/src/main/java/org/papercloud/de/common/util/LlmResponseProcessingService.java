package org.papercloud.de.common.util;

import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;

import java.util.Optional;

public interface LlmResponseProcessingService {
    String extractJsonResponse(String responseBody);
    String extractEmbeddedJson(String text);
    Optional<EnrichmentResultDTO> parseEnrichmentResult(String json);
}
