package org.papercloud.de.pdfservice.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.common.events.EnrichmentEvent;
import org.papercloud.de.common.events.IndexDocumentEvent;
import org.papercloud.de.common.util.DocumentEnrichmentService;
import org.papercloud.de.common.util.OcrTextCleaningService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({DocumentEnrichmentProcessorImpl.class, DocumentEnrichmentProcessorPersistenceTest.TxConfig.class})
class DocumentEnrichmentProcessorPersistenceTest {

    @Autowired
    private DocumentEnrichmentProcessorImpl processor;

    @Autowired
    private DocumentRepository documentRepository;

    @MockBean
    private DocumentEnrichmentService documentEnrichmentService;

    @MockBean
    private OcrTextCleaningService ocrTextCleaningService;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    private DocumentPdfEntity document;

    @BeforeEach
    void setUp() {
        document = new DocumentPdfEntity();
        document.setTitle("Original");
        document.setContentType("application/pdf");
        document = documentRepository.save(document);
    }

    @Test
    @DisplayName("enrichDocument should persist enrichment fields and publish indexing event")
    void enrichDocument_shouldUpdateEntityAndPublish() {
        EnrichmentEvent event = new EnrichmentEvent(document.getId(), List.of("page text"));
        EnrichmentResultDTO enrichmentResult = EnrichmentResultDTO.builder()
                .title("Updated Title")
                .date_sent("02.02.2024")
                .tags(List.of(new EnrichmentResultDTO.TagDTO("finance"), new EnrichmentResultDTO.TagDTO("urgent")))
                .build();

        when(ocrTextCleaningService.cleanOcrText("page text")).thenReturn("page text");
        when(documentEnrichmentService.enrichTextAsync("page text")).thenReturn(Mono.just(enrichmentResult));

        processor.enrichDocument(event).block();

        DocumentPdfEntity updated = documentRepository.findById(document.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getTags()).containsExactlyInAnyOrder("finance", "urgent");
        assertThat(updated.getDateOnDocument()).isEqualTo(LocalDate.of(2024, 2, 2));

        ArgumentCaptor<IndexDocumentEvent> captor = ArgumentCaptor.forClass(IndexDocumentEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().payload().id()).isEqualTo(document.getId());
    }

    static class TxConfig {
        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager txManager) {
            return new TransactionTemplate(txManager);
        }
    }
}
