package org.papercloud.de.pdfdatabase.repository;

import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for DocumentPdfEntity.
 * This is the low-level JPA interface, used internally by the adapter.
 */
public interface DocumentJpaRepository extends JpaRepository<DocumentPdfEntity, Long> {

    boolean existsByFilenameAndOwnerUsername(String filename, String username);

    List<DocumentPdfEntity> findByOwnerUsername(String username);
}
