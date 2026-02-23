package org.papercloud.de.pdfservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.domain.AuditActionType;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.domain.UploadSource;
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentUploadDTO;
import org.papercloud.de.core.events.OcrEvent;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.pdfservice.errors.DocumentUploadException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.papercloud.de.pdfservice.errors.UserAuthenticationException;
import org.papercloud.de.pdfservice.mapper.DocumentServiceMapper;
import org.papercloud.de.pdfservice.util.ClientInfoExtractor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUploadServiceImpl implements DocumentUploadService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentServiceMapper documentMapper;
    private final ApplicationEventPublisher publisher;
    private final AuditService auditService;
    private final ClientInfoExtractor clientInfoExtractor;

    @Override
    public DocumentDTO processUpload(MultipartFile file, Authentication authentication, UploadSource source) {
        Authentication resolvedAuth = resolveAuthentication(authentication);
        validateAuthentication(resolvedAuth);
        validateFile(file);
        validatePdfContentType(file);

        DocumentUploadDTO uploadDTO = buildUploadDto(file, source);
        try {
            return processDocument(uploadDTO, resolvedAuth.getName());
        } catch (IOException e) {
            throw new DocumentUploadException("Failed to process uploaded document.", e);
        }
    }

    @Override
    @Transactional
    public DocumentDTO processDocument(DocumentUploadDTO uploadDTO, String username) throws IOException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

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

        String source = uploadDTO.getUploadSource() != null
                ? uploadDTO.getUploadSource().name()
                : UploadSource.FILE_UPLOAD.name();
        recordAuditSafely(documentPdfEntity.getId(), username, AuditActionType.UPLOADED,
                clientInfoExtractor.getClientIp(), clientInfoExtractor.getClientUserAgent(),
                "UPLOAD_SOURCE:" + source);

        DocumentDTO documentDTO = documentMapper.toDocumentDTO(documentPdfEntity);
        publisher.publishEvent(new OcrEvent(documentDTO.getId()));
        return documentDTO;
    }

    private DocumentUploadDTO buildUploadDto(MultipartFile file, UploadSource uploadSource) {
        try {
            return DocumentUploadDTO.builder()
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .inputPdfBytes(file.getBytes())
                    .uploadSource(uploadSource)
                    .build();
        } catch (IOException e) {
            throw new DocumentUploadException("Failed to read uploaded file.", e);
        }
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

    private void recordAuditSafely(Long documentId, String username, AuditActionType action,
                                   String ipAddress, String userAgent, String additionalInfo) {
        try {
            auditService.recordAction(documentId, username, action, ipAddress, userAgent, additionalInfo);
        } catch (Exception e) {
            log.warn("Failed to record audit event {} for document {}: {}", action, documentId, e.getMessage());
        }
    }
}
