package org.papercloud.de.core.events;

public record OcrEvent(Long documentId) {
    public OcrEvent {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
    }
}
