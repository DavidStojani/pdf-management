package org.papercloud.de.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

  private String jwtToken;
  private String refreshToken;
  private String username;
  private String email;
  private Long userId;
  private String[] roles;
}
