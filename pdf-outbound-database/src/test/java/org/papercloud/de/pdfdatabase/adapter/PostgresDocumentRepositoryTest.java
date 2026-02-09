package org.papercloud.de.pdfdatabase.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.mapper.DocumentPersistenceMapper;
import org.papercloud.de.pdfdatabase.repository.DocumentJpaRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresDocumentRepositoryTest {

    @Mock
    private DocumentJpaRepository jpaRepository;

    @Mock
    private DocumentPersistenceMapper mapper;

    @InjectMocks
    private PostgresDocumentRepository repository;

    @Test
    void save_shouldMapToEntitySaveAndMapBack() {
        Document domain = Document.builder().id(1L).filename("test.pdf").build();
        DocumentPdfEntity entity = DocumentPdfEntity.builder().id(1L).filename("test.pdf").build();
        DocumentPdfEntity savedEntity = DocumentPdfEntity.builder().id(1L).filename("test.pdf").build();
        Document savedDomain = Document.builder().id(1L).filename("test.pdf").build();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedDomain);

        Document result = repository.save(domain);

        assertSame(savedDomain, result);
        verify(mapper).toEntity(domain);
        verify(jpaRepository).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void findById_found_shouldReturnMappedDomain() {
        DocumentPdfEntity entity = DocumentPdfEntity.builder().id(1L).build();
        Document domain = Document.builder().id(1L).build();

        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Document> result = repository.findById(1L);

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
    }

    @Test
    void findById_notFound_shouldReturnEmpty() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Document> result = repository.findById(99L);

        assertTrue(result.isEmpty());
        verify(mapper, never()).toDomain(any());
    }

    @Test
    void findByOwnerUsername_shouldMapEntityListToDomainList() {
        List<DocumentPdfEntity> entities = List.of(
                DocumentPdfEntity.builder().id(1L).build(),
                DocumentPdfEntity.builder().id(2L).build()
        );
        List<Document> domains = List.of(
                Document.builder().id(1L).build(),
                Document.builder().id(2L).build()
        );

        when(jpaRepository.findByOwnerUsername("john")).thenReturn(entities);
        when(mapper.toDomainList(entities)).thenReturn(domains);

        List<Document> result = repository.findByOwnerUsername("john");

        assertEquals(2, result.size());
        assertSame(domains, result);
    }

    @Test
    void deleteById_shouldDelegateToJpaRepository() {
        repository.deleteById(5L);

        verify(jpaRepository).deleteById(5L);
    }

    @Test
    void existsByFilenameAndOwnerUsername_true() {
        when(jpaRepository.existsByFilenameAndOwnerUsername("test.pdf", "john")).thenReturn(true);

        assertTrue(repository.existsByFilenameAndOwnerUsername("test.pdf", "john"));
    }

    @Test
    void existsByFilenameAndOwnerUsername_false() {
        when(jpaRepository.existsByFilenameAndOwnerUsername("test.pdf", "john")).thenReturn(false);

        assertFalse(repository.existsByFilenameAndOwnerUsername("test.pdf", "john"));
    }
}
