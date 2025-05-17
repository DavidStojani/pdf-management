package org.papercloud.de.pdfservice.auth;

public interface VerificationService {
    void verifyEmail(String token);
    void resendVerificationEmail(String email);
}
