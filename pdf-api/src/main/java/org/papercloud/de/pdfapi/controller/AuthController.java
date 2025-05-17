package org.papercloud.de.pdfapi.controller;


import org.papercloud.de.common.dto.auth.*;
import org.papercloud.de.pdfservice.auth.AuthenticationService;
import org.papercloud.de.pdfservice.auth.PasswordService;
import org.papercloud.de.pdfservice.auth.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final PasswordService passwordService;
    private final VerificationService verificationService;

    public AuthController(
            AuthenticationService authenticationService,
            PasswordService passwordService,
            VerificationService verificationService) {
        this.authenticationService = authenticationService;
        this.passwordService = passwordService;
        this.verificationService = verificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/password-reset-request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        passwordService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok().body(Map.of("message", "Password reset email sent"));
    }

    @PostMapping("/password-reset")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordChangeRequest request) {
        passwordService.resetPassword(
                request.getToken(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        return ResponseEntity.ok().body(Map.of("message", "Password reset successful"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        verificationService.verifyEmail(token);
        return ResponseEntity.ok().body(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody EmailVerificationRequest request) {
        verificationService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok().body(Map.of("message", "Verification email sent"));
    }
}
