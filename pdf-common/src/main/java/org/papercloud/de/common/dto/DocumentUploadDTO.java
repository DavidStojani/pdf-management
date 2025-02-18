package org.papercloud.de.common.dto;

import java.io.InputStream;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentUploadDTO  {
  private String fileName;
  private String contentType;
  private long size;
  private InputStream inputStream;
}
