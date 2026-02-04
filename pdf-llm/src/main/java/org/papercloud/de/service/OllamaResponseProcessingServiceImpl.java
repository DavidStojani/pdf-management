package org.papercloud.de.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.service.internal.LlmResponseProcessingService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaResponseProcessingServiceImpl implements LlmResponseProcessingService {

    private final ObjectMapper objectMapper;

    @Override
    public String extractJsonResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(responseBody))) {
            return reader.lines()
                    .map(this::extractResponseFromLine)
                    .filter(response -> !response.isEmpty())
                    .collect(Collectors.joining());
        } catch (IOException e) {
            log.error("Error extracting JSON response from response body", e);
            return "";
        }
    }

    @Override
    public String extractEmbeddedJson(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // First try to extract content between triple backticks
        String jsonFromBackticks = extractFromTripleBackticks(text);
        if (!jsonFromBackticks.isEmpty()) {
            return jsonFromBackticks;
        }

        // Fallback: try to extract the largest outermost JSON block
        return extractOutermostJsonBlock(text);
    }

    @Override
    public Optional<EnrichmentResultDTO> parseEnrichmentResult(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return Optional.of(objectMapper.readValue(json, EnrichmentResultDTO.class));
        } catch (Exception e) {
            // Log or handle JSON parsing error
            return Optional.empty();
        }
    }

    private String extractResponseFromLine(String line) {
        try {
            JsonNode node = objectMapper.readTree(line);
            JsonNode responseNode = node.get("response");
            return responseNode != null ? responseNode.asText() : "";
        } catch (Exception e) {
            log.debug("Could not parse line as JSON: {}", line);
            return "";
        }
    }

    private String extractFromTripleBackticks(String text) {
        Pattern tripleBacktickPattern = Pattern.compile("```\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
        Matcher matcher = tripleBacktickPattern.matcher(text);

        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String extractOutermostJsonBlock(String text) {
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1).trim();
        }

        return "";
    }
}
