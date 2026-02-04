package org.papercloud.de.core.ports.outbound;

/**
 * Port interface for cleaning OCR-extracted text.
 */
public interface OcrTextCleaningService {
    String cleanOcrText(String rawText);
}
