package org.papercloud.de.common.events;

import org.papercloud.de.common.events.payload.IndexDocumentPayload;

public record IndexDocumentEvent(IndexDocumentPayload payload) {
    public IndexDocumentEvent {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
    }
}
