package org.papercloud.de.common.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class IndexDocumentEvent {
    private final Long documentId;
}
