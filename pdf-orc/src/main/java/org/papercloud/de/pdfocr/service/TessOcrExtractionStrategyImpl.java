package org.papercloud.de.pdfocr.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.papercloud.de.common.util.TextExtractionStrategy;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class TessOcrExtractionStrategyImpl implements TextExtractionStrategy {
    private String tesseractDataPath = "/usr/share/tesseract-ocr/4.00/tessdata";
    private String tesseractLanguage = "deu";


    public void setTesseractDataPath(String tesseractDataPath) {
        this.tesseractDataPath = tesseractDataPath;
    }

    public void setTesseractLanguage(String tesseractLanguage) {
        this.tesseractLanguage = tesseractLanguage;
    }

    @Override
    public List<String> extractText(byte[] pdfBytes) throws IOException {
        List<String> textByPage = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tesseractDataPath); // Adjust your tessdata path
            tesseract.setLanguage(tesseractLanguage);
            tesseract.setVariable("dpi","300");
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                // Render PDF page to high-res image
                BufferedImage pageImage = renderer.renderImageWithDPI(i, 300);

                // Preprocess image for better OCR accuracy
                BufferedImage processedImage = preprocessImage(pageImage);

                // OCR with Tesseract
                try {
                    String text = tesseract.doOCR(processedImage);
                    textByPage.add(text);
                } catch (TesseractException e) {
                    throw new RuntimeException("Tesseract OCR failed on page " + (i + 1), e);
                }
            }
        }
        return textByPage;
    }

    private BufferedImage preprocessImage(BufferedImage input) {
        // Convert to grayscale
        BufferedImage grayImage = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(input, 0, 0, null);
        g2d.dispose();

        // Binarization (simple thresholding)
        BufferedImage binaryImage = new BufferedImage(grayImage.getWidth(), grayImage.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = binaryImage.createGraphics();
        g.drawImage(grayImage, 0, 0, null);
        g.dispose();

        return binaryImage;
    }
    @Override
    public boolean canProcess(byte[] pdfBytes) {
        // This strategy can process any PDF, but we'll let it be the fallback
        return true;
    }
}
