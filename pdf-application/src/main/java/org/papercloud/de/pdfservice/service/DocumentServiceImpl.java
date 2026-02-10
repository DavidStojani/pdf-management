package org.papercloud.de.pdfservice.service;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.core.dto.document.DocumentUploadDTO;
import org.papercloud.de.core.dto.document.DocumentListItemDTO;
import org.papercloud.de.core.events.OcrEvent;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.errors.DocumentUploadException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.papercloud.de.pdfservice.errors.UserAuthenticationException;
import org.papercloud.de.pdfservice.mapper.DocumentServiceMapper;
import org.papercloud.de.pdfservice.processor.DocumentOcrProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentServiceMapper documentMapper;
    private final ApplicationEventPublisher publisher;

    @Override
    public DocumentDTO processUpload(MultipartFile file, Authentication authentication) {
        Authentication resolvedAuth = resolveAuthentication(authentication);
        validateAuthentication(resolvedAuth);
        validateFile(file);
        validatePdfContentType(file);

        DocumentUploadDTO uploadDTO = buildUploadDto(file);
        return processDocumentSafely(uploadDTO, resolvedAuth.getName());
    }

    @Override
    public DocumentDTO processDocument(DocumentUploadDTO uploadDTO, String username) throws IOException {
        DocumentDTO documentDTO = saveDocToDB(username, uploadDTO);
        publisher.publishEvent(new OcrEvent(documentDTO.getId()));
        return documentDTO;
    }


    @Transactional
    protected DocumentDTO saveDocToDB(String username, DocumentUploadDTO uploadDTO) {
        UserEntity user = findUserOrThrow(username);

        DocumentPdfEntity documentPdfEntity = DocumentPdfEntity.builder()
                .filename(uploadDTO.getFileName())
                .contentType(uploadDTO.getContentType())
                .pdfContent(uploadDTO.getInputPdfBytes())
                .size(uploadDTO.getSize())
                .owner(user)
                .uploadedAt(LocalDateTime.now())
                .status(Document.Status.UPLOADED)
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

    @Override
    public List<DocumentListItemDTO> searchDocuments(String username, String query) {
        List<DocumentPdfEntity> documents = documentRepository.findByOwnerUsername(username);
        if (query == null || query.isBlank()) {
            return mapToListItems(documents);
        }
        String q = query.toLowerCase(Locale.ROOT);
        List<DocumentPdfEntity> filtered = documents.stream()
                .filter(doc -> {
                    String title = doc.getTitle();
                    String filename = doc.getFilename();
                    return (title != null && title.toLowerCase(Locale.ROOT).contains(q))
                            || (filename != null && filename.toLowerCase(Locale.ROOT).contains(q));
                })
                .collect(Collectors.toList());
        return mapToListItems(filtered);
    }

    @Override
    public List<DocumentListItemDTO> getFavourites(String username) {
        return List.of();
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

    private List<DocumentListItemDTO> mapToListItems(List<DocumentPdfEntity> documents) {
        return documents.stream()
                .map(doc -> DocumentListItemDTO.builder()
                        .id(doc.getId())
                        .title(doc.getTitle() != null ? doc.getTitle() : doc.getFilename())
                        .isFavourite(false)
                        .build())
                .collect(Collectors.toList());
    }

    private Authentication resolveAuthentication(Authentication authentication) {
        if (authentication != null) {
            return authentication;
        }
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private void validateAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserAuthenticationException("User must be authenticated to upload documents.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("Uploaded file must not be empty.");
        }
    }

    private void validatePdfContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (!MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(contentType)) {
            throw new InvalidDocumentException("Invalid file format. Only PDF files are allowed.");
        }
    }

}
