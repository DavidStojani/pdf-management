package org.papercloud.de.common.dto.auth;

import lombok.Data;

@Data
public class PasswordChangeRequest {

  private String token;
  private String newPassword;
  private String confirmPassword;
}
