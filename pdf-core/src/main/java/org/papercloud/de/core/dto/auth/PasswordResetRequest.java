package org.papercloud.de.core.dto.auth;

import lombok.Data;

@Data
public class PasswordResetRequest {
    private String email;
}
