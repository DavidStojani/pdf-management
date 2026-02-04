package org.papercloud.de.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core domain object representing a PDF document.
 * This is a pure domain object with no infrastructure dependencies.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    private Long id;
    private String title;
    private String filename;
    private String contentType;
    private Long size;
    private byte[] pdfContent;
    private LocalDateTime uploadedAt;

    @Builder.Default
    private List<Page> pages = new ArrayList<>();

    private User owner;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private LocalDate dateOnDocument;
    private boolean failedEnrichment;

    /**
     * Adds a page to this document.
     */
    public void addPage(Page page) {
        if (pages == null) {
            pages = new ArrayList<>();
        }
        pages.add(page);
        page.setDocument(this);
    }
}
