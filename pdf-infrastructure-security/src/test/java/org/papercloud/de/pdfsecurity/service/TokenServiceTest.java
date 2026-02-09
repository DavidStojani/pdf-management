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
import org.papercloud.de.pdfdatabase.entity.VerificationTokenEntity;
import org.papercloud.de.pdfdatabase.repository.PasswordResetTokenRepository;
import org.papercloud.de.pdfdatabase.repository.VerificationTokenRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenService.
 * Tests verification token and password reset token creation, retrieval, validation, and deletion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService")
class TokenServiceTest {

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Captor
    private ArgumentCaptor<VerificationTokenEntity> verificationTokenCaptor;

    @Captor
    private ArgumentCaptor<PasswordResetTokenEntity> passwordResetTokenCaptor;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(verificationTokenRepository, passwordResetTokenRepository);
    }

    @Nested
    @DisplayName("createVerificationToken")
    class CreateVerificationTokenTests {

        @Test
        @DisplayName("should create and save verification token for user")
        void should_createToken_when_validUser() {
            // Arrange
            UserEntity user = createUser("testuser", "test@example.com");

            // Act
            String token = tokenService.createVerificationToken(user);

            // Assert
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            verify(verificationTokenRepository).save(verificationTokenCaptor.capture());

            VerificationTokenEntity savedToken = verificationTokenCaptor.getValue();
            assertThat(savedToken.getToken()).isEqualTo(token);
            assertThat(savedToken.getUser()).isEqualTo(user);
            assertThat(savedToken.getExpiryDate()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("should set expiry date 24 hours in future")
        void should_setCorrectExpiry_when_creatingToken() {
            // Arrange
            UserEntity user = createUser("testuser", "test@example.com");
            LocalDateTime beforeCreation = LocalDateTime.now();

            // Act
            tokenService.createVerificationToken(user);

            // Assert
            verify(verificationTokenRepository).save(verificationTokenCaptor.capture());
            VerificationTokenEntity savedToken = verificationTokenCaptor.getValue();

            LocalDateTime expectedExpiry = beforeCreation.plusHours(24);
            assertThat(savedToken.getExpiryDate()).isAfterOrEqualTo(expectedExpiry.minusSeconds(1));
            assertThat(savedToken.getExpiryDate()).isBeforeOrEqualTo(expectedExpiry.plusSeconds(1));
        }

        @Test
        @DisplayName("should generate unique tokens for multiple calls")
        void should_generateUniqueTokens_when_calledMultipleTimes() {
            // Arrange
            UserEntity user = createUser("testuser", "test@example.com");

            // Act
            String token1 = tokenService.createVerificationToken(user);
            String token2 = tokenService.createVerificationToken(user);

            // Assert
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("createPasswordResetToken")
    class CreatePasswordResetTokenTests {

        @Test
        @DisplayName("should create and save password reset token for user")
        void should_createToken_when_validUser() {
            // Arrange
            UserEntity user = createUser("testuser", "test@example.com");

            // Act
            String token = tokenService.createPasswordResetToken(user);

            // Assert
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            verify(passwordResetTokenRepository).save(passwordResetTokenCaptor.capture());

            PasswordResetTokenEntity savedToken = passwordResetTokenCaptor.getValue();
            assertThat(savedToken.getToken()).isEqualTo(token);
            assertThat(savedToken.getUser()).isEqualTo(user);
            assertThat(savedToken.getExpiryDate()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("should set expiry date 24 hours in future")
        void should_setCorrectExpiry_when_creatingToken() {
            // Arrange
            UserEntity user = createUser("testuser", "test@example.com");
            LocalDateTime beforeCreation = LocalDateTime.now();

            // Act
            tokenService.createPasswordResetToken(user);

            // Assert
            verify(passwordResetTokenRepository).save(passwordResetTokenCaptor.capture());
            PasswordResetTokenEntity savedToken = passwordResetTokenCaptor.getValue();

            LocalDateTime expectedExpiry = beforeCreation.plusHours(24);
            assertThat(savedToken.getExpiryDate()).isAfterOrEqualTo(expectedExpiry.minusSeconds(1));
            assertThat(savedToken.getExpiryDate()).isBeforeOrEqualTo(expectedExpiry.plusSeconds(1));
        }

        @Test
        @DisplayName("should generate unique tokens for multiple calls")
        void should_generateUniqueTokens_when_calledMultipleTimes() {
            // Arrange
            UserEntity user = createUser("testuser", "test@example.com");

            // Act
            String token1 = tokenService.createPasswordResetToken(user);
            String token2 = tokenService.createPasswordResetToken(user);

            // Assert
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("getVerificationToken")
    class GetVerificationTokenTests {

        @Test
        @DisplayName("should return token when found")
        void should_returnToken_when_tokenExists() {
            // Arrange
            String tokenString = "test-token";
            VerificationTokenEntity token = createVerificationToken(tokenString);
            when(verificationTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(token));

            // Act
            VerificationTokenEntity result = tokenService.getVerificationToken(tokenString);

            // Assert
            assertThat(result).isEqualTo(token);
        }

        @Test
        @DisplayName("should throw exception when token not found")
        void should_throwException_when_tokenNotFound() {
            // Arrange
            String tokenString = "non-existent-token";
            when(verificationTokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> tokenService.getVerificationToken(tokenString))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid verification token");
        }
    }

    @Nested
    @DisplayName("getPasswordResetToken")
    class GetPasswordResetTokenTests {

        @Test
        @DisplayName("should return token when found")
        void should_returnToken_when_tokenExists() {
            // Arrange
            String tokenString = "reset-token";
            PasswordResetTokenEntity token = createPasswordResetToken(tokenString);
            when(passwordResetTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(token));

            // Act
            PasswordResetTokenEntity result = tokenService.getPasswordResetToken(tokenString);

            // Assert
            assertThat(result).isEqualTo(token);
        }

        @Test
        @DisplayName("should throw exception when token not found")
        void should_throwException_when_tokenNotFound() {
            // Arrange
            String tokenString = "non-existent-token";
            when(passwordResetTokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> tokenService.getPasswordResetToken(tokenString))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid password reset token");
        }
    }

    @Nested
    @DisplayName("isTokenValid - VerificationToken")
    class IsVerificationTokenValidTests {

        @Test
        @DisplayName("should return true when token not expired")
        void should_returnTrue_when_tokenNotExpired() {
            // Arrange
            VerificationTokenEntity token = createVerificationToken("test-token");
            token.setExpiryDate(LocalDateTime.now().plusHours(1));

            // Act
            boolean isValid = tokenService.isTokenValid(token);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("should return false when token expired")
        void should_returnFalse_when_tokenExpired() {
            // Arrange
            VerificationTokenEntity token = createVerificationToken("test-token");
            token.setExpiryDate(LocalDateTime.now().minusHours(1));

            // Act
            boolean isValid = tokenService.isTokenValid(token);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should return false when token just expired")
        void should_returnFalse_when_tokenJustExpired() {
            // Arrange
            VerificationTokenEntity token = createVerificationToken("test-token");
            token.setExpiryDate(LocalDateTime.now().minusSeconds(1));

            // Act
            boolean isValid = tokenService.isTokenValid(token);

            // Assert
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("isTokenValid - PasswordResetToken")
    class IsPasswordResetTokenValidTests {

        @Test
        @DisplayName("should return true when token not expired")
        void should_returnTrue_when_tokenNotExpired() {
            // Arrange
            PasswordResetTokenEntity token = createPasswordResetToken("reset-token");
            token.setExpiryDate(LocalDateTime.now().plusHours(1));

            // Act
            boolean isValid = tokenService.isTokenValid(token);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("should return false when token expired")
        void should_returnFalse_when_tokenExpired() {
            // Arrange
            PasswordResetTokenEntity token = createPasswordResetToken("reset-token");
            token.setExpiryDate(LocalDateTime.now().minusHours(1));

            // Act
            boolean isValid = tokenService.isTokenValid(token);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should return false when token just expired")
        void should_returnFalse_when_tokenJustExpired() {
            // Arrange
            PasswordResetTokenEntity token = createPasswordResetToken("reset-token");
            token.setExpiryDate(LocalDateTime.now().minusSeconds(1));

            // Act
            boolean isValid = tokenService.isTokenValid(token);

            // Assert
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteVerificationToken")
    class DeleteVerificationTokenTests {

        @Test
        @DisplayName("should delete verification token")
        void should_deleteToken_when_called() {
            // Arrange
            VerificationTokenEntity token = createVerificationToken("test-token");

            // Act
            tokenService.deleteVerificationToken(token);

            // Assert
            verify(verificationTokenRepository).delete(token);
        }
    }

    @Nested
    @DisplayName("deletePasswordResetToken")
    class DeletePasswordResetTokenTests {

        @Test
        @DisplayName("should delete password reset token")
        void should_deleteToken_when_called() {
            // Arrange
            PasswordResetTokenEntity token = createPasswordResetToken("reset-token");

            // Act
            tokenService.deletePasswordResetToken(token);

            // Assert
            verify(passwordResetTokenRepository).delete(token);
        }
    }

    // Helper methods

    private UserEntity createUser(String username, String email) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encoded-password");
        return user;
    }

    private VerificationTokenEntity createVerificationToken(String tokenString) {
        VerificationTokenEntity token = new VerificationTokenEntity();
        token.setToken(tokenString);
        token.setUser(createUser("testuser", "test@example.com"));
        token.setExpiryDate(LocalDateTime.now().plusHours(24));
        return token;
    }

    private PasswordResetTokenEntity createPasswordResetToken(String tokenString) {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.setToken(tokenString);
        token.setUser(createUser("testuser", "test@example.com"));
        token.setExpiryDate(LocalDateTime.now().plusHours(24));
        return token;
    }
}
