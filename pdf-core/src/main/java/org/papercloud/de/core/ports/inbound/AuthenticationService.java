package org.papercloud.de.core.ports.inbound;

import org.papercloud.de.core.dto.auth.AuthResponse;
import org.papercloud.de.core.dto.auth.LoginRequest;
import org.papercloud.de.core.dto.auth.RegisterRequest;

/**
 * Port interface for authentication use cases.
 */
public interface AuthenticationService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
