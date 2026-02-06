package org.papercloud.de.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.core.ports.outbound.EnrichmentService;
import org.papercloud.de.service.internal.LlmResponseProcessingService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Ollama implementation of the EnrichmentService port.
 * This adapter communicates with an Ollama LLM instance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaEnrichmentServiceImp implements EnrichmentService {

    private static final String MODEL_NAME = "mistral";
    private static final String PROMPT_TEMPLATE =
            "Give me a json-format with title, date_sent as dd.MM.yyyy and 5 tags for this text: \"%s\"";
    private static final Duration OLLAMA_TIMEOUT = Duration.ofMinutes(10);

    private final WebClient webClient;
    private final LlmResponseProcessingService llmResponseProcessingService;

    @Override
    public Mono<EnrichmentResultDTO> enrichTextAsync(String plainText) {
        String prompt = buildPrompt(plainText);
        log.debug("OLLAMA prompt: {}", prompt);

        return sendPromptToModelAsync(prompt)
                // ignore null / blank body
                .filter(responseBody -> responseBody != null && !responseBody.isBlank())
                .flatMap(this::mapResponseToEnrichmentResult)
                // no Body (404 etc.) → fallback
                .switchIfEmpty(Mono.just(getFallbackResult()))
                // absolutely any unexpected error → fallback
                .onErrorResume(ex -> {
                    log.error("Error while enriching text via Ollama. Returning fallback result.", ex);
                    return Mono.just(getFallbackResult());
                });
    }

    private Mono<EnrichmentResultDTO> mapResponseToEnrichmentResult(String responseBody) {
        return Mono.fromCallable(() -> {
                    String json = llmResponseProcessingService.extractJsonResponse(responseBody);
                    return llmResponseProcessingService.extractEmbeddedJson(json);
                })
                .flatMap(json ->
                        Mono.justOrEmpty(llmResponseProcessingService.parseEnrichmentResult(json))
                );
    }

    private Mono<String> sendPromptToModelAsync(String prompt) {
        return webClient.post()
                .uri("/api/generate")
                .bodyValue(Map.of("model", MODEL_NAME, "prompt", prompt))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(OLLAMA_TIMEOUT)
                .doOnSubscribe(sub -> log.info("Calling OLLAMA (reactive)..."))
                .doOnSuccess(resp -> log.info("OLLAMA response received."))
                // ANY client error (404, timeout, etc.) → log & emit empty()
                .onErrorResume(e -> {
                    log.error("Failed to call Ollama model", e);
                    return Mono.empty();
                });
    }

    private EnrichmentResultDTO getFallbackResult() {
        return EnrichmentResultDTO.builder()
                .title("Unknown Title")
                .date_sent("01.01.2000")
                .tags(List.of())
                .flagFailedEnrichment(true)
                .build();
    }

    private String buildPrompt(String plainText) {
        return PROMPT_TEMPLATE.formatted(plainText);
    }
}
