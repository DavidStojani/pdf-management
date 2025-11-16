package org.papercloud.de.pdfservice.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.papercloud.de.common.dto.llm.EnrichmentResultDTO;
import org.papercloud.de.common.events.EnrichmentEvent;
import org.papercloud.de.common.events.IndexDocumentEvent;
import org.papercloud.de.common.util.DocumentEnrichmentService;
import org.papercloud.de.common.util.OcrTextCleaningService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfservice.errors.DocumentEnrichmentException;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest(classes = TestConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
class DocumentEnrichmentProcessorImplIntegrationTest {

    @Autowired
    private DocumentEnrichmentProcessor processor;

    @Autowired
    private TestEntityManager entityManager;

    @MockBean
    private DocumentEnrichmentService documentEnrichmentService;

    @MockBean
    private OcrTextCleaningService ocrTextCleaningService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<IndexDocumentEvent> eventCaptor;

    private DocumentPdfEntity testDocument;

    @BeforeEach
    void setUp() {
        testDocument = createAndPersistTestDocument();
    }

    @Test
    @DisplayName("Should successfully enrich document and persist changes")
    void shouldSuccessfullyEnrichDocumentAndPersistChanges() {
        // Given
        EnrichmentEvent event = new EnrichmentEvent(testDocument.getId(), List.of("test content"));
        EnrichmentResultDTO expectedResult = createEnrichmentResult();

        when(ocrTextCleaningService.cleanOcrText("test content")).thenReturn("cleaned content");
        when(documentEnrichmentService.enrichTextAsync("cleaned content"))
                .thenReturn(Mono.just(expectedResult));

        // When
        EnrichmentResultDTO result = processor.enrichDocument(event).block();

        // Then
        assertThat(result).isEqualTo(expectedResult);

        // Verify database changes
        entityManager.flush();
        entityManager.clear();

        DocumentPdfEntity updatedDocument = entityManager.find(DocumentPdfEntity.class, testDocument.getId());
        assertThat(updatedDocument.getTitle()).isEqualTo("Enriched Title");
        assertThat(updatedDocument.getDateOnDocument()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(updatedDocument.getTags()).containsExactlyInAnyOrder("finance", "important");
        assertThat(updatedDocument.isFailedEnrichment()).isFalse();
    }

    @Test
    @DisplayName("Should handle database transaction rollback on enrichment failure")
    void shouldHandleDatabaseTransactionRollbackOnEnrichmentFailure() {
        // Given
        EnrichmentEvent event = new EnrichmentEvent(testDocument.getId(), List.of("test content"));

        when(ocrTextCleaningService.cleanOcrText("test content")).thenReturn("cleaned content");
        when(documentEnrichmentService.enrichTextAsync("cleaned content"))
                .thenReturn(Mono.error(new RuntimeException("Enrichment service failed")));

        String originalTitle = testDocument.getTitle();
        LocalDate originalDate = testDocument.getDateOnDocument();

        // When & Then
        StepVerifier.create(processor.enrichDocument(event))
                .expectError(DocumentEnrichmentException.class)
                .verify();

        // Verify database state remains unchanged
        entityManager.flush();
        entityManager.clear();

        DocumentPdfEntity unchangedDocument = entityManager.find(DocumentPdfEntity.class, testDocument.getId());
        assertThat(unchangedDocument.getTitle()).isEqualTo(originalTitle);
        assertThat(unchangedDocument.getDateOnDocument()).isEqualTo(originalDate);
    }
/*
    @Test
    @DisplayName("Should mark document as failed during recovery")
    void shouldMarkDocumentAsFailedDuringRecovery() {
        // Given
        EnrichmentEvent event = new EnrichmentEvent(testDocument.getId(), List.of("test content"));
        Exception testException = new RuntimeException("Test failure");

        // When
        processor.recover(testException, event);

        // Then
        entityManager.flush();
        entityManager.clear();

        DocumentPdfEntity failedDocument = entityManager.find(DocumentPdfEntity.class, testDocument.getId());
        assertThat(failedDocument.isFailedEnrichment()).isTrue();
    }

 */

    @Test
    @DisplayName("Should handle concurrent access to same document")
    void shouldHandleConcurrentAccessToSameDocument() throws InterruptedException {
        // Given
        EnrichmentEvent event = new EnrichmentEvent(testDocument.getId(), List.of("test content"));
        EnrichmentResultDTO expectedResult = createEnrichmentResult();

        when(ocrTextCleaningService.cleanOcrText("test content")).thenReturn("cleaned content");
        when(documentEnrichmentService.enrichTextAsync("cleaned content"))
                .thenReturn(Mono.just(expectedResult));

        CountDownLatch latch = new CountDownLatch(2);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // When - Simulate concurrent enrichment
        Thread thread1 = new Thread(() -> {
            try {
                processor.enrichDocument(event).block();
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                latch.countDown();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                processor.enrichDocument(event).block();
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                latch.countDown();
            }
        });

        thread1.start();
        thread2.start();
        latch.await(5, TimeUnit.SECONDS);

        // Then - Should handle gracefully without data corruption
        assertThat(exceptions).isEmpty();

        DocumentPdfEntity finalDocument = entityManager.find(DocumentPdfEntity.class, testDocument.getId());
        assertThat(finalDocument.getTitle()).isEqualTo("Enriched Title");
    }

    @Test
    @DisplayName("Should validate document exists before enrichment")
    void shouldValidateDocumentExistsBeforeEnrichment() {
        // Given
        Long nonExistentId = 999L;
        EnrichmentEvent event = new EnrichmentEvent(nonExistentId, List.of("test content"));

        // When & Then
        StepVerifier.create(processor.enrichDocument(event))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DocumentNotFoundException.class);
                    assertThat(error).hasMessageContaining("Document not found with ID: 999");
                })
                .verify();
    }

    private DocumentPdfEntity createAndPersistTestDocument() {
        DocumentPdfEntity document = new DocumentPdfEntity();
        document.setTitle("Original Title");
        document.setDateOnDocument(LocalDate.of(2024, 1, 1));
        document.setTags(List.of("original"));
        document.setFailedEnrichment(false);

        return entityManager.persistAndFlush(document);
    }

    private EnrichmentResultDTO createEnrichmentResult() {
        return EnrichmentResultDTO.builder()
                .title("Enriched Title")
                .date_sent("15.06.2024")
                .tags(List.of(new EnrichmentResultDTO.TagDTO("finance"), new EnrichmentResultDTO.TagDTO("important")))
                .build();
    }
}

