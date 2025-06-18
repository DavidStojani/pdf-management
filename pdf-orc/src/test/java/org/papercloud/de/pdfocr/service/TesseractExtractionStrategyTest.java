package org.papercloud.de.pdfocr.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.common.util.OcrTextCleaningService;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@ExtendWith(MockitoExtension.class)
public class TesseractExtractionStrategyTest {

    @InjectMocks
    private TessOcrExtractionStrategyImpl tessOcrExtractionStrategy;

    @Mock
    private OcrTextCleaningService ocrTextCleaningService;
    @Mock
    private Tesseract tesseract;

    @Container
    static GenericContainer<?> tesseractContainer = new GenericContainer<>(DockerImageName.parse("tesseractshadow/tesseract4re:latest"))
            .withCommand("tail", "-f", "/dev/null") // Keep container running
            .withWorkingDirectory("/workspace");

    @Test
    void testTesseractInContainer() throws Exception {
        // Beispiel: Führe ein Tesseract-Kommando im Container aus
        String result = execInContainer("tesseract --version");

        System.out.println("Tesseract Output: \n" + result);

        assertThat(result).contains("tesseract");
    }

    private static String execInContainer(String command) throws Exception {
        String[] cmd = {"/bin/sh", "-c", command};
        var execResult = tesseractContainer.execInContainer(cmd);
        String stdout = execResult.getStdout();
        String stderr = execResult.getStderr();

        if (!stderr.isEmpty()) {
            System.err.println("Tesseract STDERR: \n" + stderr);
        }

        return stdout + stderr;
    }

    private static String tessDataPath;

    @BeforeAll
    static void setUpContainer() {
        // Get the path to tessdata inside the container
        tessDataPath = "/usr/share/tesseract-ocr/5/tessdata";

        // Verify container is running
        assertTrue(tesseractContainer.isRunning());
        System.out.println("Tesseract container started successfully");
    }


    @Test
    void testCanProcess() {
        assertTrue(tessOcrExtractionStrategy.canProcess(new byte[]{1, 2, 3}));
    }

    @Test
    void testTesseractBasicConfiguration() {
        assertDoesNotThrow(() -> {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            assertNotNull(tesseract);
        });
    }

    @Test
    void testSimpleTextRecognition() throws TesseractException, IOException {
        // Create a simple image with text
        BufferedImage image = createTestImageWithText("HELLO WORLD");

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(8); // Single word mode
        tesseract.setOcrEngineMode(1); // LSTM OCR Engine

        String result = tesseract.doOCR(image);

        assertNotNull(result);
        assertFalse(result.trim().isEmpty());

        // The OCR might not be perfect, but should contain some recognizable text
        String cleanResult = result.replaceAll("\\s+", " ").trim().toUpperCase();
        System.out.println("OCR Result: '" + cleanResult + "'");

        // Basic validation - should contain at least some letters
        assertTrue(cleanResult.matches(".*[A-Z].*"), "OCR should recognize some text");
    }

    @Test
    void testExtractTextFromSimplePdf() throws IOException {
        // Create a simple PDF with text (you'd need a test PDF file)
        // For now, test the canProcess method
        byte[] testPdfBytes = createSimplePdfBytes();

        if (testPdfBytes != null) {
            List<String> result = tessOcrExtractionStrategy.extractText(testPdfBytes);
            System.out.println(result);
            assertNotNull(result);
            assertFalse(result.isEmpty());
        } else {
            // Skip this test if we can't create a test PDF
            System.out.println("Skipping PDF test - no test PDF available");
            assert false;
        }
    }


    // Helper method to create a test image with text
    private BufferedImage createTestImageWithText(String text) {
        int width = 400;
        int height = 100;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Set white background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Set black text
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));

        // Center the text
        FontMetrics fm = g2d.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();

        g2d.drawString(text, x, y);
        g2d.dispose();

        return image;
    }

    // Helper method to create simple PDF bytes (mock implementation)
    private byte[] createSimplePdfBytes() {
        // This is a placeholder - you would need to create actual PDF bytes
        // or load from a test resource file
        try {
            Path testPdfPath = Paths.get("src/test/resources/lens.pdf");
            if (Files.exists(testPdfPath)) {
                return Files.readAllBytes(testPdfPath);
            }
        } catch (IOException e) {
            System.out.println("Could not load test PDF: " + e.getMessage());
        }
        return null;
    }
}