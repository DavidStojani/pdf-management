package org.papercloud.de.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.common.util.DocumentEnrichmentService;
import org.papercloud.de.common.util.LlmResponseProcessingService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaEnrichmentServiceImp implements DocumentEnrichmentService {

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
                .switchIfEmpty(Mono.empty())
                // absolutely any unexpected error → fallback
                .onErrorResume(ex -> {
                    log.error("Error while enriching text via Ollama. Returning fallback result.", ex);
                    return Mono.empty();
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

    private String buildPrompt(String plainText) {
        return PROMPT_TEMPLATE.formatted(plainText);
    }

}
