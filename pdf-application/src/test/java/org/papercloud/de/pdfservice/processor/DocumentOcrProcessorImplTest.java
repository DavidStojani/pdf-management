package org.papercloud.de.pdfservice.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.pdfservice.textutils.PdfTextExtractorService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DocumentOcrProcessorImpl.
 * Tests OCR text extraction with validation and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentOcrProcessorImpl Tests")
class DocumentOcrProcessorImplTest {

    @Mock
    private PdfTextExtractorService textExtractorService;

    @InjectMocks
    private DocumentOcrProcessorImpl ocrProcessor;

    private byte[] validPdfBytes;

    @BeforeEach
    void setUp() {
        validPdfBytes = "PDF content".getBytes();
    }

    @Nested
    @DisplayName("Successful Extraction Tests")
    class SuccessfulExtractionTests {

        @Test
        @DisplayName("should successfully extract text from valid PDF")
        void should_extractText_when_validPdf() throws IOException {
            // Arrange
            List<String> expectedPages = Arrays.asList(
                    "Page 1 content",
                    "Page 2 content",
                    "Page 3 content"
            );

            when(textExtractorService.extractTextFromPdf(validPdfBytes))
                    .thenReturn(expectedPages);

            // Act
            List<String> result = ocrProcessor.extractTextFromPdf(validPdfBytes);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(3);
            assertThat(result).containsExactly(
                    "Page 1 content",
                    "Page 2 content",
                    "Page 3 content"
            );

            verify(textExtractorService).extractTextFromPdf(validPdfBytes);
        }

        @Test
        @DisplayName("should return single page for single page PDF")
        void should_returnSinglePage_when_singlePagePdf() throws IOException {
            // Arrange
            List<String> expectedPages = Collections.singletonList("Single page content");

            when(textExtractorService.extractTextFromPdf(validPdfBytes))
                    .thenReturn(expectedPages);

            // Act
            List<String> result = ocrProcessor.extractTextFromPdf(validPdfBytes);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo("Single page content");
        }

        @Test
        @DisplayName("should handle empty pages in extracted text")
        void should_handleEmptyPages() throws IOException {
            // Arrange
            List<String> expectedPages = Arrays.asList(
                    "Page 1 content",
                    "",
                    "Page 3 content"
            );

            when(textExtractorService.extractTextFromPdf(validPdfBytes))
                    .thenReturn(expectedPages);

            // Act
            List<String> result = ocrProcessor.extractTextFromPdf(validPdfBytes);

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result.get(1)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Null and Empty Bytes Tests")
    class NullAndEmptyBytesTests {

        @Test
        @DisplayName("should return empty list when PDF bytes are null")
        void should_returnEmptyList_when_bytesAreNull() throws IOException {
            // Arrange
            byte[] nullBytes = null;

            // Act
            List<String> result = ocrProcessor.extractTextFromPdf(nullBytes);

            // Assert
            assertThat(result).isEmpty();
            verify(textExtractorService, never()).extractTextFromPdf(nullBytes);
        }

        @Test
        @DisplayName("should return empty list when PDF bytes are empty")
        void should_returnEmptyList_when_bytesAreEmpty() throws IOException {
            // Arrange
            byte[] emptyBytes = new byte[0];

            // Act
            List<String> result = ocrProcessor.extractTextFromPdf(emptyBytes);

            // Assert
            assertThat(result).isEmpty();
            verify(textExtractorService, never()).extractTextFromPdf(emptyBytes);
        }
    }

    @Nested
    @DisplayName("IOException Handling Tests")
    class IOExceptionHandlingTests {

        @Test
        @DisplayName("should propagate IOException when text extraction fails")
        void should_propagateIOException_when_extractionFails() throws IOException {
            // Arrange
            IOException expectedException = new IOException("Failed to extract text from PDF");

            when(textExtractorService.extractTextFromPdf(validPdfBytes))
                    .thenThrow(expectedException);

            // Act & Assert
            assertThatThrownBy(() -> ocrProcessor.extractTextFromPdf(validPdfBytes))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Failed to extract text from PDF");

            verify(textExtractorService).extractTextFromPdf(validPdfBytes);
        }

        @Test
        @DisplayName("should propagate IOException with cause")
        void should_propagateIOException_withCause() throws IOException {
            // Arrange
            IOException cause = new IOException("Underlying error");
            IOException expectedException = new IOException("Extraction failed", cause);

            when(textExtractorService.extractTextFromPdf(validPdfBytes))
                    .thenThrow(expectedException);

            // Act & Assert
            assertThatThrownBy(() -> ocrProcessor.extractTextFromPdf(validPdfBytes))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Extraction failed")
                    .hasCause(cause);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle empty list returned from extractor")
        void should_handleEmptyListFromExtractor() throws IOException {
            // Arrange
            when(textExtractorService.extractTextFromPdf(validPdfBytes))
                    .thenReturn(Collections.emptyList());

            // Act
            List<String> result = ocrProcessor.extractTextFromPdf(validPdfBytes);

            // Assert
            assertThat(result).isEmpty();
            verify(textExtractorService).extractTextFromPdf(validPdfBytes);
        }

        @Test
        @DisplayName("should handle large PDF with many pages")
        void should_handleLargePdf_withManyPages() throws IOException {
            // Arrange
            List<String> manyPages = Arrays.asList(
                    "Page 1", "Page 2", "Page 3", "Page 4", "Page 5",
                    "Page 6", "Page 7", "Page 8", "Page 9", "Page 10"
            );

            when(textExtractorService.extractTextFromPdf(validPdfBytes))
                    .thenReturn(manyPages);

            // Act
            List<String> result = ocrProcessor.extractTextFromPdf(validPdfBytes);

            // Assert
            assertThat(result).hasSize(10);
            assertThat(result.get(0)).isEqualTo("Page 1");
            assertThat(result.get(9)).isEqualTo("Page 10");
        }

        @Test
        @DisplayName("should handle PDF with special characters")
        void should_handleSpecialCharacters() throws IOException {
            // Arrange
            List<String> pagesWithSpecialChars = Arrays.asList(
                    "Page with €, £, ¥ symbols",
                    "Page with unicode: 你好",
                    "Page with emojis: 😀"
            );

            when(textExtractorService.extractTextFromPdf(validPdfBytes))
                    .thenReturn(pagesWithSpecialChars);

            // Act
            List<String> result = ocrProcessor.extractTextFromPdf(validPdfBytes);

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result.get(0)).contains("€", "£", "¥");
            assertThat(result.get(1)).contains("你好");
            assertThat(result.get(2)).contains("😀");
        }

        @Test
        @DisplayName("should handle PDF with very long page text")
        void should_handleVeryLongPageText() throws IOException {
            // Arrange
            String longText = "a".repeat(10000);
            List<String> pages = Collections.singletonList(longText);

            when(textExtractorService.extractTextFromPdf(validPdfBytes))
                    .thenReturn(pages);

            // Act
            List<String> result = ocrProcessor.extractTextFromPdf(validPdfBytes);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).hasSize(10000);
        }
    }
}
