package org.papercloud.de.core.dto.auth;

import lombok.Data;

@Data
public class EmailVerificationRequest {
    private String token;
    private String email;
}
