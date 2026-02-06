package org.papercloud.de.pdfservice.processor;

import java.io.IOException;
import java.util.List;

public interface DocumentOcrProcessor {
    List<String> extractTextFromPdf(byte[] pdfByte) throws IOException;
}
