package org.papercloud.de.pdfdatabase.entity;

import jakarta.persistence.*;
import lombok.*;
import org.papercloud.de.core.domain.AuditActionType;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "document_audit_log",
        indexes = {
                @Index(name = "idx_audit_document_id", columnList = "document_id"),
                @Index(name = "idx_audit_document_occurred", columnList = "document_id, occurred_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private AuditActionType actionType;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;
}
