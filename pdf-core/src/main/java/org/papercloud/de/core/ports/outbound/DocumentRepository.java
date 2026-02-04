package org.papercloud.de.core.ports.outbound;

import org.papercloud.de.core.domain.Document;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for document persistence.
 * Implementations may use JPA, MongoDB, or any other storage mechanism.
 */
public interface DocumentRepository {

    Document save(Document document);

    Optional<Document> findById(Long id);

    List<Document> findByOwnerUsername(String username);

    void deleteById(Long id);

    boolean existsByFilenameAndOwnerUsername(String filename, String username);
}
