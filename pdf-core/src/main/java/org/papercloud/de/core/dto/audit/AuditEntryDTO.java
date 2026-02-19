package org.papercloud.de.core.dto.audit;

import lombok.Builder;
import lombok.Data;
import org.papercloud.de.core.domain.AuditActionType;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditEntryDTO {
    private Long id;
    private Long documentId;
    private Long userId;
    private String username;
    private AuditActionType actionType;
    private LocalDateTime occurredAt;
    private String ipAddress;
    private String additionalInfo;
}
