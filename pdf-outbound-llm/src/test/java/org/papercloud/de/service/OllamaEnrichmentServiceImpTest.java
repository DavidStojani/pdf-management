package org.papercloud.de.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.service.internal.LlmResponseProcessingService;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OllamaEnrichmentServiceImp.
 * Tests LLM enrichment workflow including successful enrichment, error handling, and fallback scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaEnrichmentServiceImp")
class OllamaEnrichmentServiceImpTest {

    @Mock
    private WebClient webClient;

    @Mock
    private LlmResponseProcessingService llmResponseProcessingService;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Captor
    private ArgumentCaptor<Map<String, Object>> bodyCaptor;

    private OllamaEnrichmentServiceImp service;

    @BeforeEach
    void setUp() {
        service = new OllamaEnrichmentServiceImp(webClient, llmResponseProcessingService);
    }

    @Nested
    @DisplayName("enrichTextAsync - Happy Path")
    class SuccessfulEnrichmentTests {

        @Test
        @DisplayName("should successfully enrich text and return EnrichmentResultDTO")
        void should_returnEnrichmentResult_when_successfulEnrichment() {
            // Arrange
            String plainText = "This is a test document about finances.";
            String ollamaResponseBody = "{\"response\":\"Test response\"}";
            String extractedJson = "{\"title\":\"Finance Report\",\"date_sent\":\"08.02.2026\",\"tags\":[{\"name\":\"finance\"}]}";
            EnrichmentResultDTO expectedResult = EnrichmentResultDTO.builder()
                    .title("Finance Report")
                    .date_sent("08.02.2026")
                    .tags(List.of(new EnrichmentResultDTO.TagDTO("finance")))
                    .build();

            when(webClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(ollamaResponseBody));

            when(llmResponseProcessingService.extractJsonResponse(ollamaResponseBody))
                    .thenReturn("Test response");
            when(llmResponseProcessingService.extractEmbeddedJson("Test response"))
                    .thenReturn(extractedJson);
            when(llmResponseProcessingService.parseEnrichmentResult(extractedJson))
                    .thenReturn(Optional.of(expectedResult));

            // Act
            Mono<EnrichmentResultDTO> result = service.enrichTextAsync(plainText);

            // Assert
            StepVerifier.create(result)
                    .expectNextMatches(dto ->
                            dto.getTitle().equals("Finance Report") &&
                            dto.getDate_sent().equals("08.02.2026") &&
                            dto.getTags().size() == 1)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should build correct prompt with plain text")
        void should_buildCorrectPrompt_when_enriching() {
            // Arrange
            String plainText = "Document content";
            String expectedPrompt = "Give me a json-format with title, date_sent with Format DD.MM.YYYY and 5 tags for this text: \"Document content\"";

            when(webClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(bodyCaptor.capture())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{}"));

            when(llmResponseProcessingService.extractJsonResponse(any())).thenReturn("");
            when(llmResponseProcessingService.extractEmbeddedJson(any())).thenReturn("");
            when(llmResponseProcessingService.parseEnrichmentResult(any())).thenReturn(Optional.empty());

            // Act
            service.enrichTextAsync(plainText).block();

            // Assert
            Map<String, Object> capturedBody = bodyCaptor.getValue();
            assertThat(capturedBody).containsEntry("model", "qwen2.5:0.5b");
            assertThat(capturedBody).containsEntry("prompt", expectedPrompt);
        }

        @Test
        @DisplayName("should call Ollama API with correct URI")
        void should_callCorrectUri_when_enriching() {
            // Arrange
            String plainText = "Test";

            when(webClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri("/api/generate")).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{}"));

            when(llmResponseProcessingService.extractJsonResponse(any())).thenReturn("");
            when(llmResponseProcessingService.extractEmbeddedJson(any())).thenReturn("");
            when(llmResponseProcessingService.parseEnrichmentResult(any())).thenReturn(Optional.empty());

            // Act
            service.enrichTextAsync(plainText).block();

            // Assert
            verify(requestBodyUriSpec).uri("/api/generate");
        }
    }

    @Nested
    @DisplayName("enrichTextAsync - Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should return fallback result when WebClient throws exception")
        void should_returnFallback_when_webClientError() {
            // Arrange
            String plainText = "Test";

            when(webClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class))
                    .thenReturn(Mono.error(new WebClientResponseException(500, "Internal Server Error", null, null, null)));

            // Act
            Mono<EnrichmentResultDTO> result = service.enrichTextAsync(plainText);

            // Assert
            StepVerifier.create(result)
                    .expectNextMatches(dto ->
                            dto.getTitle().equals("Unknown Title") &&
                            dto.getDate_sent().equals("01.01.2000") &&
                            dto.getTags().isEmpty() &&
                            dto.isFlagFailedEnrichment())
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return fallback result when response body is null")
        void should_returnFallback_when_responseBodyIsNull() {
            // Arrange
            String plainText = "Test";

            when(webClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.empty());

            // Act
            Mono<EnrichmentResultDTO> result = service.enrichTextAsync(plainText);

            // Assert
            StepVerifier.create(result)
                    .expectNextMatches(EnrichmentResultDTO::isFlagFailedEnrichment)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return fallback result when response body is blank")
        void should_returnFallback_when_responseBodyIsBlank() {
            // Arrange
            String plainText = "Test";

            when(webClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("   "));

            // Act
            Mono<EnrichmentResultDTO> result = service.enrichTextAsync(plainText);

            // Assert
            StepVerifier.create(result)
                    .expectNextMatches(EnrichmentResultDTO::isFlagFailedEnrichment)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return fallback result when response processing fails")
        void should_returnFallback_when_responseProcessingFails() {
            // Arrange
            String plainText = "Test";
            String ollamaResponseBody = "{\"response\":\"Some response\"}";

            when(webClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(ollamaResponseBody));

            when(llmResponseProcessingService.extractJsonResponse(ollamaResponseBody))
                    .thenThrow(new RuntimeException("Processing error"));

            // Act
            Mono<EnrichmentResultDTO> result = service.enrichTextAsync(plainText);

            // Assert
            StepVerifier.create(result)
                    .expectNextMatches(EnrichmentResultDTO::isFlagFailedEnrichment)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return fallback result when parseEnrichmentResult returns empty")
        void should_returnFallback_when_parseReturnsEmpty() {
            // Arrange
            String plainText = "Test";
            String ollamaResponseBody = "{\"response\":\"response\"}";

            when(webClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(ollamaResponseBody));

            when(llmResponseProcessingService.extractJsonResponse(ollamaResponseBody))
                    .thenReturn("extracted");
            when(llmResponseProcessingService.extractEmbeddedJson("extracted"))
                    .thenReturn("json");
            when(llmResponseProcessingService.parseEnrichmentResult("json"))
                    .thenReturn(Optional.empty());

            // Act
            Mono<EnrichmentResultDTO> result = service.enrichTextAsync(plainText);

            // Assert
            StepVerifier.create(result)
                    .expectNextMatches(dto ->
                            dto.getTitle().equals("Unknown Title") &&
                            dto.isFlagFailedEnrichment())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Fallback Result Validation")
    class FallbackResultTests {

        @Test
        @DisplayName("should return fallback with correct default values")
        void should_havCorrectDefaults_when_fallback() {
            // Arrange
            String plainText = "Test";

            when(webClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException()));

            // Act
            Mono<EnrichmentResultDTO> result = service.enrichTextAsync(plainText);

            // Assert
            StepVerifier.create(result)
                    .assertNext(dto -> {
                        assertThat(dto.getTitle()).isEqualTo("Unknown Title");
                        assertThat(dto.getDate_sent()).isEqualTo("01.01.2000");
                        assertThat(dto.getTags()).isEmpty();
                        assertThat(dto.isFlagFailedEnrichment()).isTrue();
                    })
                    .verifyComplete();
        }
    }
}
