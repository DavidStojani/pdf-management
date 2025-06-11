package org.papercloud.de.pdfservice.textutils;

import org.papercloud.de.common.util.OcrTextCleaningService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@Component
public class BasicOcrTextCleaningServiceImpl implements OcrTextCleaningService {

    private static final String SEPARATOR_PATTERN = ".*[|/\\\\_~^*]{3,}.*";
    private static final String NON_STANDARD_CHARS_PATTERN = "[^\\p{L}\\p{N}\\p{P}\\p{Z}]";
    private static final String MULTIPLE_SPACES_PATTERN = "\\s{2,}";
    private static final int MINIMUM_LINE_LENGTH = 3;

    public String cleanOcrText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "";
        }

        return Arrays.stream(rawText.split("\n"))
                .map(String::trim)
                .filter(this::isValidLine)
                .map(this::normalizeCharacters)
                .map(this::normalizeSpacing)
                .collect(Collectors.joining(" "));
    }

    private boolean isValidLine(String line) {
        return !line.isEmpty()
                && line.length() >= MINIMUM_LINE_LENGTH
                && !line.matches(SEPARATOR_PATTERN);
    }

    private String normalizeCharacters(String line) {
        return line.replaceAll(NON_STANDARD_CHARS_PATTERN, "");
    }

    private String normalizeSpacing(String line) {
        return line.replaceAll(MULTIPLE_SPACES_PATTERN, " ");
    }
}