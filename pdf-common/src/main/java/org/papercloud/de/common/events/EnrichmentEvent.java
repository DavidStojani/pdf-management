package org.papercloud.de.common.events;

import java.util.List;

public record EnrichmentEvent(Long documentId, List<String> pageTexts) {
    public EnrichmentEvent {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        pageTexts = pageTexts == null ? List.of() : List.copyOf(pageTexts);
    }
}