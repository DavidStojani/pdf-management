package org.papercloud.de.common.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class DocumentDTO {
  private Long id;
  private String fileName;
  private long size;
  private LocalDateTime uploadedAt;
  private List<PageDTO> pages;
  private byte[] pdfContent;
}

