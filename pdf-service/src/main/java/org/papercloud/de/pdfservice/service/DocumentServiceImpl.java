package org.papercloud.de.pdfservice.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.papercloud.de.common.dto.DocumentDTO;
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

  @Override
  @Transactional
  public DocumentDTO processDocument(DocumentUploadDTO file) throws IOException {
    byte[] pdfBytes = extractBytesFromInputStream(file.getInputStream());

    DocumentPdfEntity documentEntity = saveDocument(file, pdfBytes);

    List<PagesPdfEntity> pagesEntities = extractAndSavePages(documentEntity, pdfBytes);

    return mapDocumentToDTO(documentEntity, pagesEntities);
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

    List<PagesPdfEntity> pagesPdfEntityList = IntStream.rangeClosed(1, textByPage.size())
        .mapToObj(p -> PagesPdfEntity.builder()
            .document(document)
            .pageText(textByPage.get(p))
            .pageNumber(p + 1)
            .build())
        .collect(Collectors.toList());

    return pageRepository.saveAll(pagesPdfEntityList);
  }

  private String extractTitle(DocumentUploadDTO file) {
    // TODO: Extract metadata title if available
    return file.getFileName();
  }

  private DocumentDTO mapDocumentToDTO(DocumentPdfEntity document, List<PagesPdfEntity> pages) {
    DocumentDTO dto = new DocumentDTO();
    dto.setId(document.getId());
    dto.setFileName(document.getFilename());
    dto.setSize(document.getSize());
    dto.setUploadedAt(document.getUploadedAt());

    List<PageDTO> pageDTOs = pages.stream()
        .map(p -> {
          PageDTO pageDTO = new PageDTO();
          pageDTO.setPageNumber(p.getPageNumber());
          pageDTO.setExtractedText(p.getPageText());
          return pageDTO;
        })
        .collect(Collectors.toList());

    dto.setPages(pageDTOs);
    return dto;
  }
}

