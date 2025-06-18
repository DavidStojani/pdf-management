package org.papercloud.de.pdfservice.search;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.common.dto.document.DocumentDownloadDTO;
import org.papercloud.de.common.dto.document.DocumentMapper;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

  @Mock
  private DocumentRepository documentRepository;

  @Mock
  private DocumentMapper documentMapper;

  @InjectMocks
  private DocumentServiceImpl documentService;

  // Download success
  @Test
  void downloadDocument_whenDocumentExistsAndUserIsOwner_shouldReturnDownloadDTO() {
    // Given
    Long documentId = 1L;
    String username = "testuser";

    DocumentPdfEntity entity = createSampleEntity(username);
    DocumentDownloadDTO expectedDto = createSampleDownloadDTO();

    when(documentRepository.findById(documentId)).thenReturn(Optional.of(entity));
    when(documentMapper.toDownloadDTO(entity)).thenReturn(expectedDto);

    // When
      DocumentDownloadDTO result = null;
      try {
          result = documentService.downloadDocument(username, documentId);
      } catch (AccessDeniedException e) {
          throw new RuntimeException(e);
      }

      // Then
    assertNotNull(result);
    assertEquals(expectedDto.getId(), result.getId());
    assertEquals(expectedDto.getFileName(), result.getFileName());
    assertArrayEquals(expectedDto.getContent(), result.getContent());
    verify(documentRepository).findById(documentId);
    verify(documentMapper).toDownloadDTO(entity);
  }

  // Not found
  @Test
  void downloadDocument_whenDocumentNotFound_shouldThrowNoSuchElementException() {
    // Given
    Long documentId = 1L;
    when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

    // When / Then
    assertThrows(ResponseStatusException.class, () ->
            documentService.downloadDocument("anyuser", documentId)
    );
  }

  // Access denied
  @Test
  void downloadDocument_whenUserIsNotOwner_shouldThrowAccessDeniedException() {
    // Given
    Long documentId = 1L;
    DocumentPdfEntity entity = createSampleEntity("owneruser");
    when(documentRepository.findById(documentId)).thenReturn(Optional.of(entity));

    // When / Then
    assertThrows(AccessDeniedException.class, () ->
            documentService.downloadDocument("intruder", documentId)
    );
  }

  // === Test helpers ===

  private DocumentDownloadDTO createSampleDownloadDTO() {
    return DocumentDownloadDTO.builder()
            .id(1L)
            .size(1024L)
            .content("PDF content".getBytes())
            .contentType("application/pdf")
            .fileName("test.pdf")
            .build();
  }

  private DocumentPdfEntity createSampleEntity(String ownerUsername) {
    UserEntity owner = new UserEntity();
    owner.setUsername(ownerUsername);

    DocumentPdfEntity entity = new DocumentPdfEntity();
    entity.setId(1L);
    entity.setFilename("test.pdf");
    entity.setPdfContent("PDF content".getBytes());
    entity.setContentType("application/pdf");
    entity.setSize(1024L);
    entity.setUploadedAt(LocalDateTime.of(2023, 8, 1, 12, 0));
    entity.setOwner(owner);

    return entity;
  }
}
