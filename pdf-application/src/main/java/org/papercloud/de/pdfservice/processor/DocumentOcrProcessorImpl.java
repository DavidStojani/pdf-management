package org.papercloud.de.pdfservice.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.pdfservice.textutils.PdfTextExtractorService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentOcrProcessorImpl implements DocumentOcrProcessor {

    private final PdfTextExtractorService textExtractorService;

    @Override
    public List<String> extractTextFromPdf(byte[] pdfBytes) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            log.warn("Received empty or null PDF byte array for OCR.");
            return Collections.emptyList();
        }

        try {
            List<String> pages = textExtractorService.extractTextFromPdf(pdfBytes);
            log.info("Successfully extracted text from PDF. Pages: {}", pages.size());
            //TODO: Set status as OCR_COMPLETED
            return pages;
        } catch (IOException e) {
            log.error("Failed to extract text from PDF", e);
            throw e;
        }
    }
}
