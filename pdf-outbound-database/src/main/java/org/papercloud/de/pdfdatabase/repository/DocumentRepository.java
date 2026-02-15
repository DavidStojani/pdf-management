package org.papercloud.de.pdfdatabase.repository;

import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface DocumentRepository extends JpaRepository<DocumentPdfEntity, Long> {

    boolean existsByFilenameAndOwnerUsername(String filename, String username);

    java.util.List<DocumentPdfEntity> findByOwnerUsername(String username);

    @Query("""
            select d from DocumentPdfEntity d
            where d.status = org.papercloud.de.core.domain.Document.Status.OCR_ERROR
              and d.ocrRetryCount < :maxAttempts
              and (d.ocrNextRetryAt is null or d.ocrNextRetryAt <= :now)
            order by d.uploadedAt asc
            """)
    List<DocumentPdfEntity> findRetryableOcrDocuments(
            @Param("now") LocalDateTime now,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable
    );

    @Query("""
            select d from DocumentPdfEntity d
            where d.status = org.papercloud.de.core.domain.Document.Status.ENRICHMENT_ERROR
              and d.enrichmentRetryCount < :maxAttempts
              and (d.enrichmentNextRetryAt is null or d.enrichmentNextRetryAt <= :now)
            order by d.uploadedAt asc
            """)
    List<DocumentPdfEntity> findRetryableEnrichmentDocuments(
            @Param("now") LocalDateTime now,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable
    );

    @Query("""
            select d from DocumentPdfEntity d
            where d.status = org.papercloud.de.core.domain.Document.Status.INDEXING_ERROR
              and d.indexingRetryCount < :maxAttempts
              and (d.indexingNextRetryAt is null or d.indexingNextRetryAt <= :now)
            order by d.uploadedAt asc
            """)
    List<DocumentPdfEntity> findRetryableIndexingDocuments(
            @Param("now") LocalDateTime now,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable
    );
}
