package org.papercloud.de.pdfservice.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FailedDocumentRecoveryScheduler Tests")
class FailedDocumentRecoverySchedulerTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FailedDocumentRecoveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "retryEnabled", true);
        ReflectionTestUtils.setField(scheduler, "maxAttempts", 5);
        ReflectionTestUtils.setField(scheduler, "batchSize", 50);
    }

    @Test
    @DisplayName("should publish OCR, enrichment, and indexing retry events for eligible documents")
    void should_publishRetryEvents() {
        DocumentPdfEntity ocrDoc = DocumentPdfEntity.builder().id(1L).build();
        DocumentPdfEntity enrichmentDoc = DocumentPdfEntity.builder().id(2L).build();
        DocumentPdfEntity indexingDoc = DocumentPdfEntity.builder().id(3L).build();

        when(documentRepository.findRetryableOcrDocuments(any(LocalDateTime.class), eq(5), any(Pageable.class)))
                .thenReturn(List.of(ocrDoc));
        when(documentRepository.findRetryableEnrichmentDocuments(any(LocalDateTime.class), eq(5), any(Pageable.class)))
                .thenReturn(List.of(enrichmentDoc));
        when(documentRepository.findRetryableIndexingDocuments(any(LocalDateTime.class), eq(5), any(Pageable.class)))
                .thenReturn(List.of(indexingDoc));

        scheduler.retryFailedDocuments();

        verify(eventPublisher).publishEvent(any(org.papercloud.de.core.events.OcrEvent.class));
        verify(eventPublisher).publishEvent(any(org.papercloud.de.core.events.EnrichmentEvent.class));
        verify(eventPublisher).publishEvent(any(org.papercloud.de.core.events.DocumentIndexingEvent.class));
    }

    @Test
    @DisplayName("should publish indexing retry events for indexing-failed documents")
    void should_publishIndexingRetryEvents() {
        DocumentPdfEntity indexingDoc = DocumentPdfEntity.builder().id(5L).build();

        when(documentRepository.findRetryableOcrDocuments(any(LocalDateTime.class), eq(5), any(Pageable.class)))
                .thenReturn(List.of());
        when(documentRepository.findRetryableEnrichmentDocuments(any(LocalDateTime.class), eq(5), any(Pageable.class)))
                .thenReturn(List.of());
        when(documentRepository.findRetryableIndexingDocuments(any(LocalDateTime.class), eq(5), any(Pageable.class)))
                .thenReturn(List.of(indexingDoc));

        scheduler.retryFailedDocuments();

        verify(eventPublisher).publishEvent(any(org.papercloud.de.core.events.DocumentIndexingEvent.class));
    }

    @Test
    @DisplayName("should skip execution when retry is disabled")
    void should_skipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "retryEnabled", false);

        scheduler.retryFailedDocuments();

        verify(documentRepository, never()).findRetryableOcrDocuments(any(LocalDateTime.class), anyInt(), any(Pageable.class));
        verify(documentRepository, never()).findRetryableEnrichmentDocuments(any(LocalDateTime.class), anyInt(), any(Pageable.class));
        verify(documentRepository, never()).findRetryableIndexingDocuments(any(LocalDateTime.class), anyInt(), any(Pageable.class));
        verify(eventPublisher, never()).publishEvent(any());
    }
}
