package org.papercloud.de.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.common.util.DocumentEnrichmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class LlmEnrichmentServiceImp implements DocumentEnrichmentService {
    @Autowired
    private WebClient webClient;

    @Override
    public Mono<EnrichmentResultDTO> enrichTextAsync(String plainText) {
      /*  String prompt = buildPrompt(plainText);
        System.out.printf("PROMT:: %s ", prompt);
        String responseBody = sendPromptToModel(prompt);
        if (responseBody == null || responseBody.isBlank()) {
            // Return fallback DTO or throw custom exception if needed
            return getFallbackResult();
        }

        String jsonContent = extractJsonResponse(responseBody);
        System.out.println("\n\n Response from OLLAMA----- \n" + jsonContent);
        String onlyJson = extractEmbeddedJson(jsonContent);

        System.out.println("\n\n Response from EXTRACT-JSON----- \n" + onlyJson);
        return parseEnrichmentResult(onlyJson).orElseGet(this::getFallbackResult);

       */
        return enrichMe(plainText);
    }

    public Mono<EnrichmentResultDTO> enrichMe(String plainText) {
        String prompt = buildPrompt(plainText);
        System.out.printf("PROMPT:: %s ", prompt);

        return sendPromptToModelAsync(prompt)
                .filter(responseBody -> responseBody != null && !responseBody.isBlank())
                .map(this::extractJsonResponse)
                .map(this::extractEmbeddedJson)
                .flatMap(json -> parseEnrichmentResult(json)
                        .map(Mono::just)
                        .orElseGet(() -> Mono.just(getFallbackResult())))
                .switchIfEmpty(Mono.just(getFallbackResult()));
    }


    private String buildPrompt(String plainText) {
        return "Give me a json-format with title, date_sent and 5 tags for this text: \"" + plainText + "\"";
    }

    private String sendPromptToModel(String prompt) {
        try {
            System.out.println("Calling the WebClient for OLLAMA...");

            long start = System.currentTimeMillis();

            ResponseEntity<String> response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(Map.of("model", "llama3", "prompt", prompt))
                    .retrieve()
                    .toEntity(String.class)
                    .block(Duration.ofMinutes(10)); // This limits the blocking time

            long duration = System.currentTimeMillis() - start;
            System.out.printf("OLLAMA call finished in %d ms%n", duration);

            return response != null ? response.getBody() : null;

        } catch (Exception e) {
            System.err.println("Error while calling OLLAMA: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Mono<String> sendPromptToModelAsync(String prompt) {
        return webClient.post()
                .uri("/api/generate")
                .bodyValue(Map.of("model", "llama3", "prompt", prompt))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMinutes(10))
                .doOnSubscribe(sub -> System.out.println("Calling OLLAMA (reactive)..."))
                .doOnSuccess(resp -> System.out.println("OLLAMA response received."));
    }



    private String extractEmbeddedJson(String text) {
        // First try to extract content between triple backticks
        Pattern tripleBacktickPattern = Pattern.compile("```\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
        Matcher matcher = tripleBacktickPattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Fallback: try to extract the largest outermost JSON block
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1).trim();
        }

        return ""; // Nothing found
    }


    private String extractJsonResponse(String responseBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        try (BufferedReader reader = new BufferedReader(new StringReader(responseBody))) {
            return reader.lines()
                    .map(line -> {
                        try {
                            JsonNode node = objectMapper.readTree(line);
                            return node.get("response").asText();
                        } catch (Exception e) {
                            // Log individual parsing issues
                            return "";
                        }
                    })
                    .collect(Collectors.joining());
        } catch (IOException e) {
            // Log or handle error
            return "";
        }
    }

    private Optional<EnrichmentResultDTO> parseEnrichmentResult(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return Optional.of(objectMapper.readValue(json, EnrichmentResultDTO.class));
        } catch (Exception e) {
            // Log or handle JSON parsing error
            return Optional.empty();
        }
    }

    private EnrichmentResultDTO getFallbackResult() {
        return EnrichmentResultDTO.builder()
                .title("Unknown Title")
                .date_sent("01.01.2000")
                .tags(List.of())
                .build();
    }


}
