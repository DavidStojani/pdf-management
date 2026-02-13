package org.papercloud.de.core.dto.document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentListItemDTO {
    private Long id;
    private String title;
    private Integer pageCount;
    private Boolean isFavourite;
}
