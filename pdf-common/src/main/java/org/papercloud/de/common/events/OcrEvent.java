package org.papercloud.de.common.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OcrEvent {
    private Long documentId;
    private byte[] pdfBytes;
}
