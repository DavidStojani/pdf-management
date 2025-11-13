package org.papercloud.de.common.dto.search;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchHitDTO {

  private String documentId;
  private String documentName;
  private int pageNumber;
  private String textSnippet;
}
