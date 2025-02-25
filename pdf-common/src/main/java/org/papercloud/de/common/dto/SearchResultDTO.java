package org.papercloud.de.common.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResultDTO {
  private List<SearchHitDTO> hits;
  private long totalHits;
  private int totalPages;
  private int currentPage;
}
