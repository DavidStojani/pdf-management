package org.papercloud.de.pdfdatabase.repository;

import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
   Optional<UserEntity> findByEmail(String email);
   Optional<UserEntity> findByUsername(String username);
}
