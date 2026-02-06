package org.papercloud.de.pdfsecurity.service;

import org.papercloud.de.pdfdatabase.entity.PasswordResetTokenEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.entity.VerificationTokenEntity;
import org.papercloud.de.pdfdatabase.repository.PasswordResetTokenRepository;
import org.papercloud.de.pdfdatabase.repository.VerificationTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TokenService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final int EXPIRATION_HOURS = 24;

    public TokenService(
            VerificationTokenRepository verificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public String createVerificationToken(UserEntity user) {
        String token = UUID.randomUUID().toString();

        VerificationTokenEntity verificationToken = new VerificationTokenEntity();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(EXPIRATION_HOURS));

        verificationTokenRepository.save(verificationToken);

        return token;
    }

    public String createPasswordResetToken(UserEntity user) {
        String token = UUID.randomUUID().toString();

        PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(EXPIRATION_HOURS));

        passwordResetTokenRepository.save(resetToken);

        return token;
    }

    public VerificationTokenEntity getVerificationToken(String token) {
        return verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));
    }

    public PasswordResetTokenEntity getPasswordResetToken(String token) {
        return passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));
    }

    public boolean isTokenValid(VerificationTokenEntity token) {
        return token.getExpiryDate().isAfter(LocalDateTime.now());
    }

    public boolean isTokenValid(PasswordResetTokenEntity token) {
        return token.getExpiryDate().isAfter(LocalDateTime.now());
    }

    public void deleteVerificationToken(VerificationTokenEntity token) {
        verificationTokenRepository.delete(token);
    }

    public void deletePasswordResetToken(PasswordResetTokenEntity token) {
        passwordResetTokenRepository.delete(token);
    }
}