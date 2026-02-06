package org.papercloud.de.pdfdatabase.adapter;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.core.domain.User;
import org.papercloud.de.core.ports.outbound.UserRepository;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.mapper.DocumentPersistenceMapper;
import org.papercloud.de.pdfdatabase.repository.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PostgreSQL/JPA implementation of the UserRepository port.
 * This adapter translates between domain objects and JPA entities.
 */
@Repository
@RequiredArgsConstructor
public class PostgresUserRepository implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final DocumentPersistenceMapper mapper;

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toUserEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return mapper.toUserDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toUserDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(mapper::toUserDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username)
                .map(mapper::toUserDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }
}
