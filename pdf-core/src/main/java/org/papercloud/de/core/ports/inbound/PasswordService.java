package org.papercloud.de.core.ports.inbound;

/**
 * Port interface for password management use cases.
 */
public interface PasswordService {
    void initiatePasswordReset(String email);
    void resetPassword(String token, String newPassword, String confirmPassword);
}
