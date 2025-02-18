package org.papercloud.de.pdfservice.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public interface PdfTextExtractorService {
  List<String> extractTextFromPdf(InputStream pdfInputStream) throws IOException;
}
