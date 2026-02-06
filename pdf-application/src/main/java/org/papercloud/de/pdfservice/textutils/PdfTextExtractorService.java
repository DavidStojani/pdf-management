package org.papercloud.de.pdfservice.textutils;

import java.io.IOException;
import java.util.List;

/**
 * Internal service for extracting text from PDF documents.
 * Orchestrates multiple TextExtractionService implementations.
 */
public interface PdfTextExtractorService {
    List<String> extractTextFromPdf(byte[] pdfBytes) throws IOException;
}
