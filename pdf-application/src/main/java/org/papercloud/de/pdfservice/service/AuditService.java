package org.papercloud.de.pdfservice.service;

import org.papercloud.de.core.domain.AuditActionType;
import org.papercloud.de.core.dto.audit.AuditEntryDTO;

import java.util.List;

public interface AuditService {

    void recordAction(Long documentId, String username, AuditActionType action,
                      String ipAddress, String userAgent, String additionalInfo);

    List<AuditEntryDTO> getAuditLog(Long documentId, String requestingUsername);
}
