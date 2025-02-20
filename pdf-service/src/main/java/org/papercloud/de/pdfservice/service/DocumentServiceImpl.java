package org.papercloud.de.pdfservice.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.papercloud.de.common.dto.DocumentDTO;
import org.papercloud.de.common.dto.DocumentMapper;
import org.papercloud.de.common.dto.DocumentUploadDTO;
import org.papercloud.de.common.dto.PageDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfservice.utils.PdfTextExtractorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

  private final DocumentRepository documentRepository;
  private final PageRepository pageRepository;
  private final PdfTextExtractorService pdfTextExtractorService;
  private final DocumentMapper documentMapper;

  @Override
  @Transactional
  public DocumentDTO processDocument(DocumentUploadDTO file) throws IOException {
    byte[] pdfBytes = extractBytesFromInputStream(file.getInputStream());

    DocumentPdfEntity documentEntity = saveDocument(file, pdfBytes);

    extractAndSavePages(documentEntity, pdfBytes);

    return documentMapper.toDocumentDTO(documentEntity);
  }

  @Override
  public DocumentDTO getDocument(Long id) {
    return null;
  }

  @Override
  public byte[] getDocumentContent(Long id) {
    return new byte[0];
  }

  private byte[] extractBytesFromInputStream(InputStream inputStream) throws IOException {
    return StreamUtils.copyToByteArray(inputStream);
  }

  private DocumentPdfEntity saveDocument(DocumentUploadDTO file, byte[] pdfBytes) {
    DocumentPdfEntity documentEntity = DocumentPdfEntity.builder()
        .title(extractTitle(file))
        .filename(file.getFileName())
        .contentType(file.getContentType())
        .pdfContent(pdfBytes)
        .size(file.getSize())
        .uploadedAt(LocalDateTime.now())
        .build();

    return documentRepository.save(documentEntity);
  }

  private List<PagesPdfEntity> extractAndSavePages(DocumentPdfEntity document, byte[] pdfBytes)
      throws IOException {
    List<String> textByPage = pdfTextExtractorService.extractTextFromPdf(
        new ByteArrayInputStream(pdfBytes));
    List<PagesPdfEntity> pagesPdfEntityList = new ArrayList<>();

    for (int i = 0; i < textByPage.size(); i++) {
      PagesPdfEntity pagesPdfEntity = PagesPdfEntity.builder()
          .document(document)
          .pageNumber(i + 1)
          .pageText(textByPage.get(i)).build();

      pagesPdfEntityList.add(pagesPdfEntity);
    }
    return pageRepository.saveAll(pagesPdfEntityList);
  }

  private String extractTitle(DocumentUploadDTO file) {
    // TODO: Extract metadata title if available
    return file.getFileName();
  }
}

