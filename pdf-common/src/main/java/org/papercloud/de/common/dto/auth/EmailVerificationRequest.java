package org.papercloud.de.common.dto.auth;

import lombok.Data;

@Data
public class EmailVerificationRequest {
  private String token;
  private String email;
}
