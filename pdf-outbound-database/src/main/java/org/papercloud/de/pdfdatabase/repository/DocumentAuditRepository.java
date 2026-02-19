package org.papercloud.de.pdfdatabase.repository;

import org.papercloud.de.pdfdatabase.entity.DocumentAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentAuditRepository extends JpaRepository<DocumentAuditEntity, Long> {

    List<DocumentAuditEntity> findByDocumentIdOrderByOccurredAtDesc(Long documentId);
}
