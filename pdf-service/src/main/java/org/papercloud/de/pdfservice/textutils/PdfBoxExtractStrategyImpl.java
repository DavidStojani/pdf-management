package org.papercloud.de.pdfservice.textutils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.papercloud.de.core.ports.outbound.TextExtractionService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfBoxExtractStrategyImpl implements TextExtractionService {
    @Override
    public List<String> extractText(byte[] pdfBytes) throws IOException {
        // Your existing PDFBox code
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<String> textByPage = new ArrayList<>();

            for (int i = 1; i <= document.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document);
                textByPage.add(pageText);
            }

            return textByPage;
        }
    }

    @Override
    public boolean canProcess(byte[] pdfBytes) throws IOException {
        // Check if PDF has extractable text
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            // If extracted text length is very small compared to page count,
            // it's likely a scanned document
            return text.trim().length() > document.getNumberOfPages() * 50;
        }
    }
}
