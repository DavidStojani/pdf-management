package org.papercloud.de.pdfservice.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.core.events.EnrichmentEvent;
import org.papercloud.de.core.events.IndexDocumentEvent;
import org.papercloud.de.core.events.payload.IndexDocumentPayload;
import org.papercloud.de.core.ports.outbound.EnrichmentService;
import org.papercloud.de.core.ports.outbound.OcrTextCleaningService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfservice.errors.DocumentEnrichmentException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentEnrichmentProcessorImplUnitTest {

    @Mock
    private EnrichmentService documentEnrichmentService;

    @Mock
    private OcrTextCleaningService ocrTextCleaningService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private DocumentEnrichmentProcessorImpl processor;

    @BeforeEach
    void setUpTransactionTemplate() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            TransactionCallback<EnrichmentResultDTO> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    @DisplayName("enrichDocument should clean text, persist, and publish index event")
    void enrichDocument_shouldPersistAndPublishEvent() {
        DocumentPdfEntity document = new DocumentPdfEntity();
        document.setId(42L);
        document.setContentType("application/pdf");

        EnrichmentEvent event = new EnrichmentEvent(document.getId(), List.of(" raw text "));
        EnrichmentResultDTO enrichmentResult = EnrichmentResultDTO.builder()
                .title("Sample Title")
                .date_sent("01.01.2024")
                .tags(List.of(new EnrichmentResultDTO.TagDTO("tag-1")))
                .build();

        when(ocrTextCleaningService.cleanOcrText(" raw text ")).thenReturn("raw text");
        when(documentEnrichmentService.enrichTextAsync("raw text"))
                .thenReturn(Mono.just(enrichmentResult));
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        processor.enrichDocument(event).block();

        verify(documentRepository).save(document);

        ArgumentCaptor<IndexDocumentEvent> eventCaptor = ArgumentCaptor.forClass(IndexDocumentEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        IndexDocumentPayload payload = eventCaptor.getValue().payload();
        assertThat(payload.id()).isEqualTo(document.getId());
        assertThat(payload.fileName()).isEqualTo(document.getTitle());
        assertThat(payload.tags()).containsExactly("tag-1");
    }

    @Test
    @DisplayName("enrichDocument should propagate DocumentEnrichmentException when service fails")
    void enrichDocument_shouldMapToDocumentEnrichmentException() {
        EnrichmentEvent event = new EnrichmentEvent(99L, List.of("page"));

        when(ocrTextCleaningService.cleanOcrText("page")).thenReturn("page");
        when(documentEnrichmentService.enrichTextAsync("page"))
                .thenReturn(Mono.error(new RuntimeException("downstream failure")));

        StepVerifier.create(processor.enrichDocument(event))
                .expectError(DocumentEnrichmentException.class)
                .verify();
    }

    @Test
    @DisplayName("enrichDocument should reject empty page text payloads")
    void enrichDocument_shouldValidatePageTexts() {
        EnrichmentEvent event = new EnrichmentEvent(1L, List.of());

        assertThrows(InvalidDocumentException.class, () -> processor.enrichDocument(event));
    }
}
