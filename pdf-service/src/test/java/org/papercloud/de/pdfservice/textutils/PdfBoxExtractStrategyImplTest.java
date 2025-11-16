package org.papercloud.de.pdfservice.textutils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfBoxExtractStrategyImplTest {

    private final PdfBoxExtractStrategyImpl strategy = new PdfBoxExtractStrategyImpl();

    @Test
    @DisplayName("extractText should return text for each page")
    void extractText_shouldReturnPerPageContent() throws IOException {
        byte[] pdfBytes = createPdfWithText("Hello PDFBox!");

        List<String> pages = strategy.extractText(pdfBytes);

        assertThat(pages).hasSize(1);
        assertThat(pages.getFirst()).contains("Hello PDFBox!");
    }

    @Test
    @DisplayName("canProcess should detect extractable text")
    void canProcess_shouldDetectExtractableText() throws IOException {
        String repeated = "This page has readable text. ".repeat(10);
        byte[] pdfBytes = createPdfWithText(repeated);

        assertThat(strategy.canProcess(pdfBytes)).isTrue();
    }

    private byte[] createPdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                document.save(outputStream);
                return outputStream.toByteArray();
            }
        }
    }
}
