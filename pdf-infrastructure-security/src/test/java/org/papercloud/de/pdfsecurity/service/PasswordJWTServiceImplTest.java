package org.papercloud.de.pdfsecurity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.pdfdatabase.entity.PasswordResetTokenEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PasswordJWTServiceImpl.
 * Tests password reset initiation and password reset completion workflows.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordJWTServiceImpl")
class PasswordJWTServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<UserEntity> userCaptor;

    private PasswordJWTServiceImpl passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordJWTServiceImpl(
                userRepository,
                tokenService,
                emailService,
                passwordEncoder
        );
    }

    @Nested
    @DisplayName("initiatePasswordReset")
    class InitiatePasswordResetTests {

        @Test
        @DisplayName("should create token and send email when user exists")
        void should_sendEmail_when_userExists() {
            // Arrange
            String email = "user@example.com";
            UserEntity user = createUser("testuser", email);
            String resetToken = "reset-token-123";

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(tokenService.createPasswordResetToken(user)).thenReturn(resetToken);

            // Act
            passwordService.initiatePasswordReset(email);

            // Assert
            verify(tokenService).createPasswordResetToken(user);
            verify(emailService).sendPasswordResetEmail(email, resetToken);
        }

        @Test
        @DisplayName("should throw exception when user does not exist")
        void should_throwException_when_userNotFound() {
            // Arrange
            String email = "nonexistent@example.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> passwordService.initiatePasswordReset(email))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User with this email does not exist");

            verify(tokenService, never()).createPasswordResetToken(any());
            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("should use correct user when multiple users exist")
        void should_useCorrectUser_when_multipleUsers() {
            // Arrange
            String email = "user@example.com";
            UserEntity user = createUser("specificuser", email);
            String resetToken = "reset-token-456";

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(tokenService.createPasswordResetToken(user)).thenReturn(resetToken);

            // Act
            passwordService.initiatePasswordReset(email);

            // Assert
            verify(tokenService).createPasswordResetToken(user);
            assertThat(user.getUsername()).isEqualTo("specificuser");
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("should reset password when all conditions valid")
        void should_resetPassword_when_validRequest() {
            // Arrange
            String token = "valid-token";
            String newPassword = "newPassword123";
            String confirmPassword = "newPassword123";
            String encodedPassword = "encoded-newPassword123";

            UserEntity user = createUser("testuser", "test@example.com");
            PasswordResetTokenEntity resetToken = createPasswordResetToken(token, user, LocalDateTime.now().plusHours(1));

            when(tokenService.getPasswordResetToken(token)).thenReturn(resetToken);
            when(tokenService.isTokenValid(resetToken)).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

            // Act
            passwordService.resetPassword(token, newPassword, confirmPassword);

            // Assert
            verify(userRepository).save(userCaptor.capture());
            UserEntity savedUser = userCaptor.getValue();
            assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
            verify(tokenService).deletePasswordResetToken(resetToken);
        }

        @Test
        @DisplayName("should throw exception when passwords do not match")
        void should_throwException_when_passwordMismatch() {
            // Arrange
            String token = "valid-token";
            String newPassword = "password123";
            String confirmPassword = "differentPassword123";

            // Act & Assert
            assertThatThrownBy(() -> passwordService.resetPassword(token, newPassword, confirmPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Passwords don't match");

            verify(tokenService, never()).getPasswordResetToken(anyString());
            verify(userRepository, never()).save(any());
            verify(tokenService, never()).deletePasswordResetToken(any());
        }

        @Test
        @DisplayName("should throw exception when token is expired")
        void should_throwException_when_tokenExpired() {
            // Arrange
            String token = "expired-token";
            String newPassword = "newPassword123";
            String confirmPassword = "newPassword123";

            UserEntity user = createUser("testuser", "test@example.com");
            PasswordResetTokenEntity resetToken = createPasswordResetToken(token, user, LocalDateTime.now().minusHours(1));

            when(tokenService.getPasswordResetToken(token)).thenReturn(resetToken);
            when(tokenService.isTokenValid(resetToken)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> passwordService.resetPassword(token, newPassword, confirmPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Token has expired");

            verify(userRepository, never()).save(any());
            verify(tokenService, never()).deletePasswordResetToken(any());
        }

        @Test
        @DisplayName("should throw exception when token does not exist")
        void should_throwException_when_tokenNotFound() {
            // Arrange
            String token = "non-existent-token";
            String newPassword = "newPassword123";
            String confirmPassword = "newPassword123";

            when(tokenService.getPasswordResetToken(token))
                    .thenThrow(new IllegalArgumentException("Invalid password reset token"));

            // Act & Assert
            assertThatThrownBy(() -> passwordService.resetPassword(token, newPassword, confirmPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid password reset token");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should encode password before saving")
        void should_encodePassword_when_resetting() {
            // Arrange
            String token = "valid-token";
            String newPassword = "plainPassword";
            String confirmPassword = "plainPassword";
            String encodedPassword = "encoded-plainPassword";

            UserEntity user = createUser("testuser", "test@example.com");
            user.setPassword("old-password");
            PasswordResetTokenEntity resetToken = createPasswordResetToken(token, user, LocalDateTime.now().plusHours(1));

            when(tokenService.getPasswordResetToken(token)).thenReturn(resetToken);
            when(tokenService.isTokenValid(resetToken)).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

            // Act
            passwordService.resetPassword(token, newPassword, confirmPassword);

            // Assert
            verify(passwordEncoder).encode(newPassword);
            verify(userRepository).save(userCaptor.capture());
            UserEntity savedUser = userCaptor.getValue();
            assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
            assertThat(savedUser.getPassword()).isNotEqualTo(newPassword);
        }

        @Test
        @DisplayName("should delete token after successful password reset")
        void should_deleteToken_when_resetSuccessful() {
            // Arrange
            String token = "valid-token";
            String newPassword = "newPassword123";
            String confirmPassword = "newPassword123";

            UserEntity user = createUser("testuser", "test@example.com");
            PasswordResetTokenEntity resetToken = createPasswordResetToken(token, user, LocalDateTime.now().plusHours(1));

            when(tokenService.getPasswordResetToken(token)).thenReturn(resetToken);
            when(tokenService.isTokenValid(resetToken)).thenReturn(true);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

            // Act
            passwordService.resetPassword(token, newPassword, confirmPassword);

            // Assert
            verify(tokenService).deletePasswordResetToken(resetToken);
        }

        @Test
        @DisplayName("should preserve user data except password")
        void should_preserveUserData_when_resetting() {
            // Arrange
            String token = "valid-token";
            String newPassword = "newPassword123";
            String confirmPassword = "newPassword123";

            UserEntity user = createUser("originaluser", "original@example.com");
            user.setId(100L);
            user.setEnabled(true);
            PasswordResetTokenEntity resetToken = createPasswordResetToken(token, user, LocalDateTime.now().plusHours(1));

            when(tokenService.getPasswordResetToken(token)).thenReturn(resetToken);
            when(tokenService.isTokenValid(resetToken)).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn("encoded-password");

            // Act
            passwordService.resetPassword(token, newPassword, confirmPassword);

            // Assert
            verify(userRepository).save(userCaptor.capture());
            UserEntity savedUser = userCaptor.getValue();
            assertThat(savedUser.getId()).isEqualTo(100L);
            assertThat(savedUser.getUsername()).isEqualTo("originaluser");
            assertThat(savedUser.getEmail()).isEqualTo("original@example.com");
            assertThat(savedUser.isEnabled()).isTrue();
        }
    }

    // Helper methods

    private UserEntity createUser(String username, String email) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("old-encoded-password");
        user.setEnabled(true);
        return user;
    }

    private PasswordResetTokenEntity createPasswordResetToken(String token, UserEntity user, LocalDateTime expiryDate) {
        PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(expiryDate);
        return resetToken;
    }
}
