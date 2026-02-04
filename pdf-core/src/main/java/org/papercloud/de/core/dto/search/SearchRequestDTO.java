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
public class SearchRequestDTO {
    private String query;
    private Integer year;
    private List<String> tags;
    private String username;
    private Integer page;
    private Integer size;
}
