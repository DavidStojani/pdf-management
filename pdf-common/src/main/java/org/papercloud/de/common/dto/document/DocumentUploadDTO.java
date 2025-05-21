package org.papercloud.de.common.dto.document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentUploadDTO  {
  private String fileName;
  private String contentType;
  private long size;
  private byte[] inputPdfBytes;
}