// Separate test class for event publishing verification
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:eventdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DocumentEnrichmentProcessorEventIntegrationTest {

    @Autowired
    private DocumentEnrichmentProcessor processor;

    @Autowired
    private TestEntityManager entityManager;

    @MockBean
    private DocumentEnrichmentService documentEnrichmentService;

    @MockBean
    private OcrTextCleaningService ocrTextCleaningService;

    @EventListener
    @Async
    public void handleIndexDocumentEvent(IndexDocumentEvent event) {
        // This will be called when the event is published
        receivedEvents.add(event);
    }

    private final List<IndexDocumentEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void setUp() {
        receivedEvents.clear();
    }

    @Test
    @DisplayName("Should publish IndexDocumentEvent after successful enrichment")
    void shouldPublishIndexDocumentEventAfterSuccessfulEnrichment() throws InterruptedException {
        // Given
        DocumentPdfEntity document = createAndPersistTestDocument();
        EnrichmentEvent event = new EnrichmentEvent(document.getId(), List.of("test content"));
        EnrichmentResultDTO expectedResult = createEnrichmentResult();

        when(ocrTextCleaningService.cleanOcrText("test content")).thenReturn("cleaned content");
        when(documentEnrichmentService.enrichTextAsync("cleaned content"))
                .thenReturn(Mono.just(expectedResult));

        // When
        processor.enrichDocument(event).block();

        // Then - Wait for async event processing
        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(receivedEvents).hasSize(1);
                    assertThat(receivedEvents.get(0).payload().id()).isEqualTo(document.getId());
                });
    }

    private DocumentPdfEntity createAndPersistTestDocument() {
        DocumentPdfEntity document = new DocumentPdfEntity();
        document.setTitle("Test Document");
        document.setDateOnDocument(LocalDate.now());
        document.setTags(List.of("test"));
        document.setFailedEnrichment(false);

        return entityManager.persistAndFlush(document);
    }

    private EnrichmentResultDTO createEnrichmentResult() {
        return EnrichmentResultDTO.builder()
                .title("Enriched Title")
                .date_sent("15.06.2024")
                .tags(List.of(new EnrichmentResultDTO.TagDTO("finance")))
                .build();
    }
}