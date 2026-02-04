package org.papercloud.de.core.dto.auth;

import lombok.Data;

@Data
public class PasswordChangeRequest {
    private String token;
    private String newPassword;
    private String confirmPassword;
}
