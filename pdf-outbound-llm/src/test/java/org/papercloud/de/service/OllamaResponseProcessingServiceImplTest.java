package org.papercloud.de.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.papercloud.de.core.dto.llm.EnrichmentResultDTO;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OllamaResponseProcessingServiceImpl.
 * Tests JSON extraction, embedded JSON parsing, and enrichment result deserialization.
 */
@DisplayName("OllamaResponseProcessingServiceImpl")
class OllamaResponseProcessingServiceImplTest {

    private OllamaResponseProcessingServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new OllamaResponseProcessingServiceImpl(objectMapper);
    }

    @Nested
    @DisplayName("extractJsonResponse")
    class ExtractJsonResponseTests {

        @Test
        @DisplayName("should extract response from single-line Ollama JSON")
        void should_extractResponse_when_singleLineJson() {
            // Arrange
            String responseBody = "{\"response\":\"Hello World\"}";

            // Act
            String result = service.extractJsonResponse(responseBody);

            // Assert
            assertThat(result).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("should concatenate responses from multi-line Ollama JSON")
        void should_concatenateResponses_when_multiLineJson() {
            // Arrange
            String responseBody = "{\"response\":\"Line 1\"}\n" +
                    "{\"response\":\" Line 2\"}\n" +
                    "{\"response\":\" Line 3\"}";

            // Act
            String result = service.extractJsonResponse(responseBody);

            // Assert
            assertThat(result).isEqualTo("Line 1 Line 2 Line 3");
        }

        @Test
        @DisplayName("should return empty string when response body is null")
        void should_returnEmpty_when_responseBodyIsNull() {
            // Act
            String result = service.extractJsonResponse(null);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string when response body is empty")
        void should_returnEmpty_when_responseBodyIsEmpty() {
            // Act
            String result = service.extractJsonResponse("");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string when response body is whitespace only")
        void should_returnEmpty_when_responseBodyIsWhitespace() {
            // Act
            String result = service.extractJsonResponse("   \n\t  ");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should skip invalid JSON lines and extract valid ones")
        void should_skipInvalidLines_when_mixedContent() {
            // Arrange
            String responseBody = "{\"response\":\"Valid line\"}\n" +
                    "Not JSON\n" +
                    "{\"response\":\" Another valid\"}";

            // Act
            String result = service.extractJsonResponse(responseBody);

            // Assert
            assertThat(result).isEqualTo("Valid line Another valid");
        }

        @Test
        @DisplayName("should return empty string when no response field in JSON")
        void should_returnEmpty_when_noResponseField() {
            // Arrange
            String responseBody = "{\"other\":\"value\"}";

            // Act
            String result = service.extractJsonResponse(responseBody);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractEmbeddedJson")
    class ExtractEmbeddedJsonTests {

        @Test
        @DisplayName("should extract JSON from triple backticks")
        void should_extractJson_when_tripleBackticks() {
            // Arrange
            String text = "Here is the JSON:\n```\n{\"title\":\"Test\"}\n```";

            // Act
            String result = service.extractEmbeddedJson(text);

            // Assert
            assertThat(result).isEqualTo("{\"title\":\"Test\"}");
        }

        @Test
        @DisplayName("should extract JSON from triple backticks with json tag")
        void should_extractJson_when_tripleBackticksWithJsonTag() {
            // Arrange
            String text = "```json\n{\"title\":\"Test\"}\n```";

            // Act
            String result = service.extractEmbeddedJson(text);

            // Assert
            assertThat(result).isEqualTo("{\"title\":\"Test\"}");
        }

        @Test
        @DisplayName("should extract raw JSON block when no backticks")
        void should_extractRawJson_when_noBackticks() {
            // Arrange
            String text = "Some text {\"title\":\"Test\",\"value\":123} more text";

            // Act
            String result = service.extractEmbeddedJson(text);

            // Assert
            assertThat(result).isEqualTo("{\"title\":\"Test\",\"value\":123}");
        }

        @Test
        @DisplayName("should return empty string when no JSON found")
        void should_returnEmpty_when_noJson() {
            // Arrange
            String text = "This is plain text without any JSON";

            // Act
            String result = service.extractEmbeddedJson(text);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string when text is null")
        void should_returnEmpty_when_textIsNull() {
            // Act
            String result = service.extractEmbeddedJson(null);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string when text is empty")
        void should_returnEmpty_when_textIsEmpty() {
            // Act
            String result = service.extractEmbeddedJson("");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should extract multiline JSON from backticks")
        void should_extractMultilineJson_when_tripleBackticks() {
            // Arrange
            String text = "```\n{\n  \"title\": \"Test\",\n  \"date_sent\": \"01.01.2025\"\n}\n```";

            // Act
            String result = service.extractEmbeddedJson(text);

            // Assert
            assertThat(result).contains("\"title\": \"Test\"");
            assertThat(result).contains("\"date_sent\": \"01.01.2025\"");
        }

        @Test
        @DisplayName("should prefer backticks over raw JSON extraction")
        void should_preferBackticks_when_bothPresent() {
            // Arrange
            String text = "{\"wrong\":\"data\"} ```{\"correct\":\"data\"}```";

            // Act
            String result = service.extractEmbeddedJson(text);

            // Assert
            assertThat(result).isEqualTo("{\"correct\":\"data\"}");
        }
    }

    @Nested
    @DisplayName("parseEnrichmentResult")
    class ParseEnrichmentResultTests {

        @Test
        @DisplayName("should parse valid JSON into EnrichmentResultDTO")
        void should_parseSuccessfully_when_validJson() {
            // Arrange
            String json = "{\"title\":\"Test Document\",\"date_sent\":\"15.01.2025\",\"tags\":[{\"name\":\"important\"},{\"name\":\"finance\"}]}";

            // Act
            Optional<EnrichmentResultDTO> result = service.parseEnrichmentResult(json);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Test Document");
            assertThat(result.get().getDate_sent()).isEqualTo("15.01.2025");
            assertThat(result.get().getTags()).hasSize(2);
            assertThat(result.get().getTags().get(0).getName()).isEqualTo("important");
            assertThat(result.get().getTags().get(1).getName()).isEqualTo("finance");
        }

        @Test
        @DisplayName("should return empty Optional when JSON is invalid")
        void should_returnEmpty_when_invalidJson() {
            // Arrange
            String json = "Not valid JSON at all";

            // Act
            Optional<EnrichmentResultDTO> result = service.parseEnrichmentResult(json);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty Optional when JSON has wrong structure")
        void should_returnEmpty_when_wrongStructure() {
            // Arrange - Jackson throws UnrecognizedPropertyException for unknown fields
            String json = "{\"wrongField\":\"value\"}";

            // Act
            Optional<EnrichmentResultDTO> result = service.parseEnrichmentResult(json);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should parse JSON with missing tags field")
        void should_parse_when_tagsFieldMissing() {
            // Arrange
            String json = "{\"title\":\"Test\",\"date_sent\":\"01.01.2025\"}";

            // Act
            Optional<EnrichmentResultDTO> result = service.parseEnrichmentResult(json);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Test");
            assertThat(result.get().getDate_sent()).isEqualTo("01.01.2025");
            assertThat(result.get().getTags()).isNull();
        }

        @Test
        @DisplayName("should parse JSON with empty tags array")
        void should_parse_when_tagsEmpty() {
            // Arrange
            String json = "{\"title\":\"Test\",\"date_sent\":\"01.01.2025\",\"tags\":[]}";

            // Act
            Optional<EnrichmentResultDTO> result = service.parseEnrichmentResult(json);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getTags()).isEmpty();
        }

        @Test
        @DisplayName("should return empty Optional when JSON is null")
        void should_returnEmpty_when_jsonIsNull() {
            // Act
            Optional<EnrichmentResultDTO> result = service.parseEnrichmentResult(null);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty Optional when JSON is empty string")
        void should_returnEmpty_when_jsonIsEmpty() {
            // Act
            Optional<EnrichmentResultDTO> result = service.parseEnrichmentResult("");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should parse JSON with all fields present")
        void should_parse_when_allFieldsPresent() {
            // Arrange
            String json = "{\"title\":\"Complete\",\"date_sent\":\"10.02.2026\",\"tags\":[{\"name\":\"tag1\"}],\"flagFailedEnrichment\":true}";

            // Act
            Optional<EnrichmentResultDTO> result = service.parseEnrichmentResult(json);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Complete");
            assertThat(result.get().getDate_sent()).isEqualTo("10.02.2026");
            assertThat(result.get().getTags()).hasSize(1);
            assertThat(result.get().isFlagFailedEnrichment()).isTrue();
        }
    }
}


