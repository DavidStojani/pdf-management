//package org.papercloud.de.pdfapi.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.papercloud.de.core.dto.auth.AuthResponse;
//import org.papercloud.de.core.dto.auth.EmailVerificationRequest;
//import org.papercloud.de.core.dto.auth.LoginRequest;
//import org.papercloud.de.core.dto.auth.PasswordChangeRequest;
//import org.papercloud.de.core.dto.auth.PasswordResetRequest;
//import org.papercloud.de.core.dto.auth.RegisterRequest;
//import org.papercloud.de.core.ports.inbound.AuthenticationService;
//import org.papercloud.de.core.ports.inbound.PasswordService;
//import org.papercloud.de.pdfsecurity.service.VerificationJWTService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Import;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.FilterType;
//import org.springframework.context.annotation.Primary;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.concurrent.atomic.AtomicReference;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * Integration tests for AuthController using MockMvc.
// * Tests authentication endpoints including registration, login, email verification, and password reset.
// */
//@WebMvcTest(controllers = AuthController.class,
//        properties = "spring.main.allow-bean-definition-overriding=true",
//        excludeFilters = {
//                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = DocumentController.class)
//        },
//        excludeAutoConfiguration = {
//                org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
//                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
//        })
//@AutoConfigureMockMvc(addFilters = false)
//@Import(AuthControllerTest.TestStubs.class)
//@ContextConfiguration(classes = {AuthController.class, GlobalExceptionHandler.class, AuthControllerTest.TestStubs.class})
//@DisplayName("AuthController Integration Tests")
//class AuthControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private StubAuthenticationService authenticationService;
//
//    @Autowired
//    private StubPasswordService passwordService;
//
//    @Autowired
//    private StubVerificationJWTService verificationService;
//
//    private AuthResponse sampleAuthResponse;
//
//    @BeforeEach
//    void setUp() {
//        sampleAuthResponse = AuthResponse.builder()
//                .jwtToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test")
//                .refreshToken("refresh-token-123")
//                .username("testuser")
//                .email("testuser@example.com")
//                .userId(1L)
//                .roles(new String[]{"ROLE_USER"})
//                .build();
//
//        authenticationService.reset();
//        passwordService.reset();
//        verificationService.reset();
//    }
//
//    @Nested
//    @DisplayName("Register Endpoint Tests")
//    class RegisterEndpointTests {
//
//        @Test
//        @DisplayName("should successfully register new user with valid credentials")
//        void register_validCredentials_returnsAuthResponse() throws Exception {
//            RegisterRequest request = new RegisterRequest();
//            request.setUsername("newuser");
//            request.setEmail("newuser@example.com");
//            request.setPassword("SecurePass123!");
//            request.setConfirmPassword("SecurePass123!");
//
//            authenticationService.nextRegisterResponse.set(sampleAuthResponse);
//
//            mockMvc.perform(post("/api/auth/register")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.jwtToken").value(sampleAuthResponse.getJwtToken()))
//                    .andExpect(jsonPath("$.username").value(sampleAuthResponse.getUsername()))
//                    .andExpect(jsonPath("$.email").value(sampleAuthResponse.getEmail()))
//                    .andExpect(jsonPath("$.userId").value(1))
//                    .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
//
//            assertThat(authenticationService.lastRegisterRequest.get()).isNotNull();
//        }
//
//        @Test
//        @DisplayName("should handle registration with minimum valid data")
//        void register_minimumValidData_processesSuccessfully() throws Exception {
//            RegisterRequest request = new RegisterRequest();
//            request.setUsername("user");
//            request.setEmail("u@example.com");
//            request.setPassword("pass");
//            request.setConfirmPassword("pass");
//
//            authenticationService.nextRegisterResponse.set(sampleAuthResponse);
//
//            mockMvc.perform(post("/api/auth/register")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.jwtToken").exists());
//        }
//
//        @Test
//        @DisplayName("should handle registration with special characters in username")
//        void register_specialCharactersInUsername_processesRequest() throws Exception {
//            RegisterRequest request = new RegisterRequest();
//            request.setUsername("user_name-123");
//            request.setEmail("user@example.com");
//            request.setPassword("password");
//            request.setConfirmPassword("password");
//
//            AuthResponse response = AuthResponse.builder()
//                    .username("user_name-123")
//                    .jwtToken("token")
//                    .build();
//            authenticationService.nextRegisterResponse.set(response);
//
//            mockMvc.perform(post("/api/auth/register")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.username").value("user_name-123"));
//        }
//
//        @Test
//        @DisplayName("should handle registration error when service throws exception")
//        void register_serviceException_returnsErrorStatus() throws Exception {
//            RegisterRequest request = new RegisterRequest();
//            request.setUsername("existinguser");
//            request.setEmail("existing@example.com");
//            request.setPassword("password");
//            request.setConfirmPassword("password");
//
//            authenticationService.nextRegisterError.set(new RuntimeException("Username already exists"));
//
//            mockMvc.perform(post("/api/auth/register")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().is5xxServerError());
//        }
//    }
//
//    @Nested
//    @DisplayName("Login Endpoint Tests")
//    class LoginEndpointTests {
//
//        @Test
//        @DisplayName("should successfully login with valid credentials")
//        void login_validCredentials_returnsAuthResponse() throws Exception {
//            LoginRequest request = new LoginRequest();
//            request.setUsername("testuser");
//            request.setPassword("password123");
//
//            authenticationService.nextLoginResponse.set(sampleAuthResponse);
//
//            mockMvc.perform(post("/api/auth/login")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.jwtToken").exists())
//                    .andExpect(jsonPath("$.refreshToken").exists())
//                    .andExpect(jsonPath("$.username").value("testuser"));
//
//            assertThat(authenticationService.lastLoginRequest.get()).isNotNull();
//        }
//
//        @Test
//        @DisplayName("should return auth response with all fields populated")
//        void login_successfulLogin_returnsCompleteAuthResponse() throws Exception {
//            LoginRequest request = new LoginRequest();
//            request.setUsername("admin");
//            request.setPassword("adminpass");
//
//            AuthResponse adminResponse = AuthResponse.builder()
//                    .jwtToken("admin-jwt-token")
//                    .refreshToken("admin-refresh-token")
//                    .username("admin")
//                    .email("admin@example.com")
//                    .userId(100L)
//                    .roles(new String[]{"ROLE_ADMIN", "ROLE_USER"})
//                    .build();
//            authenticationService.nextLoginResponse.set(adminResponse);
//
//            mockMvc.perform(post("/api/auth/login")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.userId").value(100))
//                    .andExpect(jsonPath("$.roles.length()").value(2))
//                    .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
//        }
//
//        @Test
//        @DisplayName("should handle login failure with invalid credentials")
//        void login_invalidCredentials_returnsErrorStatus() throws Exception {
//            LoginRequest request = new LoginRequest();
//            request.setUsername("wronguser");
//            request.setPassword("wrongpass");
//
//            authenticationService.nextLoginError.set(new RuntimeException("Invalid credentials"));
//
//            mockMvc.perform(post("/api/auth/login")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().is5xxServerError());
//        }
//
//        @Test
//        @DisplayName("should handle login with empty username")
//        void login_emptyUsername_processesRequest() throws Exception {
//            LoginRequest request = new LoginRequest();
//            request.setUsername("");
//            request.setPassword("password");
//
//            authenticationService.nextLoginError.set(new RuntimeException("Username cannot be empty"));
//
//            mockMvc.perform(post("/api/auth/login")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().is5xxServerError());
//        }
//    }
//
//    @Nested
//    @DisplayName("Email Verification Endpoint Tests")
//    class EmailVerificationTests {
//
//        @Test
//        @DisplayName("should successfully verify email with valid token")
//        void verifyEmail_validToken_returnsSuccessMessage() throws Exception {
//            String token = "valid-verification-token-123";
//
//            mockMvc.perform(get("/api/auth/verify-email")
//                            .param("token", token))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.message").value("Email verified successfully"));
//
//            assertThat(verificationService.lastVerifiedToken.get()).isEqualTo(token);
//        }
//
//        @Test
//        @DisplayName("should handle verification with invalid token")
//        void verifyEmail_invalidToken_returnsErrorStatus() throws Exception {
//            String token = "invalid-token";
//            verificationService.nextVerifyError.set(new RuntimeException("Invalid or expired token"));
//
//            mockMvc.perform(get("/api/auth/verify-email")
//                            .param("token", token))
//                    .andExpect(status().is5xxServerError());
//        }
//
//        @Test
//        @DisplayName("should handle verification with empty token")
//        void verifyEmail_emptyToken_processesRequest() throws Exception {
//            verificationService.nextVerifyError.set(new RuntimeException("Token cannot be empty"));
//
//            mockMvc.perform(get("/api/auth/verify-email")
//                            .param("token", ""))
//                    .andExpect(status().is5xxServerError());
//        }
//
//        @Test
//        @DisplayName("should successfully resend verification email")
//        void resendVerification_validEmail_returnsSuccessMessage() throws Exception {
//            EmailVerificationRequest request = new EmailVerificationRequest();
//            request.setEmail("testuser@example.com");
//
//            mockMvc.perform(post("/api/auth/resend-verification")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.message").value("Verification email sent"));
//
//            assertThat(verificationService.lastResentEmail.get()).isEqualTo(request.getEmail());
//        }
//
//        @Test
//        @DisplayName("should handle resend verification for non-existent email")
//        void resendVerification_nonExistentEmail_returnsErrorStatus() throws Exception {
//            EmailVerificationRequest request = new EmailVerificationRequest();
//            request.setEmail("nonexistent@example.com");
//
//            verificationService.nextResendError.set(new RuntimeException("Email not found"));
//
//            mockMvc.perform(post("/api/auth/resend-verification")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().is5xxServerError());
//        }
//    }
//
//    @Nested
//    @DisplayName("Password Reset Endpoint Tests")
//    class PasswordResetTests {
//
//        @Test
//        @DisplayName("should successfully request password reset")
//        void requestPasswordReset_validEmail_returnsSuccessMessage() throws Exception {
//            PasswordResetRequest request = new PasswordResetRequest();
//            request.setEmail("testuser@example.com");
//
//            mockMvc.perform(post("/api/auth/password-reset-request")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.message").value("Password reset email sent"));
//
//            assertThat(passwordService.lastResetRequestEmail.get()).isEqualTo(request.getEmail());
//        }
//
//        @Test
//        @DisplayName("should handle password reset request for non-existent email")
//        void requestPasswordReset_nonExistentEmail_returnsSuccessMessage() throws Exception {
//            PasswordResetRequest request = new PasswordResetRequest();
//            request.setEmail("nonexistent@example.com");
//
//            mockMvc.perform(post("/api/auth/password-reset-request")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.message").value("Password reset email sent"));
//        }
//
//        @Test
//        @DisplayName("should successfully reset password with valid token")
//        void resetPassword_validTokenAndPassword_returnsSuccessMessage() throws Exception {
//            PasswordChangeRequest request = new PasswordChangeRequest();
//            request.setToken("valid-reset-token");
//            request.setNewPassword("NewSecurePass123!");
//            request.setConfirmPassword("NewSecurePass123!");
//
//            mockMvc.perform(post("/api/auth/password-reset")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.message").value("Password reset successful"));
//
//            assertThat(passwordService.lastResetToken.get()).isEqualTo(request.getToken());
//            assertThat(passwordService.lastResetNewPassword.get()).isEqualTo(request.getNewPassword());
//            assertThat(passwordService.lastResetConfirmPassword.get()).isEqualTo(request.getConfirmPassword());
//        }
//
//        @Test
//        @DisplayName("should handle password reset with invalid token")
//        void resetPassword_invalidToken_returnsErrorStatus() throws Exception {
//            PasswordChangeRequest request = new PasswordChangeRequest();
//            request.setToken("invalid-token");
//            request.setNewPassword("NewPassword123");
//            request.setConfirmPassword("NewPassword123");
//
//            passwordService.nextResetError.set(new RuntimeException("Invalid or expired token"));
//
//            mockMvc.perform(post("/api/auth/password-reset")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().is5xxServerError());
//        }
//
//        @Test
//        @DisplayName("should handle password reset with mismatched passwords")
//        void resetPassword_mismatchedPasswords_returnsErrorStatus() throws Exception {
//            PasswordChangeRequest request = new PasswordChangeRequest();
//            request.setToken("valid-token");
//            request.setNewPassword("Password123");
//            request.setConfirmPassword("DifferentPassword123");
//
//            passwordService.nextResetError.set(new RuntimeException("Passwords do not match"));
//
//            mockMvc.perform(post("/api/auth/password-reset")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().is5xxServerError());
//        }
//
//        @Test
//        @DisplayName("should handle password reset with weak password")
//        void resetPassword_weakPassword_returnsErrorStatus() throws Exception {
//            PasswordChangeRequest request = new PasswordChangeRequest();
//            request.setToken("valid-token");
//            request.setNewPassword("123");
//            request.setConfirmPassword("123");
//
//            passwordService.nextResetError.set(new RuntimeException("Password does not meet security requirements"));
//
//            mockMvc.perform(post("/api/auth/password-reset")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().is5xxServerError());
//        }
//    }
//
//    @TestConfiguration
//    static class TestStubs {
//        @Bean
//        @Primary
//        StubAuthenticationService authenticationService() {
//            return new StubAuthenticationService();
//        }
//
//        @Bean
//        @Primary
//        StubPasswordService passwordService() {
//            return new StubPasswordService();
//        }
//
//        @Bean
//        @Primary
//        StubVerificationJWTService verificationService() {
//            return new StubVerificationJWTService();
//        }
//    }
//
//    static final class StubAuthenticationService implements AuthenticationService {
//        final AtomicReference<RegisterRequest> lastRegisterRequest = new AtomicReference<>();
//        final AtomicReference<LoginRequest> lastLoginRequest = new AtomicReference<>();
//        final AtomicReference<AuthResponse> nextRegisterResponse = new AtomicReference<>();
//        final AtomicReference<AuthResponse> nextLoginResponse = new AtomicReference<>();
//        final AtomicReference<RuntimeException> nextRegisterError = new AtomicReference<>();
//        final AtomicReference<RuntimeException> nextLoginError = new AtomicReference<>();
//
//        @Override
//        public AuthResponse register(RegisterRequest request) {
//            lastRegisterRequest.set(request);
//            if (nextRegisterError.get() != null) {
//                throw nextRegisterError.get();
//            }
//            return nextRegisterResponse.get();
//        }
//
//        @Override
//        public AuthResponse login(LoginRequest request) {
//            lastLoginRequest.set(request);
//            if (nextLoginError.get() != null) {
//                throw nextLoginError.get();
//            }
//            return nextLoginResponse.get();
//        }
//
//        void reset() {
//            lastRegisterRequest.set(null);
//            lastLoginRequest.set(null);
//            nextRegisterResponse.set(null);
//            nextLoginResponse.set(null);
//            nextRegisterError.set(null);
//            nextLoginError.set(null);
//        }
//    }
//
//    static final class StubPasswordService implements PasswordService {
//        final AtomicReference<String> lastResetRequestEmail = new AtomicReference<>();
//        final AtomicReference<String> lastResetToken = new AtomicReference<>();
//        final AtomicReference<String> lastResetNewPassword = new AtomicReference<>();
//        final AtomicReference<String> lastResetConfirmPassword = new AtomicReference<>();
//        final AtomicReference<RuntimeException> nextResetError = new AtomicReference<>();
//
//        @Override
//        public void initiatePasswordReset(String email) {
//            lastResetRequestEmail.set(email);
//        }
//
//        @Override
//        public void resetPassword(String token, String newPassword, String confirmPassword) {
//            if (nextResetError.get() != null) {
//                throw nextResetError.get();
//            }
//            lastResetToken.set(token);
//            lastResetNewPassword.set(newPassword);
//            lastResetConfirmPassword.set(confirmPassword);
//        }
//
//        void reset() {
//            lastResetRequestEmail.set(null);
//            lastResetToken.set(null);
//            lastResetNewPassword.set(null);
//            lastResetConfirmPassword.set(null);
//            nextResetError.set(null);
//        }
//    }
//
//    static final class StubVerificationJWTService extends VerificationJWTService {
//        final AtomicReference<String> lastVerifiedToken = new AtomicReference<>();
//        final AtomicReference<String> lastResentEmail = new AtomicReference<>();
//        final AtomicReference<RuntimeException> nextVerifyError = new AtomicReference<>();
//        final AtomicReference<RuntimeException> nextResendError = new AtomicReference<>();
//
//        StubVerificationJWTService() {
//            super(null, null, null);
//        }
//
//        @Override
//        public void verifyEmail(String token) {
//            if (nextVerifyError.get() != null) {
//                throw nextVerifyError.get();
//            }
//            lastVerifiedToken.set(token);
//        }
//
//        @Override
//        public void resendVerificationEmail(String email) {
//            if (nextResendError.get() != null) {
//                throw nextResendError.get();
//            }
//            lastResentEmail.set(email);
//        }
//
//        void reset() {
//            lastVerifiedToken.set(null);
//            lastResentEmail.set(null);
//            nextVerifyError.set(null);
//            nextResendError.set(null);
//        }
//    }
//}
