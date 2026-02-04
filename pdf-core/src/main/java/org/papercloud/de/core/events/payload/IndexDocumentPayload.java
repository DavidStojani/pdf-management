package org.papercloud.de.core.events.payload;

import org.papercloud.de.core.dto.search.IndexableDocumentDTO;

import java.util.List;

public record IndexDocumentPayload(
        Long id,
        String fileName,
        String contentType,
        List<String> tags,
        int year,
        String fullText
) {
    public IndexDocumentPayload {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        tags = tags == null ? List.of() : List.copyOf(tags);
        fullText = fullText == null ? "" : fullText;
    }

    public IndexableDocumentDTO toDto() {
        return IndexableDocumentDTO.builder()
                .id(id)
                .fileName(fileName)
                .contentType(contentType)
                .tags(tags)
                .year(year)
                .fullText(fullText)
                .build();
    }
}
