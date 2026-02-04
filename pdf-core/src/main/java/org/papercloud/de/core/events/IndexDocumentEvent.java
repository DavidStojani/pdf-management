package org.papercloud.de.core.events;

import org.papercloud.de.core.events.payload.IndexDocumentPayload;

public record IndexDocumentEvent(IndexDocumentPayload payload) {
    public IndexDocumentEvent {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
    }
}
