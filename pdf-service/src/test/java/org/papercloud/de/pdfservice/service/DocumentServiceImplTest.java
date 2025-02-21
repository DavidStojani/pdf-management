package org.papercloud.de.pdfservice.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.common.dto.DocumentDownloadDTO;
import org.papercloud.de.common.dto.DocumentMapper;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

  @Mock
  private DocumentRepository documentRepository;

  @Mock
  private DocumentMapper documentMapper;

  @InjectMocks
  private DocumentServiceImpl documentService;

  // Test structure for download functionality
  @Test
  void downloadDocument_whenDocumentExists_shouldReturnDownloadDTO() {
    // Given
    Long documentId = 1L;
    DocumentPdfEntity entity = createSampleEntity();
    DocumentDownloadDTO expectedDto = createSampleDownloadDTO();

    when(documentRepository.findById(documentId)).thenReturn(Optional.of(entity));
    when(documentMapper.toDownloadDTO(entity)).thenReturn(expectedDto);

    // When
    DocumentDownloadDTO result = documentService.downloadDocument(documentId);

    // Then
    assertEquals(expectedDto.getId(), result.getId());
    assertEquals(expectedDto.getFileName(), result.getFileName());
    assertArrayEquals(expectedDto.getContent(), result.getContent());
    verify(documentRepository).findById(documentId);
    verify(documentMapper).toDownloadDTO(entity);
  }

  private DocumentDownloadDTO createSampleDownloadDTO() {
    return DocumentDownloadDTO.builder()
        .id(1L)
        .size(1024L)
        .content("PDF content".getBytes())
        .contentType("application/pdf")
        .fileName("test.pdf")
        .build();
  }

  private DocumentPdfEntity createSampleEntity() {
    DocumentPdfEntity entity = new DocumentPdfEntity();
    entity.setId(1L);
    entity.setFilename("test.pdf");
    entity.setPdfContent("PDF content".getBytes());
    entity.setContentType("application/pdf");
    entity.setSize(1024L);
    entity.setUploadedAt(LocalDateTime.of(2023, 8, 1, 12, 0, 0));
    return entity;
  }

  @Test
  void downloadDocument_whenDocumentNotFound_shouldThrowException() {
    // Given
    Long documentId = 1L;
    when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(RuntimeException.class, () ->
        documentService.downloadDocument(documentId));
  }
}