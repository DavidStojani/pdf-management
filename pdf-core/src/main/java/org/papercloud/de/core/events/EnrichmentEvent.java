package org.papercloud.de.core.events;

public record EnrichmentEvent(Long documentId) {
    public EnrichmentEvent {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
    }
}
