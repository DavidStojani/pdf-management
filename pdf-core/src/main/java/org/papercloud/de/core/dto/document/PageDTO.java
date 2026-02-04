package org.papercloud.de.core.dto.document;

import lombok.Data;

@Data
public class PageDTO {
    private int pageNumber;
    private String extractedText;
}
