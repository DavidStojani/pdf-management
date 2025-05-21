package org.papercloud.de.pdfservice.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class PdfTextExtractorServiceImpl implements PdfTextExtractorService {

  @Override
  public List<String> extractTextFromPdf(byte[] pdfBytes) throws IOException {
    // Load document
    try (PDDocument document = Loader.loadPDF(pdfBytes)) {
      PDFTextStripper stripper = new PDFTextStripper();
      List<String> textByPage = new ArrayList<>();

      for (int i = 1; i <= document.getNumberOfPages(); i++) {
        stripper.setStartPage(i);
        stripper.setEndPage(i);
        String pageText = stripper.getText(document);
        textByPage.add(pageText);
      }

      return textByPage;
    }
  }
}
