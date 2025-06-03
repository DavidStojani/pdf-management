package integration.builders;

import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DocumentTestDataBuilder {

    public static MockMultipartFile createValidPdfFile() {
        // Create a minimal valid PDF for testing
        byte[] pdfContent = createMinimalPdfBytes();
        return new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                pdfContent
        );
    }

    public static MockMultipartFile createInvalidFile() {
        return new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "This is not a PDF".getBytes()
        );
    }

    public static MockMultipartFile createEmptyFile() {
        return new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );
    }

    private static byte[] createMinimalPdfBytes() {
        // Minimal PDF structure for testing
        String pdfContent = "%PDF-1.4\n" +
                "1 0 obj\n" +
                "<<\n" +
                "/Type /Catalog\n" +
                "/Pages 2 0 R\n" +
                ">>\n" +
                "endobj\n" +
                "2 0 obj\n" +
                "<<\n" +
                "/Type /Pages\n" +
                "/Kids [3 0 R]\n" +
                "/Count 1\n" +
                ">>\n" +
                "endobj\n" +
                "3 0 obj\n" +
                "<<\n" +
                "/Type /Page\n" +
                "/Parent 2 0 R\n" +
                "/MediaBox [0 0 612 792]\n" +
                ">>\n" +
                "endobj\n" +
                "xref\n" +
                "0 4\n" +
                "0000000000 65535 f \n" +
                "0000000009 00000 n \n" +
                "0000000074 00000 n \n" +
                "0000000120 00000 n \n" +
                "trailer\n" +
                "<<\n" +
                "/Size 4\n" +
                "/Root 1 0 R\n" +
                ">>\n" +
                "startxref\n" +
                "207\n" +
                "%%EOF";
        return pdfContent.getBytes();
    }
}