package org.papercloud.de.pdfservice.textutils;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.pdfservice.config.TextCleaningConfiguration;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfigurableTextCleaningService implements TextCleaningService {

    private final TextCleaningConfiguration config;

    @Override
    public String cleanOcrText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "";
        }

        return Arrays.stream(rawText.split("\n"))
                .map(String::trim)
                .filter(line -> isValidLine(line, config))
                .map(line -> normalizeCharacters(line, config))
                .map(line -> normalizeSpacing(line, config))
                .collect(Collectors.joining(" "));
    }

    private boolean isValidLine(String line, TextCleaningConfiguration config) {
        return !line.isEmpty()
                && line.length() >= config.getMinimumLineLength()
                && !line.matches(config.getSeparatorPattern());
    }

    private String normalizeCharacters(String line, TextCleaningConfiguration config) {
        return line.replaceAll(config.getNonStandardCharsPattern(), "");
    }

    private String normalizeSpacing(String line, TextCleaningConfiguration config) {
        return line.replaceAll(config.getMultipleSpacesPattern(), " ");
    }
}