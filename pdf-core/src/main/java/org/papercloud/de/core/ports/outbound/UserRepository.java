package org.papercloud.de.core.ports.outbound;

import org.papercloud.de.core.domain.User;

import java.util.Optional;

/**
 * Port interface for user persistence.
 * Implementations may use JPA, MongoDB, or any other storage mechanism.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
