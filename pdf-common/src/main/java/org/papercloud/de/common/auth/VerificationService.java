package org.papercloud.de.common.auth;

public interface VerificationService {
    void verifyEmail(String token);
    void resendVerificationEmail(String email);
}
