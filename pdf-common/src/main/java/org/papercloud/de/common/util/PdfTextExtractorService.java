package org.papercloud.de.common.util;

import java.io.IOException;
import java.util.List;


public interface PdfTextExtractorService {
  List<String> extractTextFromPdf(byte[] pdfByte) throws IOException;

}
