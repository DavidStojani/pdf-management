package org.papercloud.de.core.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnrichmentResultDTO {
    private String title;
    private String date_sent;
    private List<TagDTO> tags;
    private boolean flagFailedEnrichment;

    public List<String> getTagNames() {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .map(TagDTO::getName)
                .toList();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagDTO {
        private String name;
    }
}
