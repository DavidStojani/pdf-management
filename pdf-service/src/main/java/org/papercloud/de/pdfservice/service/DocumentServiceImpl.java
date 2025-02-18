package org.papercloud.de.pdfservice.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

  private final DocumentRepository documentRepository;
  private final PageRepository pageRepository;
  private final PdfTextExtractorService pdfTextExtractorService;

  @Override
  public DocumentDTO processDocument(DocumentUploadDTO file) throws IOException {
    //work with the inputstream from dto
    try (InputStream pdfInputStream = file.getInputStream()) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      pdfInputStream.transferTo(outputStream);
      byte[] pdfBytes = outputStream.toByteArray();

      //extract text
      List<String> textByPage = pdfTextExtractorService.extractTextFromPdf(new ByteArrayInputStream(pdfBytes));

      //fill the document with info from dto
      DocumentPdfEntity documentPdfEntity = DocumentPdfEntity.builder()
          .title(file.getFileName()) // ToDo: Get title from metadata(AI)
          .filename(file.getFileName())
          .contentType(file.getContentType())
          .pdfContent(pdfBytes)
          .size(file.getSize())
          .uploadedAt(LocalDateTime.now())
          .build();

      DocumentPdfEntity savedDocumentPdfEntity = documentRepository.save(documentPdfEntity);

      List<PagesPdfEntity> pagesPdfEntityList = new ArrayList<>();

      for (int i = 0; i < textByPage.size(); i++) {
        PagesPdfEntity pagesPdfEntity = PagesPdfEntity.builder()
            .document(savedDocumentPdfEntity)
            .pageNumber(i + 1)
            .pageText(textByPage.get(i)).build();

        pagesPdfEntityList.add(pagesPdfEntity);
      }
      pageRepository.saveAll(pagesPdfEntityList);

      //save the document
      return null;
    }
  }
    @Override
    public DocumentDTO getDocument (Long id){
      return documentRepository.findById(id)
          .map(document -> mapDocumentToDTO(document, document.getPages()))
          .orElse(null);
    }

    @Override
    public byte[] getDocumentContent (Long id){
      return new byte[0];
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

