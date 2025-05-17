package org.papercloud.de.common.dto.document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentDownloadDTO {

  private Long id;
  private String fileName;
  private long size;
  private byte[] content;
  private String contentType;
}
