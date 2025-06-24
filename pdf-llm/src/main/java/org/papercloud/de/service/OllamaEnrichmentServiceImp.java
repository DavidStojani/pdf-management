package org.papercloud.de.service;

import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.common.util.DocumentEnrichmentService;
import org.papercloud.de.common.util.LlmResponseProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;


@Service
public class OllamaEnrichmentServiceImp implements DocumentEnrichmentService {
    @Autowired
    private WebClient webClient;
    @Autowired
    private LlmResponseProcessingService llmResponseProcessingService;

    @Override
    public Mono<EnrichmentResultDTO> enrichTextAsync(String plainText) {
        String prompt = buildPrompt(plainText);
        System.out.printf("PROMPT__::__ %s ", prompt);

        return sendPromptToModelAsync(prompt)
                .filter(responseBody -> responseBody != null && !responseBody.isBlank())
                .map(llmResponseProcessingService::extractJsonResponse)
                .map(llmResponseProcessingService::extractEmbeddedJson)
                .flatMap(json -> llmResponseProcessingService.parseEnrichmentResult(json)
                        .map(Mono::just)
                        .orElseGet(() -> Mono.just(getFallbackResult())))
                .switchIfEmpty(Mono.just(getFallbackResult()));

    }

    private String buildPrompt(String plainText) {
        return "Give me a json-format with title, date_sent as dd.MM.yyyy and 5 tags for this text: \"" + plainText + "\"";
    }

    public Mono<String> sendPromptToModelAsync(String prompt) {
        return webClient.post()
                .uri("/api/generate")
                .bodyValue(Map.of("model", "mistral", "prompt", prompt))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMinutes(10))
                .doOnSubscribe(sub -> System.out.println("Calling OLLAMA (reactive)..."))
                .doOnSuccess(resp -> System.out.println("OLLAMA response received."));
    }

    private EnrichmentResultDTO getFallbackResult() {
        return EnrichmentResultDTO.builder()
                .title("Unknown Title")
                .date_sent("01.01.2000")
                .tags(List.of())
                .build();
    }


}
