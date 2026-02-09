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
}
