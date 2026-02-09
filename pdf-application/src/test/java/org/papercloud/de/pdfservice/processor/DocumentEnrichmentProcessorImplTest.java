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
import org.papercloud.de.core.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.core.ports.outbound.EnrichmentService;
import org.papercloud.de.core.ports.outbound.OcrTextCleaningService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfservice.errors.DocumentEnrichmentException;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.papercloud.de.pdfservice.service.DocumentStatusService;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DocumentEnrichmentProcessorImpl.
 * Tests enrichment workflow, status transitions, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentEnrichmentProcessorImpl Tests")
class DocumentEnrichmentProcessorImplTest {

    @Mock
    private EnrichmentService enrichmentService;

    @Mock
    private OcrTextCleaningService textCleaningService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PageRepository pageRepository;

    @Mock
    private DocumentStatusService documentStatusService;

    @InjectMocks
    private DocumentEnrichmentProcessorImpl enrichmentProcessor;

    private DocumentPdfEntity testDocument;
    private PagesPdfEntity testPage;

    @BeforeEach
    void setUp() {
        testDocument = DocumentPdfEntity.builder()
                .id(1L)
                .filename("test.pdf")
                .status(Document.Status.OCR_COMPLETED)
                .build();

        testPage = PagesPdfEntity.builder()
                .id(1L)
                .document(testDocument)
                .pageNumber(1)
                .pageText("Raw OCR text content")
                .build();
    }

    @Nested
    @DisplayName("Successful Enrichment Tests")
    class SuccessfulEnrichmentTests {

        @Test
        @DisplayName("should successfully enrich document with valid data")
        void should_successfullyEnrichDocument_when_validData() throws Exception {
            // Arrange
            String cleanedText = "Cleaned OCR text";
            EnrichmentResultDTO enrichmentResult = EnrichmentResultDTO.builder()
                    .title("Invoice 2023")
                    .date_sent("15.03.2023")
                    .tags(Arrays.asList(
                            new EnrichmentResultDTO.TagDTO("invoice"),
                            new EnrichmentResultDTO.TagDTO("business")
                    ))
                    .flagFailedEnrichment(false)
                    .build();

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(testPage));
            when(textCleaningService.cleanOcrText("Raw OCR text content"))
                    .thenReturn(cleanedText);
            when(enrichmentService.enrichTextAsync(cleanedText))
                    .thenReturn(Mono.just(enrichmentResult));

            // Act
            enrichmentProcessor.enrichDocument(1L);

            // Assert
            ArgumentCaptor<DocumentPdfEntity> docCaptor = ArgumentCaptor.forClass(DocumentPdfEntity.class);
            verify(documentRepository).save(docCaptor.capture());

            DocumentPdfEntity savedDoc = docCaptor.getValue();
            assertThat(savedDoc.getTitle()).isEqualTo("Invoice 2023");
            assertThat(savedDoc.getDateOnDocument()).isEqualTo(LocalDate.of(2023, 3, 15));
            assertThat(savedDoc.getTags()).containsExactly("invoice", "business");
            assertThat(savedDoc.isFailedEnrichment()).isFalse();
            assertThat(savedDoc.getStatus()).isEqualTo(Document.Status.ENRICHMENT_COMPLETED);

            verify(documentRepository).saveAndFlush(any(DocumentPdfEntity.class));
        }

        @Test
        @DisplayName("should set status to ENRICHMENT_IN_PROGRESS during processing")
        void should_setStatusToInProgress_during_processing() throws Exception {
            // Arrange
            String cleanedText = "Cleaned text";
            EnrichmentResultDTO enrichmentResult = EnrichmentResultDTO.builder()
                    .title("Test Title")
                    .date_sent("01.01.2023")
                    .tags(Collections.emptyList())
                    .flagFailedEnrichment(false)
                    .build();

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(testPage));
            when(textCleaningService.cleanOcrText(any())).thenReturn(cleanedText);
            when(enrichmentService.enrichTextAsync(cleanedText))
                    .thenReturn(Mono.just(enrichmentResult));

            // Act
            enrichmentProcessor.enrichDocument(1L);

