package org.papercloud.de.core.ports.outbound;

import java.io.IOException;
import java.util.List;

/**
 * Port interface for extracting text from PDF documents.
 * Implementations may use PDFBox, Tesseract OCR, or any other extraction mechanism.
 */
public interface TextExtractionService {

    /**
     * Extracts text from each page of the PDF.
     *
     * @param pdfBytes the raw PDF bytes
     * @return a list of text strings, one per page
     * @throws IOException if text extraction fails
     */
    List<String> extractText(byte[] pdfBytes) throws IOException;

    /**
     * Checks if this extractor can process the given PDF.
     * For example, an OCR extractor might only process image-based PDFs.
     *
     * @param pdfBytes the raw PDF bytes
     * @return true if this extractor can handle the PDF
     * @throws IOException if the check fails
     */
    boolean canProcess(byte[] pdfBytes) throws IOException;
}
