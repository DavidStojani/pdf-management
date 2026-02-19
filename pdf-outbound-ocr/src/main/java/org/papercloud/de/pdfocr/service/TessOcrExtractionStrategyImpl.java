package org.papercloud.de.pdfocr.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.papercloud.de.core.ports.outbound.OcrTextCleaningService;
import org.papercloud.de.core.ports.outbound.TextExtractionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TessOcrExtractionStrategyImpl implements TextExtractionService {

    private final OcrTextCleaningService ocrTextCleaningService;
    private final Tesseract tesseract = new Tesseract();

    @Value("${tesseract.datapath}")
    private String datapath;

    @Value("${tesseract.lang}")
    private String lang;

    @Value("${tesseract.dpi:300}")
    private String dpi;

    @Value("${tesseract.psm:6}")
    private int pageSegMode;

    @PostConstruct
    private void initTesseract() {
        tesseract.setDatapath(datapath);
        tesseract.setLanguage(lang);
        tesseract.setOcrEngineMode(1);
        tesseract.setTessVariable("user_defined_dpi", dpi);
        tesseract.setPageSegMode(pageSegMode);
    }

    @Override
    public synchronized List<String> extractText(byte[] pdfBytes) throws IOException {
        List<String> textByPage = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int total = document.getNumberOfPages();
            long documentStart = System.currentTimeMillis();

            for (int i = 0; i < total; i++) {
                long renderStart = System.currentTimeMillis();
                BufferedImage pageImage = renderer.renderImageWithDPI(i, 300, ImageType.GRAY);
                long renderMs = System.currentTimeMillis() - renderStart;

                long preprocessStart = System.currentTimeMillis();
                BufferedImage processedImage = preprocessImage(pageImage);
                long preprocessMs = System.currentTimeMillis() - preprocessStart;

                try {
                    long ocrStart = System.currentTimeMillis();
                    String text = tesseract.doOCR(processedImage);
                    long ocrMs = System.currentTimeMillis() - ocrStart;

                    String cleaned = ocrTextCleaningService.cleanOcrText(text);
                    textByPage.add(cleaned);

                    log.info("[OCR] page {}/{}: render={}ms preprocess={}ms ocr={}ms chars={}",
                            i + 1, total, renderMs, preprocessMs, ocrMs, cleaned.length());
                } catch (TesseractException e) {
                    throw new RuntimeException("Tesseract OCR failed on page " + (i + 1), e);
                }
            }

            long totalMs = System.currentTimeMillis() - documentStart;
            log.info("[OCR] finished {} pages in {}ms (avg {}ms/page)",
                    total, totalMs, total > 0 ? totalMs / total : 0);
        }
        return textByPage;
    }

    private BufferedImage preprocessImage(BufferedImage input) {
        // Input is already TYPE_BYTE_GRAY (rendered directly by PDFBox)
        RescaleOp rescaleOp = new RescaleOp(1.5f, 0, null); // Increase contrast
        BufferedImage grayImage = rescaleOp.filter(input, null);

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