            // Assert - verify saveAndFlush was called (status is set to ENRICHMENT_IN_PROGRESS before this call)
            verify(documentRepository).saveAndFlush(any(DocumentPdfEntity.class));
        }
    }

    @Nested
    @DisplayName("Date Parsing Tests")
    class DateParsingTests {

        @Test
        @DisplayName("should parse valid date in dd.MM.yyyy format")
        void should_parseValidDate() throws Exception {
            // Arrange
            EnrichmentResultDTO enrichmentResult = EnrichmentResultDTO.builder()
                    .title("Test")
                    .date_sent("25.12.2023")
                    .tags(Collections.emptyList())
                    .flagFailedEnrichment(false)
                    .build();

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(testPage));
            when(textCleaningService.cleanOcrText(any())).thenReturn("text");
            when(enrichmentService.enrichTextAsync(any()))
                    .thenReturn(Mono.just(enrichmentResult));

            // Act
            enrichmentProcessor.enrichDocument(1L);

            // Assert
            ArgumentCaptor<DocumentPdfEntity> captor = ArgumentCaptor.forClass(DocumentPdfEntity.class);
            verify(documentRepository).save(captor.capture());
            assertThat(captor.getValue().getDateOnDocument())
                    .isEqualTo(LocalDate.of(2023, 12, 25));
        }

        @Test
        @DisplayName("should fallback to current date when date parsing fails")
        void should_fallbackToCurrentDate_when_parsingFails() throws Exception {
            // Arrange
            EnrichmentResultDTO enrichmentResult = EnrichmentResultDTO.builder()
                    .title("Test")
                    .date_sent("invalid-date")
                    .tags(Collections.emptyList())
                    .flagFailedEnrichment(false)
                    .build();

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(testPage));
            when(textCleaningService.cleanOcrText(any())).thenReturn("text");
            when(enrichmentService.enrichTextAsync(any()))
                    .thenReturn(Mono.just(enrichmentResult));

            // Act
            enrichmentProcessor.enrichDocument(1L);

            // Assert
            ArgumentCaptor<DocumentPdfEntity> captor = ArgumentCaptor.forClass(DocumentPdfEntity.class);
            verify(documentRepository).save(captor.capture());
            assertThat(captor.getValue().getDateOnDocument())
                    .isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should throw exception when enrichment service returns null")
        void should_throwException_when_enrichmentReturnsNull() {
            // Arrange
            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(testPage));
            when(textCleaningService.cleanOcrText(any())).thenReturn("text");
            when(enrichmentService.enrichTextAsync(any()))
                    .thenReturn(Mono.empty());

            // Act & Assert
            assertThatThrownBy(() -> enrichmentProcessor.enrichDocument(1L))
                    .isInstanceOf(Exception.class);

            // updateStatus may be called twice: once in null check, once in markEnrichmentFailed
            verify(documentStatusService, atLeast(1)).updateStatus(1L, Document.Status.ENRICHMENT_ERROR);
        }

        @Test
        @DisplayName("should throw exception when document status is not OCR_COMPLETED")
        void should_throwException_when_wrongInitialStatus() {
            // Arrange
            testDocument.setStatus(Document.Status.UPLOADED);

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));

            // Act & Assert
            assertThatThrownBy(() -> enrichmentProcessor.enrichDocument(1L))
                    .isInstanceOf(DocumentEnrichmentException.class)
                    .hasMessageContaining("Document status is not OCR_COMPLETED for ID: 1");

            verify(enrichmentService, never()).enrichTextAsync(any());
            verify(documentStatusService).updateStatus(1L, Document.Status.ENRICHMENT_ERROR);
        }

        @Test
        @DisplayName("should throw exception when document not found")
        void should_throwException_when_documentNotFound() {
            // Arrange
            when(documentRepository.findById(999L))
                    .thenReturn(Optional.empty());

            // Act & Assert - DocumentNotFoundException gets wrapped in DocumentEnrichmentException
            assertThatThrownBy(() -> enrichmentProcessor.enrichDocument(999L))
                    .isInstanceOf(DocumentEnrichmentException.class)
                    .hasCauseInstanceOf(DocumentNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when no pages exist")
        void should_throwException_when_noPagesExist() {
            // Arrange
            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(Collections.emptyList());

            // Act & Assert - InvalidDocumentException gets wrapped in DocumentEnrichmentException
            assertThatThrownBy(() -> enrichmentProcessor.enrichDocument(1L))
                    .isInstanceOf(DocumentEnrichmentException.class)
                    .hasCauseInstanceOf(InvalidDocumentException.class);

            verify(enrichmentService, never()).enrichTextAsync(any());
        }

        @Test
        @DisplayName("should mark enrichment as failed and rethrow exception")
        void should_markEnrichmentFailed_when_exceptionThrown() {
            // Arrange
            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(testPage));
            when(textCleaningService.cleanOcrText(any())).thenReturn("text");
            when(enrichmentService.enrichTextAsync(any()))
                    .thenReturn(Mono.error(new RuntimeException("Enrichment service error")));

            // Act & Assert
            assertThatThrownBy(() -> enrichmentProcessor.enrichDocument(1L))
                    .isInstanceOf(DocumentEnrichmentException.class);

            verify(documentStatusService).updateStatus(1L, Document.Status.ENRICHMENT_ERROR);
        }

        @Test
        @DisplayName("should handle enrichment timeout")
        void should_handleEnrichmentTimeout() {
            // Arrange
            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(testPage));
            when(textCleaningService.cleanOcrText(any())).thenReturn("text");
            when(enrichmentService.enrichTextAsync(any()))
                    .thenReturn(Mono.delay(Duration.ofSeconds(70)).then(Mono.empty()));

            // Act & Assert
            assertThatThrownBy(() -> enrichmentProcessor.enrichDocument(1L))
                    .isInstanceOf(Exception.class);

            verify(documentStatusService).updateStatus(1L, Document.Status.ENRICHMENT_ERROR);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle empty tags list")
        void should_handleEmptyTags() throws Exception {
            // Arrange
            EnrichmentResultDTO enrichmentResult = EnrichmentResultDTO.builder()
                    .title("Test")
                    .date_sent("01.01.2023")
                    .tags(Collections.emptyList())
                    .flagFailedEnrichment(false)
                    .build();

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(testPage));
            when(textCleaningService.cleanOcrText(any())).thenReturn("text");
            when(enrichmentService.enrichTextAsync(any()))
                    .thenReturn(Mono.just(enrichmentResult));

            // Act
            enrichmentProcessor.enrichDocument(1L);

            // Assert
            ArgumentCaptor<DocumentPdfEntity> captor = ArgumentCaptor.forClass(DocumentPdfEntity.class);
            verify(documentRepository).save(captor.capture());
            assertThat(captor.getValue().getTags()).isEmpty();
        }

        @Test
        @DisplayName("should handle null tags list")
        void should_handleNullTags() throws Exception {
            // Arrange
            EnrichmentResultDTO enrichmentResult = EnrichmentResultDTO.builder()
                    .title("Test")
                    .date_sent("01.01.2023")
                    .tags(null)
                    .flagFailedEnrichment(false)
                    .build();

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(testPage));
            when(textCleaningService.cleanOcrText(any())).thenReturn("text");
            when(enrichmentService.enrichTextAsync(any()))
                    .thenReturn(Mono.just(enrichmentResult));

            // Act
            enrichmentProcessor.enrichDocument(1L);

            // Assert
            ArgumentCaptor<DocumentPdfEntity> captor = ArgumentCaptor.forClass(DocumentPdfEntity.class);
            verify(documentRepository).save(captor.capture());
            assertThat(captor.getValue().getTags()).isEmpty();
        }

        @Test
        @DisplayName("should handle multiple pages and use only first page")
        void should_useOnlyFirstPage_when_multiplePages() throws Exception {
            // Arrange
            PagesPdfEntity page2 = PagesPdfEntity.builder()
                    .id(2L)
                    .document(testDocument)
                    .pageNumber(2)
                    .pageText("Second page text")
                    .build();

            EnrichmentResultDTO enrichmentResult = EnrichmentResultDTO.builder()
                    .title("Test")
                    .date_sent("01.01.2023")
                    .tags(Collections.emptyList())
                    .flagFailedEnrichment(false)
                    .build();

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(testPage, page2));
            when(textCleaningService.cleanOcrText("Raw OCR text content"))
                    .thenReturn("Cleaned first page");
            when(enrichmentService.enrichTextAsync("Cleaned first page"))
                    .thenReturn(Mono.just(enrichmentResult));

            // Act
            enrichmentProcessor.enrichDocument(1L);

            // Assert
            verify(textCleaningService).cleanOcrText("Raw OCR text content");
            verify(enrichmentService).enrichTextAsync("Cleaned first page");
        }
    }
}
