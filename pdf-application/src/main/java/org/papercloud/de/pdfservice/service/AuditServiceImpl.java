package org.papercloud.de.pdfservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.domain.AuditActionType;
import org.papercloud.de.core.dto.audit.AuditEntryDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentAuditEntity;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentAuditRepository;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final DocumentAuditRepository auditRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    @Override
    @Transactional
    public void recordAction(Long documentId, String username, AuditActionType action,
                             String ipAddress, String userAgent, String additionalInfo) {
        Long userId = null;
        if (username != null) {
            Optional<UserEntity> user = userRepository.findByUsername(username);
            userId = user.map(UserEntity::getId).orElse(null);
        }

        String truncatedUserAgent = (userAgent != null && userAgent.length() > 512)
                ? userAgent.substring(0, 512)
                : userAgent;

        DocumentAuditEntity entry = DocumentAuditEntity.builder()
                .documentId(documentId)
                .userId(userId)
                .actionType(action)
                .occurredAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .userAgent(truncatedUserAgent)
                .additionalInfo(additionalInfo)
                .build();

        auditRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntryDTO> getAuditLog(Long documentId, String requestingUsername) {
        DocumentPdfEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + documentId));

        if (!document.getOwner().getUsername().equals(requestingUsername)) {
            throw new AccessDeniedException("You are not allowed to access the audit log for this document.");
        }

        List<DocumentAuditEntity> entries = auditRepository.findByDocumentIdOrderByOccurredAtDesc(documentId);

        List<Long> userIds = entries.stream()
                .map(DocumentAuditEntity::getUserId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Long, String> usernameById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getUsername));

        return entries.stream()
                .map(e -> AuditEntryDTO.builder()
                        .id(e.getId())
                        .documentId(e.getDocumentId())
                        .userId(e.getUserId())
                        .username(e.getUserId() != null ? usernameById.get(e.getUserId()) : null)
                        .actionType(e.getActionType())
                        .occurredAt(e.getOccurredAt())
                        .ipAddress(e.getIpAddress())
                        .additionalInfo(e.getAdditionalInfo())
                        .build())
                .toList();
    }
}
