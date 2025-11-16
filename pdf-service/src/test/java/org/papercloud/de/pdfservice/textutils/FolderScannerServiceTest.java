package org.papercloud.de.pdfservice.textutils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.common.dto.document.DocumentUploadDTO;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.pdfservice.search.DocumentService;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FolderScannerServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private FolderScannerService folderScannerService;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("scanUserFolder should process new PDFs and persist folder path")
    void scanUserFolder_shouldProcessNewFiles() throws IOException {
        Path pdfFile = tempDir.resolve("sample.pdf");
        Files.write(pdfFile, "dummy".getBytes());

        UserEntity user = new UserEntity();
        user.setUsername("alice");
        given(userRepository.findByUsername("alice")).willReturn(java.util.Optional.of(user));
        given(documentRepository.existsByFilenameAndOwnerUsername("sample.pdf", "alice")).willReturn(false);

        folderScannerService.scanUserFolder("alice", tempDir.toString());

        verify(userRepository).save(user);

        ArgumentCaptor<DocumentUploadDTO> uploadCaptor = ArgumentCaptor.forClass(DocumentUploadDTO.class);
        verify(documentService).processDocument(uploadCaptor.capture(), eq("alice"));

        DocumentUploadDTO upload = uploadCaptor.getValue();
        assertThat(upload.getFileName()).isEqualTo("sample.pdf");
        assertThat(upload.getContentType()).isEqualTo("application/pdf");
        assertThat(upload.getInputPdfBytes()).isEqualTo("dummy".getBytes());
    }

    @Test
    @DisplayName("scanUserFolder should fail fast on invalid folder")
    void scanUserFolder_shouldRejectInvalidFolder() {
        given(userRepository.findByUsername("bob")).willReturn(java.util.Optional.of(new UserEntity()));

        assertThrows(RuntimeException.class, () -> folderScannerService.scanUserFolder("bob", tempDir.resolve("missing").toString()));

        verifyNoInteractions(documentService);
    }
}
