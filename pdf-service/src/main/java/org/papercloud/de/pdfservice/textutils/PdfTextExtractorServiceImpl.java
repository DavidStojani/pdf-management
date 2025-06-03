package org.papercloud.de.pdfservice.textutils;

import java.io.IOException;
import java.util.List;

import org.papercloud.de.common.util.TextExtractionStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PdfTextExtractorServiceImpl implements PdfTextExtractorService {
    private final List<TextExtractionStrategy> extractionStrategies;

    @Autowired
    public PdfTextExtractorServiceImpl(List<TextExtractionStrategy> extractionStrategies) {
        // Spring will inject all implementations of TextExtractionStrategy
        this.extractionStrategies = extractionStrategies;
    }

    @Override
    public List<String> extractTextFromPdf(byte[] pdfBytes) throws IOException {
        // Try each strategy in order until one can process the document
        for (TextExtractionStrategy strategy : extractionStrategies) {
            if (strategy.canProcess(pdfBytes)) {
                return strategy.extractText(pdfBytes);
            }
        }

        throw new IOException("No suitable text extraction strategy found for the PDF");
    }
}
