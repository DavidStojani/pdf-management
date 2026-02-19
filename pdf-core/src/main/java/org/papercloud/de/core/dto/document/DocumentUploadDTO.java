package org.papercloud.de.core.dto.document;

import lombok.Builder;
import lombok.Data;
import org.papercloud.de.core.domain.UploadSource;

@Data
@Builder
public class DocumentUploadDTO {
    private String fileName;
    private String contentType;
    private long size;
    private byte[] inputPdfBytes;
    private UploadSource uploadSource;
}
