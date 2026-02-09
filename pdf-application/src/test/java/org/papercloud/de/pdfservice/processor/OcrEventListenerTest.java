package org.papercloud.de.pdfservice.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.events.EnrichmentEvent;
import org.papercloud.de.core.events.OcrEvent;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.service.DocumentStatusService;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OcrEventListener.
 * Tests OCR event handling, page saving, and enrichment event triggering.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OcrEventListener Tests")
class OcrEventListenerTest {

    @Mock
    private DocumentOcrProcessor ocrProcessor;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PageRepository pageRepository;

    @Mock
    private DocumentStatusService documentStatusService;

    @InjectMocks
    private OcrEventListener ocrEventListener;

    private DocumentPdfEntity testDocument;
    private OcrEvent ocrEvent;

    @BeforeEach
    void setUp() {
        testDocument = DocumentPdfEntity.builder()
                .id(1L)
                .filename("test.pdf")
                .pdfContent("PDF content bytes".getBytes())
                .status(Document.Status.UPLOADED)
                .build();

        ocrEvent = new OcrEvent(1L);
    }

    @Nested
    @DisplayName("Document Not Found Tests")
    class DocumentNotFoundTests {

        @Test
        @DisplayName("should throw exception when document not found")
        void should_throwException_when_documentNotFound() throws IOException {
            // Arrange
            when(documentRepository.findById(1L))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> ocrEventListener.handleOcrEvent(ocrEvent))
                    .isInstanceOf(DocumentNotFoundException.class)
                    .hasMessageContaining("Document not found for ID: 1");

            verify(ocrProcessor, never()).extractTextFromPdf(any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("Null PDF Content Tests")
    class NullPdfContentTests {

        @Test
        @DisplayName("should set error status when PDF content is null")
        void should_setErrorStatus_when_pdfContentIsNull() throws IOException {
            // Arrange
            testDocument.setPdfContent(null);

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));

            // Act
            ocrEventListener.handleOcrEvent(ocrEvent);

            // Assert
            verify(documentStatusService).updateStatus(1L, Document.Status.OCR_IN_PROGRESS);
            verify(documentStatusService).updateStatus(1L, Document.Status.OCR_ERROR);
            verify(ocrProcessor, never()).extractTextFromPdf(any());
            verify(eventPublisher, never()).publishEvent(any(EnrichmentEvent.class));
        }

        @Test
        @DisplayName("should set error status when PDF content is empty")
        void should_setErrorStatus_when_pdfContentIsEmpty() throws IOException {
            // Arrange
            testDocument.setPdfContent(new byte[0]);

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));

            // Act
            ocrEventListener.handleOcrEvent(ocrEvent);

