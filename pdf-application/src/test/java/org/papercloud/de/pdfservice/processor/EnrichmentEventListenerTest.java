package org.papercloud.de.pdfservice.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.events.EnrichmentEvent;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnrichmentEventListener Tests")
class EnrichmentEventListenerTest {

    @Mock
    private DocumentEnrichmentProcessor enrichmentProcessor;

    @InjectMocks
    private EnrichmentEventListener enrichmentEventListener;

    private EnrichmentEvent enrichmentEvent;

    @BeforeEach
    void setUp() {
        enrichmentEvent = new EnrichmentEvent(1L);
    }

    @Test
    @DisplayName("should delegate to enrichment processor")
    void should_delegateToEnrichmentProcessor() throws Exception {
        enrichmentEventListener.handleDocumentUploaded(enrichmentEvent);
        verify(enrichmentProcessor).enrichDocument(1L);
    }

    @Test
    @DisplayName("should swallow processor exceptions and not rethrow")
    void should_swallowProcessorExceptions() throws Exception {
        doThrow(new Exception("enrichment failed")).when(enrichmentProcessor).enrichDocument(1L);

        assertThatCode(() -> enrichmentEventListener.handleDocumentUploaded(enrichmentEvent))
                .doesNotThrowAnyException();

        verify(enrichmentProcessor).enrichDocument(1L);
    }
}
