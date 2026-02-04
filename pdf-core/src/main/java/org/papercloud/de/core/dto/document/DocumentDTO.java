package org.papercloud.de.core.dto.document;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DocumentDTO {
    private Long id;
    private String fileName;
    private long size;
    private LocalDateTime uploadedAt;
    private List<PageDTO> pages;
    private byte[] pdfContent;
}
