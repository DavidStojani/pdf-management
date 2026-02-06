package org.papercloud.de.pdfdatabase.repository;

import org.papercloud.de.pdfdatabase.entity.VerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity,Long> {
    Optional<VerificationTokenEntity> findByToken(String token);
    Optional<VerificationTokenEntity> findByUserId(Long userId);
}
