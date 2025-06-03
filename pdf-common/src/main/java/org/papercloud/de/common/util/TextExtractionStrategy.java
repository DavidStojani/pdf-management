package org.papercloud.de.common.util;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public interface TextExtractionStrategy {
    List<String> extractText(byte[] pdfBytes) throws IOException;
    boolean canProcess(byte[] pdfBytes) throws IOException;
}
