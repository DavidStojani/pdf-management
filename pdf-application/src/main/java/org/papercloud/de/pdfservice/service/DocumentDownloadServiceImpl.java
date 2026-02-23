package org.papercloud.de.pdfservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.domain.AuditActionType;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.mapper.DocumentServiceMapper;
import org.papercloud.de.pdfservice.util.ClientInfoExtractor;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentDownloadServiceImpl implements DocumentDownloadService {

    private final DocumentRepository documentRepository;
    private final DocumentServiceMapper documentMapper;
    private final AuditService auditService;
    private final ClientInfoExtractor clientInfoExtractor;

    @Override
    public DocumentDownloadDTO downloadDocument(String username, Long id) throws AccessDeniedException {
        DocumentPdfEntity document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));

        if (!document.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("You are not allowed to access this document.");
        }

        recordAuditSafely(id, username, AuditActionType.DOWNLOADED,
                clientInfoExtractor.getClientIp(), clientInfoExtractor.getClientUserAgent(), null);

        return documentMapper.toDownloadDTO(document);
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
