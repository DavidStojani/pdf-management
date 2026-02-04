package org.papercloud.de.core.events;

import java.util.Arrays;

public record OcrEvent(Long documentId, byte[] pdfBytes) {
    public OcrEvent {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        pdfBytes = pdfBytes == null ? new byte[0] : Arrays.copyOf(pdfBytes, pdfBytes.length);
    }
}
