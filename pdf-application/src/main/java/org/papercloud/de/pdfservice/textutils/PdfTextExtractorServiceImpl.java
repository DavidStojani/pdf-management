package org.papercloud.de.pdfservice.textutils;

import java.io.IOException;
import java.util.List;

import org.papercloud.de.core.ports.outbound.TextExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PdfTextExtractorServiceImpl implements PdfTextExtractorService {
    private final List<TextExtractionService> extractionStrategies;

    @Autowired
    public PdfTextExtractorServiceImpl(List<TextExtractionService> extractionStrategies) {
        // Spring will inject all implementations of TextExtractionService
        this.extractionStrategies = extractionStrategies;
    }

    @Override
    public List<String> extractTextFromPdf(byte[] pdfBytes) throws IOException {
        // Try each strategy in order until one can process the document
        for (TextExtractionService strategy : extractionStrategies) {
            if (strategy.canProcess(pdfBytes)) {
                //TODO :Here before returning the text clean it up first.
                return strategy.extractText(pdfBytes);
            }
        }

        throw new IOException("No suitable text extraction strategy found for the PDF");
    }
}
