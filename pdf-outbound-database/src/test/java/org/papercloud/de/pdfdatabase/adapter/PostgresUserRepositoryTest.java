package org.papercloud.de.pdfdatabase.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.domain.User;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.mapper.DocumentPersistenceMapper;
import org.papercloud.de.pdfdatabase.repository.UserJpaRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresUserRepositoryTest {

    @Mock
    private UserJpaRepository jpaRepository;

    @Mock
    private DocumentPersistenceMapper mapper;

    @InjectMocks
    private PostgresUserRepository repository;

    @Test
    void save_shouldMapToEntitySaveAndMapBack() {
        User domain = User.builder().id(1L).username("john").build();
        UserEntity entity = UserEntity.builder().id(1L).username("john").build();
        UserEntity savedEntity = UserEntity.builder().id(1L).username("john").build();
        User savedDomain = User.builder().id(1L).username("john").build();

        when(mapper.toUserEntity(domain)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toUserDomain(savedEntity)).thenReturn(savedDomain);

        User result = repository.save(domain);

        assertSame(savedDomain, result);
        verify(mapper).toUserEntity(domain);
        verify(jpaRepository).save(entity);
        verify(mapper).toUserDomain(savedEntity);
    }

    @Test
    void findById_found_shouldReturnMappedDomain() {
        UserEntity entity = UserEntity.builder().id(1L).build();
        User domain = User.builder().id(1L).build();

        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toUserDomain(entity)).thenReturn(domain);

        Optional<User> result = repository.findById(1L);

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
    }

    @Test
    void findById_notFound_shouldReturnEmpty() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = repository.findById(99L);

        assertTrue(result.isEmpty());
        verify(mapper, never()).toUserDomain(any());
    }

    @Test
    void findByEmail_found() {
        UserEntity entity = UserEntity.builder().id(1L).email("john@example.com").build();
        User domain = User.builder().id(1L).email("john@example.com").build();

        when(jpaRepository.findByEmail("john@example.com")).thenReturn(Optional.of(entity));
        when(mapper.toUserDomain(entity)).thenReturn(domain);

        Optional<User> result = repository.findByEmail("john@example.com");

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
    }

    @Test
    void findByEmail_notFound() {
        when(jpaRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        Optional<User> result = repository.findByEmail("nobody@example.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByUsername_found() {
        UserEntity entity = UserEntity.builder().id(1L).username("john").build();
        User domain = User.builder().id(1L).username("john").build();

        when(jpaRepository.findByUsername("john")).thenReturn(Optional.of(entity));
        when(mapper.toUserDomain(entity)).thenReturn(domain);

        Optional<User> result = repository.findByUsername("john");

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
    }

    @Test
    void findByUsername_notFound() {
        when(jpaRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        Optional<User> result = repository.findByUsername("nobody");

        assertTrue(result.isEmpty());
    }

    @Test
    void existsByEmail_true() {
        when(jpaRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertTrue(repository.existsByEmail("john@example.com"));
    }

    @Test
    void existsByEmail_false() {
        when(jpaRepository.existsByEmail("nobody@example.com")).thenReturn(false);

        assertFalse(repository.existsByEmail("nobody@example.com"));
    }

    @Test
    void existsByUsername_true() {
        when(jpaRepository.existsByUsername("john")).thenReturn(true);

        assertTrue(repository.existsByUsername("john"));
    }

    @Test
    void existsByUsername_false() {
        when(jpaRepository.existsByUsername("nobody")).thenReturn(false);

        assertFalse(repository.existsByUsername("nobody"));
    }
}
