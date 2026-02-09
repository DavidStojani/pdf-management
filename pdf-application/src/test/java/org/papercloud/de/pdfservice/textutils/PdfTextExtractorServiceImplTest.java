package org.papercloud.de.pdfservice.textutils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.ports.outbound.TextExtractionService;

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
 * Unit tests for PdfTextExtractorServiceImpl.
 * Tests strategy pattern for text extraction with fallback logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PdfTextExtractorServiceImpl Tests")
class PdfTextExtractorServiceImplTest {

    @Mock
    private TextExtractionService firstStrategy;

    @Mock
    private TextExtractionService secondStrategy;

    @Mock
    private TextExtractionService thirdStrategy;

    private PdfTextExtractorServiceImpl pdfTextExtractor;

    private byte[] testPdfBytes;

    @BeforeEach
    void setUp() {
        testPdfBytes = "PDF content".getBytes();
    }

    @Nested
    @DisplayName("Strategy Selection Tests")
    class StrategySelectionTests {

        @Test
        @DisplayName("should use first strategy when it can process the PDF")
        void should_useFirstStrategy_when_itCanProcess() throws IOException {
            // Arrange
            pdfTextExtractor = new PdfTextExtractorServiceImpl(
                    List.of(firstStrategy, secondStrategy, thirdStrategy));

            List<String> expectedPages = Arrays.asList("Page 1", "Page 2");

            when(firstStrategy.canProcess(testPdfBytes)).thenReturn(true);
            when(firstStrategy.extractText(testPdfBytes)).thenReturn(expectedPages);

            // Act
            List<String> result = pdfTextExtractor.extractTextFromPdf(testPdfBytes);

            // Assert
            assertThat(result).isEqualTo(expectedPages);
            assertThat(result).hasSize(2);

            verify(firstStrategy).canProcess(testPdfBytes);
            verify(firstStrategy).extractText(testPdfBytes);
            verify(secondStrategy, never()).canProcess(testPdfBytes);
            verify(thirdStrategy, never()).canProcess(testPdfBytes);
        }

        @Test
        @DisplayName("should fallback to second strategy when first cannot process")
        void should_useSecondStrategy_when_firstCannotProcess() throws IOException {
            // Arrange
            pdfTextExtractor = new PdfTextExtractorServiceImpl(
                    List.of(firstStrategy, secondStrategy, thirdStrategy));

            List<String> expectedPages = Arrays.asList("Page 1 OCR", "Page 2 OCR");

            when(firstStrategy.canProcess(testPdfBytes)).thenReturn(false);
            when(secondStrategy.canProcess(testPdfBytes)).thenReturn(true);
            when(secondStrategy.extractText(testPdfBytes)).thenReturn(expectedPages);

            // Act
            List<String> result = pdfTextExtractor.extractTextFromPdf(testPdfBytes);

            // Assert
            assertThat(result).isEqualTo(expectedPages);

            verify(firstStrategy).canProcess(testPdfBytes);
            verify(firstStrategy, never()).extractText(testPdfBytes);
            verify(secondStrategy).canProcess(testPdfBytes);
            verify(secondStrategy).extractText(testPdfBytes);
            verify(thirdStrategy, never()).canProcess(testPdfBytes);
        }

