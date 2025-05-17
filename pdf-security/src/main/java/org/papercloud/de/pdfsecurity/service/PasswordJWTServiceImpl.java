package org.papercloud.de.pdfsecurity.service;

import jakarta.transaction.Transactional;
import org.papercloud.de.pdfdatabase.entity.PasswordResetTokenEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.UserRepository;


import org.papercloud.de.pdfservice.auth.PasswordService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordJWTServiceImpl implements PasswordService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordJWTServiceImpl(
            UserRepository userRepository,
            TokenService tokenService,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void initiatePasswordReset(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with this email does not exist"));

        String token = tokenService.createPasswordResetToken(user);

        emailService.sendPasswordResetEmail(email, token);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords don't match");
        }

        PasswordResetTokenEntity resetToken = tokenService.getPasswordResetToken(token);

        if (!tokenService.isTokenValid(resetToken)) {
            throw new IllegalArgumentException("Token has expired");
        }

        UserEntity user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenService.deletePasswordResetToken(resetToken);
    }
}