package org.papercloud.de.pdfapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.papercloud.de.core.dto.auth.AuthResponse;
import org.papercloud.de.core.dto.auth.EmailVerificationRequest;
import org.papercloud.de.core.dto.auth.LoginRequest;
import org.papercloud.de.core.dto.auth.PasswordChangeRequest;
import org.papercloud.de.core.dto.auth.PasswordResetRequest;
import org.papercloud.de.core.dto.auth.RegisterRequest;
import org.papercloud.de.core.ports.inbound.AuthenticationService;
import org.papercloud.de.core.ports.inbound.PasswordService;
import org.papercloud.de.pdfsecurity.service.VerificationJWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for AuthController using MockMvc.
 * Tests authentication endpoints including registration, login, email verification, and password reset.
 */
@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private PasswordService passwordService;

    @MockBean
    private VerificationJWTService verificationService;

    private AuthResponse sampleAuthResponse;

    @BeforeEach
    void setUp() {
        sampleAuthResponse = AuthResponse.builder()
                .jwtToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test")
                .refreshToken("refresh-token-123")
                .username("testuser")
                .email("testuser@example.com")
                .userId(1L)
                .roles(new String[]{"ROLE_USER"})
                .build();
    }

    @Nested
    @DisplayName("Register Endpoint Tests")
    class RegisterEndpointTests {

        @Test
        @DisplayName("should successfully register new user with valid credentials")
        void register_validCredentials_returnsAuthResponse() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("newuser@example.com");
            request.setPassword("SecurePass123!");
            request.setConfirmPassword("SecurePass123!");

            when(authenticationService.register(any(RegisterRequest.class)))
                    .thenReturn(sampleAuthResponse);

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jwtToken").value(sampleAuthResponse.getJwtToken()))
                    .andExpect(jsonPath("$.username").value(sampleAuthResponse.getUsername()))
                    .andExpect(jsonPath("$.email").value(sampleAuthResponse.getEmail()))
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

            verify(authenticationService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("should handle registration with minimum valid data")
        void register_minimumValidData_processesSuccessfully() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setUsername("user");
            request.setEmail("u@example.com");
            request.setPassword("pass");
            request.setConfirmPassword("pass");

            when(authenticationService.register(any(RegisterRequest.class)))
                    .thenReturn(sampleAuthResponse);

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jwtToken").exists());
        }

        @Test
        @DisplayName("should handle registration with special characters in username")
        void register_specialCharactersInUsername_processesRequest() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setUsername("user_name-123");
            request.setEmail("user@example.com");
            request.setPassword("password");
            request.setConfirmPassword("password");

            AuthResponse response = AuthResponse.builder()
                    .username("user_name-123")
                    .jwtToken("token")
                    .build();

            when(authenticationService.register(any(RegisterRequest.class)))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("user_name-123"));
        }

        @Test
        @DisplayName("should handle registration error when service throws exception")
        void register_serviceException_returnsErrorStatus() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setUsername("existinguser");
            request.setEmail("existing@example.com");
            request.setPassword("password");
            request.setConfirmPassword("password");

            when(authenticationService.register(any(RegisterRequest.class)))
                    .thenThrow(new RuntimeException("Username already exists"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("Login Endpoint Tests")
    class LoginEndpointTests {

        @Test
        @DisplayName("should successfully login with valid credentials")
        void login_validCredentials_returnsAuthResponse() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");

            when(authenticationService.login(any(LoginRequest.class)))
                    .thenReturn(sampleAuthResponse);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jwtToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.username").value("testuser"));

            verify(authenticationService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("should return auth response with all fields populated")
        void login_successfulLogin_returnsCompleteAuthResponse() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("adminpass");

            AuthResponse adminResponse = AuthResponse.builder()
                    .jwtToken("admin-jwt-token")
                    .refreshToken("admin-refresh-token")
                    .username("admin")
                    .email("admin@example.com")
                    .userId(100L)
                    .roles(new String[]{"ROLE_ADMIN", "ROLE_USER"})
                    .build();

            when(authenticationService.login(any(LoginRequest.class)))
                    .thenReturn(adminResponse);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(100))
                    .andExpect(jsonPath("$.roles.length()").value(2))
                    .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
        }

        @Test
        @DisplayName("should handle login failure with invalid credentials")
        void login_invalidCredentials_returnsErrorStatus() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername("wronguser");
            request.setPassword("wrongpass");

            when(authenticationService.login(any(LoginRequest.class)))
                    .thenThrow(new RuntimeException("Invalid credentials"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("should handle login with empty username")
        void login_emptyUsername_processesRequest() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername("");
            request.setPassword("password");

            when(authenticationService.login(any(LoginRequest.class)))
                    .thenThrow(new RuntimeException("Username cannot be empty"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("Email Verification Endpoint Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("should successfully verify email with valid token")
        void verifyEmail_validToken_returnsSuccessMessage() throws Exception {
            // Arrange
            String token = "valid-verification-token-123";
            doNothing().when(verificationService).verifyEmail(token);

            // Act & Assert
            mockMvc.perform(get("/api/auth/verify-email")
                            .param("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Email verified successfully"));

            verify(verificationService).verifyEmail(token);
        }

        @Test
        @DisplayName("should handle verification with invalid token")
        void verifyEmail_invalidToken_returnsErrorStatus() throws Exception {
            // Arrange
            String token = "invalid-token";
            doThrow(new RuntimeException("Invalid or expired token"))
                    .when(verificationService).verifyEmail(token);

            // Act & Assert
            mockMvc.perform(get("/api/auth/verify-email")
                            .param("token", token))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("should handle verification with empty token")
        void verifyEmail_emptyToken_processesRequest() throws Exception {
            // Arrange
            doThrow(new RuntimeException("Token cannot be empty"))
                    .when(verificationService).verifyEmail("");

            // Act & Assert
            mockMvc.perform(get("/api/auth/verify-email")
                            .param("token", ""))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("should successfully resend verification email")
        void resendVerification_validEmail_returnsSuccessMessage() throws Exception {
            // Arrange
            EmailVerificationRequest request = new EmailVerificationRequest();
            request.setEmail("testuser@example.com");

            doNothing().when(verificationService).resendVerificationEmail(request.getEmail());

            // Act & Assert
            mockMvc.perform(post("/api/auth/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Verification email sent"));

            verify(verificationService).resendVerificationEmail(request.getEmail());
        }

        @Test
        @DisplayName("should handle resend verification for non-existent email")
        void resendVerification_nonExistentEmail_returnsErrorStatus() throws Exception {
            // Arrange
            EmailVerificationRequest request = new EmailVerificationRequest();
            request.setEmail("nonexistent@example.com");

            doThrow(new RuntimeException("Email not found"))
                    .when(verificationService).resendVerificationEmail(request.getEmail());

            // Act & Assert
            mockMvc.perform(post("/api/auth/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("Password Reset Endpoint Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("should successfully request password reset")
        void requestPasswordReset_validEmail_returnsSuccessMessage() throws Exception {
            // Arrange
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail("testuser@example.com");

            doNothing().when(passwordService).initiatePasswordReset(request.getEmail());

            // Act & Assert
            mockMvc.perform(post("/api/auth/password-reset-request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset email sent"));

            verify(passwordService).initiatePasswordReset(request.getEmail());
        }

        @Test
        @DisplayName("should handle password reset request for non-existent email")
        void requestPasswordReset_nonExistentEmail_returnsSuccessMessage() throws Exception {
            // Arrange - Security best practice: don't reveal if email exists
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail("nonexistent@example.com");

            doNothing().when(passwordService).initiatePasswordReset(request.getEmail());

            // Act & Assert
            mockMvc.perform(post("/api/auth/password-reset-request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset email sent"));
        }

        @Test
        @DisplayName("should successfully reset password with valid token")
        void resetPassword_validTokenAndPassword_returnsSuccessMessage() throws Exception {
            // Arrange
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setToken("valid-reset-token");
            request.setNewPassword("NewSecurePass123!");
            request.setConfirmPassword("NewSecurePass123!");

            doNothing().when(passwordService).resetPassword(
                    eq(request.getToken()),
                    eq(request.getNewPassword()),
                    eq(request.getConfirmPassword())
            );

            // Act & Assert
            mockMvc.perform(post("/api/auth/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset successful"));

            verify(passwordService).resetPassword(
                    request.getToken(),
                    request.getNewPassword(),
                    request.getConfirmPassword()
            );
        }

        @Test
        @DisplayName("should handle password reset with invalid token")
        void resetPassword_invalidToken_returnsErrorStatus() throws Exception {
            // Arrange
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setToken("invalid-token");
            request.setNewPassword("NewPassword123");
            request.setConfirmPassword("NewPassword123");

            doThrow(new RuntimeException("Invalid or expired token"))
                    .when(passwordService).resetPassword(
                            eq(request.getToken()),
                            eq(request.getNewPassword()),
                            eq(request.getConfirmPassword())
                    );

            // Act & Assert
            mockMvc.perform(post("/api/auth/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("should handle password reset with mismatched passwords")
        void resetPassword_mismatchedPasswords_returnsErrorStatus() throws Exception {
            // Arrange
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setToken("valid-token");
            request.setNewPassword("Password123");
            request.setConfirmPassword("DifferentPassword123");

            doThrow(new RuntimeException("Passwords do not match"))
                    .when(passwordService).resetPassword(
                            eq(request.getToken()),
                            eq(request.getNewPassword()),
                            eq(request.getConfirmPassword())
                    );

            // Act & Assert
            mockMvc.perform(post("/api/auth/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("should handle password reset with weak password")
        void resetPassword_weakPassword_returnsErrorStatus() throws Exception {
            // Arrange
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setToken("valid-token");
            request.setNewPassword("123");
            request.setConfirmPassword("123");

            doThrow(new RuntimeException("Password does not meet security requirements"))
                    .when(passwordService).resetPassword(
                            eq(request.getToken()),
                            eq(request.getNewPassword()),
                            eq(request.getConfirmPassword())
                    );

            // Act & Assert
            mockMvc.perform(post("/api/auth/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }
    }
}
