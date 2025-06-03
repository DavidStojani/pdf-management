package org.papercloud.de.common.auth;

public interface PasswordService {
    void initiatePasswordReset(String email);
    void resetPassword(String token, String newPassword, String confirmPassword);
}
