package org.papercloud.de.common.auth;

import org.papercloud.de.common.dto.auth.AuthResponse;
import org.papercloud.de.common.dto.auth.LoginRequest;
import org.papercloud.de.common.dto.auth.RegisterRequest;
import org.springframework.stereotype.Service;



public interface AuthenticationService {
    AuthResponse register (RegisterRequest request);
    AuthResponse login (LoginRequest request);
}
