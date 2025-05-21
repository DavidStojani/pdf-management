package org.papercloud.de.pdfservice.utils;

import java.io.IOException;
import java.util.List;


public interface PdfTextExtractorService {
  List<String> extractTextFromPdf(byte[] pdfByte) throws IOException;
}
