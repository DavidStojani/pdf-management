package org.papercloud.de.core.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailVerificationRequest {
    private String token;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}
