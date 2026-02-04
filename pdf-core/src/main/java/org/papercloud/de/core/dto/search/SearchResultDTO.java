package org.papercloud.de.core.dto.search;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResultDTO {
    private List<SearchHitDTO> hits;
    private long totalHits;
    private int totalPages;
    private int currentPage;
}
