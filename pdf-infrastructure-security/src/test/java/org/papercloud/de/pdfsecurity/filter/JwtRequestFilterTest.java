package org.papercloud.de.pdfsecurity.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.pdfsecurity.service.PdfUserDetailsService;
import org.papercloud.de.pdfsecurity.util.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtRequestFilter.
 * Tests JWT authentication filter behavior including token extraction, validation, and SecurityContext setup.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtRequestFilter")
class JwtRequestFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PdfUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtRequestFilter jwtRequestFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtRequestFilter = new JwtRequestFilter(jwtUtil, userDetailsService);
    }

    @Nested
    @DisplayName("No Authorization Header")
    class NoAuthorizationHeaderTests {

        @Test
        @DisplayName("should pass through when no Authorization header present")
        void should_passThrough_when_noAuthHeader() throws ServletException, IOException {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn(null);

            // Act
            jwtRequestFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(jwtUtil, never()).getUsernameFromToken(anyString());
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should pass through when Authorization header does not start with Bearer")
        void should_passThrough_when_notBearerToken() throws ServletException, IOException {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

            // Act
            jwtRequestFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(jwtUtil, never()).getUsernameFromToken(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should pass through when Authorization header is empty")
        void should_passThrough_when_emptyAuthHeader() throws ServletException, IOException {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("");

            // Act
            jwtRequestFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(jwtUtil, never()).getUsernameFromToken(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Valid Token")
    class ValidTokenTests {

        @Test
        @DisplayName("should set SecurityContext when token is valid")
        void should_setSecurityContext_when_validToken() throws ServletException, IOException {
            // Arrange
            String token = "valid-jwt-token";
            String username = "testuser";
            UserDetails userDetails = createUserDetails(username);

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.getUsernameFromToken(token)).thenReturn(username);
            when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
            when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);

            // Act
            jwtRequestFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isInstanceOf(UsernamePasswordAuthenticationToken.class);

            UsernamePasswordAuthenticationToken auth =
                    (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getPrincipal()).isEqualTo(userDetails);
            assertThat(auth.getAuthorities()).contains(new SimpleGrantedAuthority("ROLE_USER"));
        }

        @Test
        @DisplayName("should extract token correctly from Bearer header")
        void should_extractToken_when_bearerHeader() throws ServletException, IOException {
            // Arrange
            String token = "my-jwt-token-123";
            String username = "user123";
            UserDetails userDetails = createUserDetails(username);

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.getUsernameFromToken(token)).thenReturn(username);
            when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
            when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);

            // Act
            jwtRequestFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(jwtUtil).getUsernameFromToken(token);
        }

        @Test
        @DisplayName("should load user details for extracted username")
        void should_loadUserDetails_when_tokenValid() throws ServletException, IOException {
            // Arrange
            String token = "valid-token";
            String username = "specificuser";
            UserDetails userDetails = createUserDetails(username);

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.getUsernameFromToken(token)).thenReturn(username);
            when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
            when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);

            // Act
            jwtRequestFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(userDetailsService).loadUserByUsername(username);
        }
    }

    @Nested
    @DisplayName("Invalid Token")
    class InvalidTokenTests {

        @Test
        @DisplayName("should pass through when token validation fails")
        void should_passThrough_when_invalidToken() throws ServletException, IOException {
            // Arrange
            String token = "invalid-token";
            String username = "testuser";
            UserDetails userDetails = createUserDetails(username);

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.getUsernameFromToken(token)).thenReturn(username);
            when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
            when(jwtUtil.validateToken(token, userDetails)).thenReturn(false);

            // Act
            jwtRequestFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should pass through when token parsing throws exception")
        void should_passThrough_when_tokenParsingFails() throws ServletException, IOException {
            // Arrange
            String token = "malformed-token";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.getUsernameFromToken(token)).thenThrow(new RuntimeException("Invalid token"));

            // Act
            jwtRequestFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should pass through when username extraction returns null")
        void should_passThrough_when_usernameIsNull() throws ServletException, IOException {
            // Arrange
            String token = "token-without-username";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.getUsernameFromToken(token)).thenReturn(null);

            // Act
            jwtRequestFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("SecurityContext Already Set")
    class SecurityContextAlreadySetTests {

        @Test
        @DisplayName("should not set authentication when SecurityContext already has authentication")
        void should_skipAuthentication_when_alreadyAuthenticated() throws ServletException, IOException {
            // Arrange
            String token = "valid-token";
            String username = "testuser";

            // Pre-set SecurityContext
            UserDetails existingUser = createUserDetails("existinguser");
            UsernamePasswordAuthenticationToken existingAuth =
                    new UsernamePasswordAuthenticationToken(existingUser, null, existingUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(existingAuth);

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.getUsernameFromToken(token)).thenReturn(username);

            // Act
            jwtRequestFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            verify(jwtUtil, never()).validateToken(anyString(), any());

            // SecurityContext should still have the original authentication
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existingAuth);
        }
    }

    @Nested
    @DisplayName("Filter Chain Continuation")
    class FilterChainContinuationTests {

        @Test
        @DisplayName("should always call filterChain.doFilter")
        void should_callFilterChain_when_processing() throws ServletException, IOException {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn(null);

            // Act
            jwtRequestFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should call filterChain.doFilter even after successful authentication")
        void should_callFilterChain_when_authenticated() throws ServletException, IOException {
            // Arrange
            String token = "valid-token";
            String username = "testuser";
            UserDetails userDetails = createUserDetails(username);

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.getUsernameFromToken(token)).thenReturn(username);
            when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
            when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);

            // Act
            jwtRequestFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should call filterChain.doFilter even when exception occurs")
        void should_callFilterChain_when_exceptionOccurs() throws ServletException, IOException {
            // Arrange
            String token = "bad-token";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.getUsernameFromToken(token)).thenThrow(new RuntimeException("Token error"));

            // Act
            jwtRequestFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
        }
    }

    // Helper methods

    private UserDetails createUserDetails(String username) {
        return new User(
                username,
                "password",
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
