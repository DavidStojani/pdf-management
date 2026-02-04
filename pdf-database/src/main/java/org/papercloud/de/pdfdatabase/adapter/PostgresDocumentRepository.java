package org.papercloud.de.pdfdatabase.adapter;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.ports.outbound.DocumentRepository;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.mapper.DocumentPersistenceMapper;
import org.papercloud.de.pdfdatabase.repository.DocumentJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL/JPA implementation of the DocumentRepository port.
 * This adapter translates between domain objects and JPA entities.
 */
@Repository
@RequiredArgsConstructor
public class PostgresDocumentRepository implements DocumentRepository {

    private final DocumentJpaRepository jpaRepository;
    private final DocumentPersistenceMapper mapper;

    @Override
    public Document save(Document document) {
        DocumentPdfEntity entity = mapper.toEntity(document);
        DocumentPdfEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Document> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Document> findByOwnerUsername(String username) {
        return mapper.toDomainList(
                jpaRepository.findByOwnerUsername(username)
        );
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByFilenameAndOwnerUsername(String filename, String username) {
        return jpaRepository.existsByFilenameAndOwnerUsername(filename, username);
    }
}