            // Assert
            verify(documentStatusService).updateStatus(1L, Document.Status.OCR_IN_PROGRESS);
            verify(documentStatusService).updateStatus(1L, Document.Status.OCR_ERROR);
            verify(ocrProcessor, never()).extractTextFromPdf(any());
            verify(eventPublisher, never()).publishEvent(any(EnrichmentEvent.class));
        }
    }

    @Nested
    @DisplayName("Successful OCR Tests")
    class SuccessfulOcrTests {

        @Test
        @DisplayName("should successfully process OCR and save pages")
        void should_successfullyProcessOcr_and_savePages() throws IOException {
            // Arrange
            List<String> extractedPages = Arrays.asList(
                    "Page 1 text",
                    "Page 2 text",
                    "Page 3 text"
            );

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(ocrProcessor.extractTextFromPdf(testDocument.getPdfContent()))
                    .thenReturn(extractedPages);

            // Act
            ocrEventListener.handleOcrEvent(ocrEvent);

            // Assert
            verify(documentStatusService).updateStatus(1L, Document.Status.OCR_IN_PROGRESS);
            verify(ocrProcessor).extractTextFromPdf(testDocument.getPdfContent());
            verify(documentStatusService).updateStatus(1L, Document.Status.OCR_COMPLETED);

            ArgumentCaptor<List<PagesPdfEntity>> pagesCaptor = ArgumentCaptor.forClass(List.class);
            verify(pageRepository).saveAll(pagesCaptor.capture());

            List<PagesPdfEntity> savedPages = pagesCaptor.getValue();
            assertThat(savedPages).hasSize(3);
            assertThat(savedPages.get(0).getPageNumber()).isEqualTo(1);
            assertThat(savedPages.get(0).getPageText()).isEqualTo("Page 1 text");
            assertThat(savedPages.get(1).getPageNumber()).isEqualTo(2);
            assertThat(savedPages.get(1).getPageText()).isEqualTo("Page 2 text");
            assertThat(savedPages.get(2).getPageNumber()).isEqualTo(3);
            assertThat(savedPages.get(2).getPageText()).isEqualTo("Page 3 text");

            ArgumentCaptor<EnrichmentEvent> eventCaptor = ArgumentCaptor.forClass(EnrichmentEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().documentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should publish enrichment event after successful OCR")
        void should_publishEnrichmentEvent_after_successfulOcr() throws IOException {
            // Arrange
            List<String> extractedPages = List.of("Single page");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(ocrProcessor.extractTextFromPdf(any()))
                    .thenReturn(extractedPages);

            // Act
            ocrEventListener.handleOcrEvent(ocrEvent);

            // Assert
            ArgumentCaptor<EnrichmentEvent> eventCaptor = ArgumentCaptor.forClass(EnrichmentEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            EnrichmentEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.documentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should handle single page document")
        void should_handleSinglePageDocument() throws IOException {
            // Arrange
            List<String> extractedPages = List.of("Only page");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(ocrProcessor.extractTextFromPdf(any()))
                    .thenReturn(extractedPages);

            // Act
            ocrEventListener.handleOcrEvent(ocrEvent);

            // Assert
            ArgumentCaptor<List<PagesPdfEntity>> pagesCaptor = ArgumentCaptor.forClass(List.class);
            verify(pageRepository).saveAll(pagesCaptor.capture());

            List<PagesPdfEntity> savedPages = pagesCaptor.getValue();
            assertThat(savedPages).hasSize(1);
            assertThat(savedPages.get(0).getPageNumber()).isEqualTo(1);
            assertThat(savedPages.get(0).getPageText()).isEqualTo("Only page");
            assertThat(savedPages.get(0).getDocument()).isEqualTo(testDocument);
        }
    }

    @Nested
    @DisplayName("OCR IOException Tests")
    class OcrIOExceptionTests {

        @Test
        @DisplayName("should set error status when IOException occurs during OCR")
        void should_setErrorStatus_when_iOExceptionDuringOcr() throws IOException {
            // Arrange
            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(ocrProcessor.extractTextFromPdf(any()))
                    .thenThrow(new IOException("OCR processing failed"));

            // Act
            ocrEventListener.handleOcrEvent(ocrEvent);

            // Assert
            verify(documentStatusService).updateStatus(1L, Document.Status.OCR_IN_PROGRESS);
            verify(documentStatusService).updateStatus(1L, Document.Status.OCR_ERROR);
            verify(pageRepository, never()).saveAll(anyList());
            verify(eventPublisher, never()).publishEvent(any(EnrichmentEvent.class));
        }

        @Test
        @DisplayName("should not save pages when IOException occurs")
        void should_notSavePages_when_iOException() throws IOException {
            // Arrange
            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(ocrProcessor.extractTextFromPdf(any()))
                    .thenThrow(new IOException("Extraction error"));

            // Act
            ocrEventListener.handleOcrEvent(ocrEvent);

            // Assert
            verify(pageRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("should not publish enrichment event when IOException occurs")
        void should_notPublishEnrichmentEvent_when_iOException() throws IOException {
            // Arrange
            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(ocrProcessor.extractTextFromPdf(any()))
                    .thenThrow(new IOException("Processing error"));

            // Act
            ocrEventListener.handleOcrEvent(ocrEvent);

            // Assert
            verify(eventPublisher, never()).publishEvent(any(EnrichmentEvent.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle empty pages list from OCR")
        void should_handleEmptyPagesList() throws IOException {
            // Arrange
            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(ocrProcessor.extractTextFromPdf(any()))
                    .thenReturn(List.of());

            // Act
            ocrEventListener.handleOcrEvent(ocrEvent);

            // Assert
            verify(documentStatusService).updateStatus(1L, Document.Status.OCR_COMPLETED);

            ArgumentCaptor<List<PagesPdfEntity>> pagesCaptor = ArgumentCaptor.forClass(List.class);
            verify(pageRepository).saveAll(pagesCaptor.capture());
            assertThat(pagesCaptor.getValue()).isEmpty();

            verify(eventPublisher).publishEvent(any(EnrichmentEvent.class));
        }

        @Test
        @DisplayName("should handle pages with empty text")
        void should_handlePagesWithEmptyText() throws IOException {
            // Arrange
            List<String> extractedPages = Arrays.asList("Text on page 1", "", "Text on page 3");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(ocrProcessor.extractTextFromPdf(any()))
                    .thenReturn(extractedPages);

            // Act
            ocrEventListener.handleOcrEvent(ocrEvent);

            // Assert
            ArgumentCaptor<List<PagesPdfEntity>> pagesCaptor = ArgumentCaptor.forClass(List.class);
            verify(pageRepository).saveAll(pagesCaptor.capture());

            List<PagesPdfEntity> savedPages = pagesCaptor.getValue();
            assertThat(savedPages).hasSize(3);
            assertThat(savedPages.get(1).getPageText()).isEmpty();
        }

        @Test
        @DisplayName("should handle very large number of pages")
        void should_handleLargeNumberOfPages() throws IOException {
            // Arrange
            List<String> manyPages = Arrays.asList(
                    "Page 1", "Page 2", "Page 3", "Page 4", "Page 5",
                    "Page 6", "Page 7", "Page 8", "Page 9", "Page 10"
            );

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(ocrProcessor.extractTextFromPdf(any()))
                    .thenReturn(manyPages);

            // Act
            ocrEventListener.handleOcrEvent(ocrEvent);

            // Assert
            ArgumentCaptor<List<PagesPdfEntity>> pagesCaptor = ArgumentCaptor.forClass(List.class);
            verify(pageRepository).saveAll(pagesCaptor.capture());

            List<PagesPdfEntity> savedPages = pagesCaptor.getValue();
            assertThat(savedPages).hasSize(10);
            assertThat(savedPages.get(9).getPageNumber()).isEqualTo(10);
        }
    }
}
