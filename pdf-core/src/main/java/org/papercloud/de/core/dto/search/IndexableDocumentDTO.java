package org.papercloud.de.core.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexableDocumentDTO {
    private Long id;
    private String fileName;
    private String contentType;
    private List<String> tags;
    private int year;
    private String fullText;
}
