package org.papercloud.de.core.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventValidationTest {

    // OcrEvent tests

    @Test
    void ocrEvent_nullDocumentId_shouldThrowException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new OcrEvent(null)
        );
        assertEquals("documentId must not be null", ex.getMessage());
    }

    @Test
    void ocrEvent_validDocumentId_shouldSucceed() {
        OcrEvent event = new OcrEvent(1L);
        assertEquals(1L, event.documentId());
    }

    // EnrichmentEvent tests

    @Test
    void enrichmentEvent_nullDocumentId_shouldThrowException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new EnrichmentEvent(null)
        );
        assertEquals("documentId must not be null", ex.getMessage());
    }

    @Test
    void enrichmentEvent_validDocumentId_shouldSucceed() {
        EnrichmentEvent event = new EnrichmentEvent(42L);
        assertEquals(42L, event.documentId());
    }
}
