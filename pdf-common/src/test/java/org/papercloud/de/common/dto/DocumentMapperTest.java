package org.papercloud.de.common.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;

class DocumentMapperTest {

  private DocumentMapper documentMapper = Mappers.getMapper(DocumentMapper.class);

  @Test
  void toDocumentDTO() {
    DocumentPdfEntity documentPdfEntity = DocumentPdfEntity.builder()
        .pdfContent("PDF content".getBytes())
        .id(1L)
        .title("Test Document")
        .contentType("application/pdf")
        .filename("test.pdf")
        .size(1024L)
        .uploadedAt(LocalDateTime.of(2023, 8, 1, 12, 0, 0))
        .build();

    DocumentDTO documentDTO = documentMapper.toDocumentDTO(documentPdfEntity);

    assertEquals(documentPdfEntity.getId(), documentDTO.getId());
    assertEquals(documentPdfEntity.getFilename(), documentDTO.getFileName());
    assertEquals(documentPdfEntity.getSize(), documentDTO.getSize());
    assertEquals(documentPdfEntity.getUploadedAt(), documentDTO.getUploadedAt());
  }

  @Test
  void toDownloadDTO() {
    DocumentPdfEntity documentPdfEntity = DocumentPdfEntity.builder()
        .pdfContent("PDF content".getBytes())
        .id(1L)
        .title("Test Document")
        .contentType("application/pdf")
        .filename("test.pdf")
        .size(1024L)
        .uploadedAt(LocalDateTime.of(2023, 8, 1, 12, 0, 0))
        .build();

    DocumentDownloadDTO documentDownloadDTO = documentMapper.toDownloadDTO(documentPdfEntity);

    assertEquals(documentPdfEntity.getId(), documentDownloadDTO.getId());
    assertEquals(documentPdfEntity.getFilename(), documentDownloadDTO.getFileName());
    assertArrayEquals(documentPdfEntity.getPdfContent(), documentDownloadDTO.getContent());
    assertEquals(documentPdfEntity.getContentType(), documentDownloadDTO.getContentType());
  }

  @Test
  void toEntity() {

    DocumentUploadDTO documentUploadDTO = DocumentUploadDTO.builder()
        .fileName("test.pdf")
        .contentType("application/pdf")
        .size(1024L)
        .build();

    DocumentPdfEntity documentPdfEntity = documentMapper.toEntity(documentUploadDTO);

    assertEquals(documentUploadDTO.getFileName(), documentPdfEntity.getFilename());
    assertEquals(documentUploadDTO.getContentType(), documentPdfEntity.getContentType());
    assertEquals(documentUploadDTO.getSize(), documentPdfEntity.getSize());
    assertEquals(documentUploadDTO.getInputStream(), documentPdfEntity.getPdfContent());
  }

  @Test
  void toPageDTO() {
    PagesPdfEntity pagesPdfEntity = PagesPdfEntity.builder()
        .id(1L)
        .pageNumber(1)
        .pageText("Test page 1")
        .build();

    PageDTO pageDTO = documentMapper.toPageDTO(pagesPdfEntity);

    assertEquals(pagesPdfEntity.getPageNumber(), pageDTO.getPageNumber());
    assertEquals(pagesPdfEntity.getPageText(), pageDTO.getExtractedText());
  }
}