        @Test
        @DisplayName("should try all strategies in order until one succeeds")
        void should_tryAllStrategiesInOrder_untilOneSucceeds() throws IOException {
            // Arrange
            pdfTextExtractor = new PdfTextExtractorServiceImpl(
                    List.of(firstStrategy, secondStrategy, thirdStrategy));

            List<String> expectedPages = Collections.singletonList("Page from third strategy");

            when(firstStrategy.canProcess(testPdfBytes)).thenReturn(false);
            when(secondStrategy.canProcess(testPdfBytes)).thenReturn(false);
            when(thirdStrategy.canProcess(testPdfBytes)).thenReturn(true);
            when(thirdStrategy.extractText(testPdfBytes)).thenReturn(expectedPages);

            // Act
            List<String> result = pdfTextExtractor.extractTextFromPdf(testPdfBytes);

            // Assert
            assertThat(result).isEqualTo(expectedPages);

            verify(firstStrategy).canProcess(testPdfBytes);
            verify(secondStrategy).canProcess(testPdfBytes);
            verify(thirdStrategy).canProcess(testPdfBytes);
            verify(thirdStrategy).extractText(testPdfBytes);
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("should throw IOException when no strategy can process the PDF")
        void should_throwIOException_when_noStrategyCanProcess() throws IOException {
            // Arrange
            pdfTextExtractor = new PdfTextExtractorServiceImpl(
                    List.of(firstStrategy, secondStrategy));

            when(firstStrategy.canProcess(testPdfBytes)).thenReturn(false);
            when(secondStrategy.canProcess(testPdfBytes)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> pdfTextExtractor.extractTextFromPdf(testPdfBytes))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("No suitable text extraction strategy found for the PDF");

            verify(firstStrategy).canProcess(testPdfBytes);
            verify(secondStrategy).canProcess(testPdfBytes);
            verify(firstStrategy, never()).extractText(testPdfBytes);
            verify(secondStrategy, never()).extractText(testPdfBytes);
        }

        @Test
        @DisplayName("should throw IOException when empty strategy list")
        void should_throwIOException_when_emptyStrategyList() throws IOException {
            // Arrange
            pdfTextExtractor = new PdfTextExtractorServiceImpl(Collections.emptyList());

            // Act & Assert
            assertThatThrownBy(() -> pdfTextExtractor.extractTextFromPdf(testPdfBytes))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("No suitable text extraction strategy found for the PDF");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle single strategy")
        void should_handleSingleStrategy() throws IOException {
            // Arrange
            pdfTextExtractor = new PdfTextExtractorServiceImpl(List.of(firstStrategy));

            List<String> expectedPages = List.of("Single page");

            when(firstStrategy.canProcess(testPdfBytes)).thenReturn(true);
            when(firstStrategy.extractText(testPdfBytes)).thenReturn(expectedPages);

            // Act
            List<String> result = pdfTextExtractor.extractTextFromPdf(testPdfBytes);

            // Assert
            assertThat(result).isEqualTo(expectedPages);
            verify(firstStrategy).canProcess(testPdfBytes);
            verify(firstStrategy).extractText(testPdfBytes);
        }

        @Test
        @DisplayName("should return empty list when strategy returns empty list")
        void should_returnEmptyList_when_strategyReturnsEmpty() throws IOException {
            // Arrange
            pdfTextExtractor = new PdfTextExtractorServiceImpl(List.of(firstStrategy));

            when(firstStrategy.canProcess(testPdfBytes)).thenReturn(true);
            when(firstStrategy.extractText(testPdfBytes)).thenReturn(Collections.emptyList());

            // Act
            List<String> result = pdfTextExtractor.extractTextFromPdf(testPdfBytes);

            // Assert
            assertThat(result).isEmpty();
            verify(firstStrategy).extractText(testPdfBytes);
        }

        @Test
        @DisplayName("should handle null bytes in PDF content")
        void should_handleNullBytes() throws IOException {
            // Arrange
            pdfTextExtractor = new PdfTextExtractorServiceImpl(List.of(firstStrategy));
            byte[] nullBytes = null;

            when(firstStrategy.canProcess(nullBytes)).thenReturn(true);
            when(firstStrategy.extractText(nullBytes)).thenReturn(List.of("text"));

            // Act
            List<String> result = pdfTextExtractor.extractTextFromPdf(nullBytes);

            // Assert
            assertThat(result).hasSize(1);
            verify(firstStrategy).canProcess(nullBytes);
            verify(firstStrategy).extractText(nullBytes);
        }
    }
}
