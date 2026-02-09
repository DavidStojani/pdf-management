package org.papercloud.de.pdfapi.controller;


import lombok.RequiredArgsConstructor;
import org.papercloud.de.core.dto.auth.*;
import org.papercloud.de.core.ports.inbound.AuthenticationService;
import org.papercloud.de.core.ports.inbound.PasswordService;
import org.papercloud.de.pdfsecurity.service.VerificationJWTService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final PasswordService passwordService;
    private final VerificationJWTService verificationService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authenticationService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset-request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        passwordService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Password reset email sent"));
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody PasswordChangeRequest request) {
        passwordService.resetPassword(
                request.getToken(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam("token") String token) {
        verificationService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody EmailVerificationRequest request) {
        verificationService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Verification email sent"));
    }
}
