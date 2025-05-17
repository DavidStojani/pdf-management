package org.papercloud.de.pdfservice.auth;

public interface PasswordService {
    void initiatePasswordReset(String email);
    void resetPassword(String token, String newPassword, String confirmPassword);
}
