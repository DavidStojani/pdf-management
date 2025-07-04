package org.papercloud.de.common.dto.document;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

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

