package org.papercloud.de.pdfsecurity.service;

import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.entity.VerificationTokenEntity;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
// VerificationService is implemented locally, not a core port
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerificationJWTService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final EmailService emailService;

    public VerificationJWTService(
            UserRepository userRepository,
            TokenService tokenService,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationTokenEntity verificationToken = tokenService.getVerificationToken(token);

        if (!tokenService.isTokenValid(verificationToken)) {
            throw new IllegalArgumentException("Token has expired");
        }

        UserEntity user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        tokenService.deleteVerificationToken(verificationToken);
    }

    public void resendVerificationEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with this email does not exist"));

        if (user.isEnabled()) {
            throw new IllegalArgumentException("Email is already verified");
        }

        String token = tokenService.createVerificationToken(user);

        emailService.sendVerificationEmail(email, token);
    }
}