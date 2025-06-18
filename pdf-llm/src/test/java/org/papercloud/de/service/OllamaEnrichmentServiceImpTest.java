package org.papercloud.de.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.common.util.LlmResponseProcessingService;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class OllamaEnrichmentServiceImpTest {

    @InjectMocks
    private OllamaEnrichmentServiceImp enrichmentService;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec uriSpec;

    @Mock
    private WebClient.RequestBodySpec bodySpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec headersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private LlmResponseProcessingService llmResponseProcessingService;

    private final String plainText = "Some input text";

    private final String prompt = "Give me a json-format with title, date_sent and 5 tags for this text: \"" + plainText + "\"";

    @BeforeEach
    void setup() {
        Mockito.when(webClient.post()).thenReturn(uriSpec);
        Mockito.when(uriSpec.uri("/api/generate")).thenReturn(bodySpec);
        Mockito.when(bodySpec.bodyValue(Mockito.any())).thenReturn(headersSpec);
        Mockito.when(headersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void testSuccessfulEnrichment() {
        String response = "{ \"title\": \"Title1\", \"date_sent\": \"12.06.2023\", \"tags\": [\"a\", \"b\"] }";

        Mockito.when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(response));
        Mockito.when(llmResponseProcessingService.extractJsonResponse(response)).thenReturn(response);
        Mockito.when(llmResponseProcessingService.extractEmbeddedJson(response)).thenReturn(response);

        EnrichmentResultDTO expected = EnrichmentResultDTO.builder()
                .title("Title1")
                .date_sent("12.06.2023")
                .tags(List.of(new EnrichmentResultDTO.TagDTO("a"), new EnrichmentResultDTO.TagDTO("b")))
                .build();

        Mockito.when(llmResponseProcessingService.parseEnrichmentResult(response)).thenReturn(Optional.of(expected));

        StepVerifier.create(enrichmentService.enrichTextAsync(plainText))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    void testEmptyResponseTriggersFallback() {
        Mockito.when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(""));

        StepVerifier.create(enrichmentService.enrichTextAsync(plainText))
                .expectNextMatches(result -> result.getTitle().equals("Unknown Title"))
                .verifyComplete();
    }

    @Test
    void testNullResponseTriggersFallback() {
        Mockito.when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.justOrEmpty(null));

        StepVerifier.create(enrichmentService.enrichTextAsync(plainText))
                .expectNextMatches(result -> result.getTitle().equals("Unknown Title"))
                .verifyComplete();
    }

    @Test
    void testParsingReturnsEmptyOptionalTriggersFallback() {
        String response = "{ \"some\": \"json\" }";

        Mockito.when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(response));
        Mockito.when(llmResponseProcessingService.extractJsonResponse(response)).thenReturn(response);
        Mockito.when(llmResponseProcessingService.extractEmbeddedJson(response)).thenReturn(response);
        Mockito.when(llmResponseProcessingService.parseEnrichmentResult(response)).thenReturn(Optional.empty());

        StepVerifier.create(enrichmentService.enrichTextAsync(plainText))
                .expectNextMatches(result -> result.getTitle().equals("Unknown Title"))
                .verifyComplete();
    }

    @Test
    void testTimeoutBehaviorHandledGracefully() {
        Mockito.when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.delay(Duration.ofSeconds(11)).map(i -> "Delayed"));

        // Mock extractJson and parsing even if it shouldn't reach here
        Mockito.when(llmResponseProcessingService.extractJsonResponse("Delayed")).thenReturn("Delayed");
        Mockito.when(llmResponseProcessingService.extractEmbeddedJson("Delayed")).thenReturn("Delayed");
        Mockito.when(llmResponseProcessingService.parseEnrichmentResult("Delayed")).thenReturn(Optional.empty());

        StepVerifier.create(enrichmentService.enrichTextAsync(plainText))
                .expectNextMatches(result -> result.getTitle().equals("Unknown Title"))
                .verifyComplete();
    }
}
