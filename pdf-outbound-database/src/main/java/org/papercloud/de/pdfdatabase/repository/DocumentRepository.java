package org.papercloud.de.pdfdatabase.repository;

import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface DocumentRepository extends JpaRepository<DocumentPdfEntity, Long> {

    boolean existsByFilenameAndOwnerUsername(String filename, String username);
}
