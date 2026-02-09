package org.papercloud.de.pdfservice.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.events.EnrichmentEvent;
import org.papercloud.de.pdfservice.errors.DocumentEnrichmentException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for EnrichmentEventListener.
 * Tests enrichment event handling and exception wrapping.
 */
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

    @Nested
    @DisplayName("Successful Enrichment Delegation Tests")
    class SuccessfulEnrichmentDelegationTests {

        @Test
        @DisplayName("should successfully delegate to enrichment processor")
        void should_successfullyDelegate_to_enrichmentProcessor() throws Exception {
            // Act
            enrichmentEventListener.handleDocumentUploaded(enrichmentEvent);

            // Assert
            verify(enrichmentProcessor).enrichDocument(1L);
        }

        @Test
        @DisplayName("should delegate with correct document ID")
        void should_delegateWithCorrectDocumentId() throws Exception {
            // Arrange
            EnrichmentEvent event = new EnrichmentEvent(42L);

            // Act
            enrichmentEventListener.handleDocumentUploaded(event);

            // Assert
            verify(enrichmentProcessor).enrichDocument(42L);
        }
    }

    @Nested
    @DisplayName("Exception Wrapping Tests")
    class ExceptionWrappingTests {

        @Test
        @DisplayName("should wrap checked exception in RuntimeException")
        void should_wrapCheckedException_in_runtimeException() throws Exception {
            // Arrange
            Exception checkedException = new Exception("Enrichment failed");
            doThrow(checkedException).when(enrichmentProcessor).enrichDocument(1L);

            // Act & Assert
            assertThatThrownBy(() -> enrichmentEventListener.handleDocumentUploaded(enrichmentEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasCause(checkedException);

            verify(enrichmentProcessor).enrichDocument(1L);
        }

        @Test
        @DisplayName("should wrap DocumentEnrichmentException in RuntimeException")
        void should_wrapDocumentEnrichmentException() throws Exception {
            // Arrange
            DocumentEnrichmentException enrichmentException =
                    new DocumentEnrichmentException("Document enrichment failed");
            doThrow(enrichmentException).when(enrichmentProcessor).enrichDocument(1L);

            // Act & Assert
            assertThatThrownBy(() -> enrichmentEventListener.handleDocumentUploaded(enrichmentEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasCause(enrichmentException);
        }

        @Test
        @DisplayName("should wrap IOException in RuntimeException")
        void should_wrapIOException() throws Exception {
            // Arrange
            Exception ioException = new java.io.IOException("I/O error during enrichment");
            doThrow(ioException).when(enrichmentProcessor).enrichDocument(1L);

            // Act & Assert
            assertThatThrownBy(() -> enrichmentEventListener.handleDocumentUploaded(enrichmentEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasCause(ioException);
        }

        @Test
        @DisplayName("should wrap generic Exception in RuntimeException")
        void should_wrapGenericException() throws Exception {
            // Arrange
            Exception genericException = new Exception("Generic error");
            doThrow(genericException).when(enrichmentProcessor).enrichDocument(1L);

            // Act & Assert
            assertThatThrownBy(() -> enrichmentEventListener.handleDocumentUploaded(enrichmentEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasCause(genericException)
                    .hasMessageContaining("Generic error");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle multiple events sequentially")
        void should_handleMultipleEvents_sequentially() throws Exception {
            // Arrange
            EnrichmentEvent event1 = new EnrichmentEvent(1L);
            EnrichmentEvent event2 = new EnrichmentEvent(2L);
            EnrichmentEvent event3 = new EnrichmentEvent(3L);

            // Act
            enrichmentEventListener.handleDocumentUploaded(event1);
            enrichmentEventListener.handleDocumentUploaded(event2);
            enrichmentEventListener.handleDocumentUploaded(event3);

            // Assert
            verify(enrichmentProcessor).enrichDocument(1L);
            verify(enrichmentProcessor).enrichDocument(2L);
            verify(enrichmentProcessor).enrichDocument(3L);
        }

        @Test
        @DisplayName("should not catch RuntimeException thrown by processor")
        void should_notCatchRuntimeException() throws Exception {
            // Arrange
            RuntimeException runtimeException = new RuntimeException("Runtime error");
            doThrow(runtimeException).when(enrichmentProcessor).enrichDocument(1L);

            // Act & Assert
            assertThatThrownBy(() -> enrichmentEventListener.handleDocumentUploaded(enrichmentEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasCause(runtimeException);
        }

        @Test
        @DisplayName("should handle exception with null message")
        void should_handleExceptionWithNullMessage() throws Exception {
            // Arrange
            Exception exceptionWithNullMessage = new Exception((String) null);
            doThrow(exceptionWithNullMessage).when(enrichmentProcessor).enrichDocument(1L);

            // Act & Assert
            assertThatThrownBy(() -> enrichmentEventListener.handleDocumentUploaded(enrichmentEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasCause(exceptionWithNullMessage);
        }

        @Test
        @DisplayName("should handle exception with nested cause")
        void should_handleExceptionWithNestedCause() throws Exception {
            // Arrange
            Exception rootCause = new IllegalStateException("Root cause");
            Exception intermediateCause = new Exception("Intermediate", rootCause);
            Exception topException = new Exception("Top level", intermediateCause);

            doThrow(topException).when(enrichmentProcessor).enrichDocument(1L);

            // Act & Assert
            assertThatThrownBy(() -> enrichmentEventListener.handleDocumentUploaded(enrichmentEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasCause(topException);
        }
    }
}
