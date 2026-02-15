package org.papercloud.de.pdfservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentStatusService")
class DocumentStatusServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentStatusService documentStatusService;

    @org.junit.jupiter.api.BeforeEach
    void initConfig() {
        ReflectionTestUtils.setField(documentStatusService, "retryBackoffBaseMinutes", 15L);
        ReflectionTestUtils.setField(documentStatusService, "retryBackoffMaxMinutes", 360L);
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatusTests {
        @Test
        @DisplayName("returns status when document exists")
        void getStatus_returnsStatus() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L)
                    .status(Document.Status.OCR_COMPLETED)
                    .build();
            when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

            Document.Status status = documentStatusService.getStatus(1L);

            assertThat(status).isEqualTo(Document.Status.OCR_COMPLETED);
        }

        @Test
        @DisplayName("throws when document not found")
        void getStatus_throwsWhenNotFound() {
            when(documentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentStatusService.getStatus(1L))
                    .isInstanceOf(DocumentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {
        @Test
        @DisplayName("updates status and saves")
        void updateStatus_updatesAndSaves() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L)
                    .status(Document.Status.UPLOADED)
                    .build();
            when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

            documentStatusService.updateStatus(1L, Document.Status.OCR_IN_PROGRESS);

            assertThat(doc.getStatus()).isEqualTo(Document.Status.OCR_IN_PROGRESS);
            verify(documentRepository).save(doc);
        }
    }

    @Nested
    @DisplayName("updateStatusIfCurrent")
    class UpdateStatusIfCurrentTests {
        @Test
        @DisplayName("updates when current matches")
        void updateStatusIfCurrent_updatesWhenMatches() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L)
                    .status(Document.Status.UPLOADED)
                    .build();
            when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

            boolean updated = documentStatusService.updateStatusIfCurrent(
                    1L, Document.Status.UPLOADED, Document.Status.OCR_IN_PROGRESS);

            assertThat(updated).isTrue();
            assertThat(doc.getStatus()).isEqualTo(Document.Status.OCR_IN_PROGRESS);
            verify(documentRepository).save(doc);
        }

        @Test
        @DisplayName("does not update when current does not match")
        void updateStatusIfCurrent_doesNotUpdateWhenMismatch() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L)
                    .status(Document.Status.OCR_COMPLETED)
                    .build();
            when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

            boolean updated = documentStatusService.updateStatusIfCurrent(
                    1L, Document.Status.UPLOADED, Document.Status.OCR_IN_PROGRESS);

            assertThat(updated).isFalse();
            assertThat(doc.getStatus()).isEqualTo(Document.Status.OCR_COMPLETED);
            verify(documentRepository, never()).save(doc);
        }
    }

    @Nested
    @DisplayName("Failure/Retry metadata")
    class FailureRetryMetadataTests {

        @Test
        @DisplayName("markEnrichmentFailure should set status and increment retry metadata")
        void markEnrichmentFailure_setsRetryMetadata() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L)
                    .status(Document.Status.ENRICHMENT_IN_PROGRESS)
                    .enrichmentRetryCount(1)
                    .build();
            when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

            documentStatusService.markEnrichmentFailure(1L, "Provider timeout");

            assertThat(doc.getStatus()).isEqualTo(Document.Status.ENRICHMENT_ERROR);
            assertThat(doc.getEnrichmentRetryCount()).isEqualTo(2);
            assertThat(doc.getEnrichmentNextRetryAt()).isNotNull();
            assertThat(doc.getEnrichmentLastError()).isEqualTo("Provider timeout");
            verify(documentRepository).save(doc);
        }

        @Test
        @DisplayName("resetEnrichmentRetry should clear retry metadata")
        void resetEnrichmentRetry_clearsRetryMetadata() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L)
                    .status(Document.Status.ENRICHMENT_COMPLETED)
                    .failedEnrichment(true)
                    .enrichmentRetryCount(3)
                    .enrichmentLastError("error")
                    .build();
            doc.setEnrichmentNextRetryAt(java.time.LocalDateTime.now());
            when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

            documentStatusService.resetEnrichmentRetry(1L);

            assertThat(doc.isFailedEnrichment()).isFalse();
            assertThat(doc.getEnrichmentRetryCount()).isZero();
            assertThat(doc.getEnrichmentLastError()).isNull();
            assertThat(doc.getEnrichmentNextRetryAt()).isNull();
            verify(documentRepository).save(doc);
        }

        @Test
        @DisplayName("markIndexingFailure should set status and increment retry metadata")
        void markIndexingFailure_setsRetryMetadata() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L)
                    .status(Document.Status.INDEXING_IN_PROGRESS)
                    .indexingRetryCount(1)
                    .build();
            when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

            documentStatusService.markIndexingFailure(1L, "ES connection refused");

            assertThat(doc.getStatus()).isEqualTo(Document.Status.INDEXING_ERROR);
            assertThat(doc.getIndexingRetryCount()).isEqualTo(2);
            assertThat(doc.getIndexingNextRetryAt()).isNotNull();
            assertThat(doc.getIndexingLastError()).isEqualTo("ES connection refused");
            verify(documentRepository).save(doc);
        }

        @Test
        @DisplayName("resetIndexingRetry should clear retry metadata")
        void resetIndexingRetry_clearsRetryMetadata() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L)
                    .status(Document.Status.INDEXING_COMPLETED)
                    .indexingRetryCount(3)
                    .indexingLastError("error")
                    .build();
            doc.setIndexingNextRetryAt(java.time.LocalDateTime.now());
            when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

            documentStatusService.resetIndexingRetry(1L);

            assertThat(doc.getIndexingRetryCount()).isZero();
            assertThat(doc.getIndexingLastError()).isNull();
            assertThat(doc.getIndexingNextRetryAt()).isNull();
            verify(documentRepository).save(doc);
        }
    }
}
