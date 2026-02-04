package org.papercloud.de.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Core domain object representing a page within a PDF document.
 * This is a pure domain object with no infrastructure dependencies.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Page {

    private Long id;
    private int pageNumber;
    private String pageText;
    private Document document;
}
