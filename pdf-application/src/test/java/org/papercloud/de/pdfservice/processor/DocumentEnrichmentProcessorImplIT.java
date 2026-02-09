package org.papercloud.de.pdfservice.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.core.ports.outbound.EnrichmentService;
import org.papercloud.de.core.ports.outbound.OcrTextCleaningService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfdatabase.repository.UserJpaRepository;
import org.papercloud.de.pdfservice.service.DocumentStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EntityScan("org.papercloud.de.pdfdatabase.entity")
@EnableJpaRepositories("org.papercloud.de.pdfdatabase.repository")
@Import(DocumentStatusService.class)
@ContextConfiguration(classes = DocumentEnrichmentProcessorImplIT.TestConfig.class)
@DisplayName("DocumentEnrichmentProcessorImpl Integration Tests")
class DocumentEnrichmentProcessorImplIT {

    @SpringBootApplication
    static class TestConfig {
        // Minimal config for DataJpaTest context
    }

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private StubEnrichmentService enrichmentService;
    private StubTextCleaningService textCleaningService;

    private DocumentEnrichmentProcessorImpl enrichmentProcessor;

    private DocumentPdfEntity testDocument;

    @BeforeEach
    void setUp() {
        enrichmentService = new StubEnrichmentService();
        textCleaningService = new StubTextCleaningService();

        enrichmentProcessor = new DocumentEnrichmentProcessorImpl(
                enrichmentService,
                textCleaningService,
                documentRepository,
                pageRepository,
                new DocumentStatusService(documentRepository)
        );

        UserEntity user = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
        user = userJpaRepository.save(user);

        testDocument = DocumentPdfEntity.builder()
                .filename("test.pdf")
                .owner(user)
                .uploadedAt(LocalDateTime.now())
                .status(Document.Status.OCR_COMPLETED)
                .build();
        testDocument = documentRepository.save(testDocument);

        PagesPdfEntity page = PagesPdfEntity.builder()
                .document(testDocument)
                .pageNumber(1)
                .pageText("Raw OCR text")
                .build();
        pageRepository.save(page);
    }

    @Test
    @DisplayName("should persist status transitions and enrichment result")
    void should_persistStatusTransitions_and_enrichmentResult() {
        // Arrange
        EnrichmentResultDTO result = EnrichmentResultDTO.builder()
                .title("Invoice 2023")
                .date_sent("15.03.2023")
                .tags(List.of(new EnrichmentResultDTO.TagDTO("invoice")))
                .flagFailedEnrichment(false)
                .build();

        textCleaningService.cleaned = "Cleaned text";
        enrichmentService.result = result;

        // Act
        try {
            enrichmentProcessor.enrichDocument(testDocument.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Assert
        DocumentPdfEntity updated = documentRepository.findById(testDocument.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Document.Status.ENRICHMENT_COMPLETED);
        assertThat(updated.getTitle()).isEqualTo("Invoice 2023");
        assertThat(updated.getDateOnDocument()).isEqualTo(LocalDate.of(2023, 3, 15));
        assertThat(updated.getTags()).containsExactly("invoice");
        assertThat(updated.isFailedEnrichment()).isFalse();
    }

    private static final class StubEnrichmentService implements EnrichmentService {
        private EnrichmentResultDTO result;

        @Override
        public Mono<EnrichmentResultDTO> enrichTextAsync(String plainText) {
            return Mono.just(result);
        }
    }

    private static final class StubTextCleaningService implements OcrTextCleaningService {
        private String cleaned;

        @Override
        public String cleanOcrText(String rawText) {
            return cleaned;
        }
    }
}
