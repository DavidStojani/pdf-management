package org.papercloud.de.pdfservice.search;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.common.dto.document.DocumentDTO;
import org.papercloud.de.common.dto.document.DocumentDownloadDTO;
import org.papercloud.de.common.dto.document.DocumentMapper;
import org.papercloud.de.common.dto.document.DocumentUploadDTO;
import org.papercloud.de.common.events.OcrEvent;
import org.papercloud.de.common.util.PdfTextExtractorService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.errors.DocumentUploadException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.papercloud.de.pdfservice.errors.UserAuthenticationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PageRepository pageRepository;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final DocumentMapper documentMapper;
    private final ApplicationEventPublisher publisher;

    @Override
    public DocumentDTO processUpload(MultipartFile file, Authentication authentication) {
        Authentication resolvedAuth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();

        if (resolvedAuth == null || !resolvedAuth.isAuthenticated()) {
            throw new UserAuthenticationException("User must be authenticated to upload documents.");
        }

        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("Uploaded file must not be empty.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(contentType)) {
            throw new InvalidDocumentException("Invalid file format. Only PDF files are allowed.");
        }

        DocumentUploadDTO uploadDTO = buildUploadDto(file);
        return processDocumentSafely(uploadDTO, resolvedAuth.getName());
    }

    @Override
    public DocumentDTO processDocument(DocumentUploadDTO uploadDTO, String username) throws IOException {
        DocumentDTO documentDTO = saveDocToDB(username, uploadDTO);
        publisher.publishEvent(new OcrEvent(documentDTO.getId(),documentDTO.getPdfContent()));
        return documentDTO;
    }


    @Transactional
    private DocumentDTO saveDocToDB(String username, DocumentUploadDTO uploadDTO) {
        UserEntity user = findUserOrThrow(username);

        DocumentPdfEntity documentPdfEntity = DocumentPdfEntity.builder()
                .filename(uploadDTO.getFileName())
                .contentType(uploadDTO.getContentType())
                .pdfContent(uploadDTO.getInputPdfBytes())
                .size(uploadDTO.getSize())
                .owner(user)
                .uploadedAt(LocalDateTime.now())
                .build();

        documentRepository.save(documentPdfEntity);
        return documentMapper.toDocumentDTO(documentPdfEntity);

    }

    @Override
    public DocumentDownloadDTO downloadDocument(String username, Long id) throws AccessDeniedException {
        DocumentPdfEntity document = getDocumentOrThrow(id);

        if (!document.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("You are not allowed to access this document.");
        }

        return documentMapper.toDownloadDTO(document);
    }

    // 🔽 --- Private Helper Methods --- 🔽

    private DocumentUploadDTO buildUploadDto(MultipartFile file) {
        try {
            return DocumentUploadDTO.builder()
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .inputPdfBytes(file.getBytes())
                    .build();
        } catch (IOException e) {
            throw new DocumentUploadException("Failed to read uploaded file.", e);
        }
    }

    private DocumentDTO processDocumentSafely(DocumentUploadDTO uploadDTO, String username) {
        try {
            return processDocument(uploadDTO, username);
        } catch (IOException e) {
            throw new DocumentUploadException("Failed to process uploaded document.", e);
        }
    }

    private UserEntity findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private DocumentPdfEntity getDocumentOrThrow(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));
    }

}
