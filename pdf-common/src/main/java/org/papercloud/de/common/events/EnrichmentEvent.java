package org.papercloud.de.common.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class EnrichmentEvent {
    private final Long documentId;
    private final List<String> pageTexts;
}