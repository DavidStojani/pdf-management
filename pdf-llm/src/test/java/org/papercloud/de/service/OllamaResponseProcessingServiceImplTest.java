package org.papercloud.de.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaResponseProcessingServiceImplTest {

    private OllamaResponseProcessingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OllamaResponseProcessingServiceImpl(new ObjectMapper());
    }

    @Test
    @DisplayName("extractJsonResponse: should extract concatenated responses from valid lines")
    void extractJsonResponse_validLines() {
        String input = "{\"response\": \"{\\\"title\\\": \\\"Doc1\\\"}\"}\n" +
                "{\"response\": \"{\\\"description\\\": \\\"Info\\\"}\"}";

        String result = service.extractJsonResponse(input);

        assertThat(result).contains("\"title\": \"Doc1\"").contains("\"description\": \"Info\"");
    }

    @Test
    @DisplayName("extractJsonResponse: should return empty string for null input")
    void extractJsonResponse_nullInput() {
        String result = service.extractJsonResponse(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("extractJsonResponse: should skip non-JSON lines")
    void extractJsonResponse_mixedLines() {
        String input = "Invalid line\n" +
                "{\"response\": \"{\\\"field\\\": \\\"value\\\"}\"}\n" +
                "Another bad line";

        String result = service.extractJsonResponse(input);

        assertThat(result).contains("\"field\": \"value\"");
    }

    @Test
    @DisplayName("extractEmbeddedJson: should extract JSON from triple backticks")
    void extractEmbeddedJson_tripleBackticks() {
        String input = "Here is your JSON:\n```{ \"title\": \"AI Output\" }```";
        String result = service.extractEmbeddedJson(input);

        assertThat(result).contains("\"title\": \"AI Output\"");
    }

    @Test
    @DisplayName("extractEmbeddedJson: should fallback to outermost JSON when no backticks found")
    void extractEmbeddedJson_outerJsonFallback() {
        String input = "Some noise... { \"title\": \"Fallback\" } ...more noise";

        String result = service.extractEmbeddedJson(input);

        assertThat(result).isEqualTo("{ \"title\": \"Fallback\" }");
    }

    @Test
    @DisplayName("extractEmbeddedJson: should return empty for null input")
    void extractEmbeddedJson_nullInput() {
        String result = service.extractEmbeddedJson(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("extractEmbeddedJson: should return empty when nothing to extract")
    void extractEmbeddedJson_noJsonFound() {
        String input = "No JSON here at all!";
        String result = service.extractEmbeddedJson(input);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("parseEnrichmentResult: should return DTO from valid JSON")
    void parseEnrichmentResult_validJson() {
        String json = """
                {
                    "title": "Test Title",
                    "date_sent": "25.05.2025",
                    "tags": [
                        "Vollmacht",
                        "doctor",
                        "Bonn",
                        "Vertrag"
                    ]
                }
                """;
        Optional<EnrichmentResultDTO> result = service.parseEnrichmentResult(json);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Test Title");
        assertThat(result.get().getDate_sent()).isEqualTo("25.05.2025");
        assertThat(result.get().getTagNames()).isEqualTo(List.of("Vollmacht", "doctor", "Bonn", "Vertrag"));
    }

    @Test
    @DisplayName("parseEnrichmentResult: should return empty for malformed JSON")
    void parseEnrichmentResult_invalidJson() {
        String invalidJson = "{ title: unquoted_value }";

        Optional<EnrichmentResultDTO> result = service.parseEnrichmentResult(invalidJson);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("parseEnrichmentResult: should return empty for empty JSON")
    void parseEnrichmentResult_emptyJson() {
        Optional<EnrichmentResultDTO> result = service.parseEnrichmentResult("");

        assertThat(result).isEmpty();
    }
}
