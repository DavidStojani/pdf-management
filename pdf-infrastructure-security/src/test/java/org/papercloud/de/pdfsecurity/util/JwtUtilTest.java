package org.papercloud.de.pdfsecurity.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JwtUtil.
 * Tests JWT token generation, validation, claims extraction, and expiration handling.
 */
@DisplayName("JwtUtil")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_SECRET = "ThisIsAVeryLongSecretKeyForTestingPurposesOnly12345678901234567890";
    private static final Long TEST_EXPIRATION = 3600L; // 1 hour in seconds
    private static final Long SHORT_EXPIRATION = 1L; // 1 second for testing expiration

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
        jwtUtil.init();
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateTokenTests {

        @Test
        @DisplayName("should generate valid JWT token for user")
        void should_generateToken_when_validUserDetails() {
            // Arrange
            UserDetails userDetails = createUserDetails("testuser");

            // Act
            String token = jwtUtil.generateToken(userDetails);

            // Assert
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("should generate different tokens for different users")
        void should_generateDifferentTokens_when_differentUsers() {
            // Arrange
            UserDetails user1 = createUserDetails("user1");
            UserDetails user2 = createUserDetails("user2");

            // Act
            String token1 = jwtUtil.generateToken(user1);
            String token2 = jwtUtil.generateToken(user2);

            // Assert
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("should include username in token subject")
        void should_includeUsername_when_generatingToken() {
            // Arrange
            String username = "johndoe";
            UserDetails userDetails = createUserDetails(username);

            // Act
            String token = jwtUtil.generateToken(userDetails);
            String extractedUsername = jwtUtil.getUsernameFromToken(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(username);
        }
    }

    @Nested
    @DisplayName("getUsernameFromToken")
    class GetUsernameFromTokenTests {

        @Test
        @DisplayName("should extract username from valid token")
        void should_extractUsername_when_validToken() {
            // Arrange
            String username = "testuser";
            UserDetails userDetails = createUserDetails(username);
            String token = jwtUtil.generateToken(userDetails);

            // Act
            String extractedUsername = jwtUtil.getUsernameFromToken(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(username);
        }

        @Test
        @DisplayName("should throw exception for invalid token format")
        void should_throwException_when_invalidTokenFormat() {
            // Arrange
            String invalidToken = "invalid.token.format";

            // Act & Assert
            assertThatThrownBy(() -> jwtUtil.getUsernameFromToken(invalidToken))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error while parsing JWT token");
        }

        @Test
        @DisplayName("should throw exception for malformed token")
        void should_throwException_when_malformedToken() {
            // Arrange
            String malformedToken = "notATokenAtAll";

            // Act & Assert
            assertThatThrownBy(() -> jwtUtil.getUsernameFromToken(malformedToken))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("should validate token when username matches and not expired")
        void should_returnTrue_when_validToken() {
            // Arrange
            String username = "testuser";
            UserDetails userDetails = createUserDetails(username);
            String token = jwtUtil.generateToken(userDetails);

            // Act
            Boolean isValid = jwtUtil.validateToken(token, userDetails);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("should reject token when username does not match")
        void should_returnFalse_when_usernameMismatch() {
            // Arrange
            UserDetails user1 = createUserDetails("user1");
            UserDetails user2 = createUserDetails("user2");
            String token = jwtUtil.generateToken(user1);

            // Act
            Boolean isValid = jwtUtil.validateToken(token, user2);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should throw exception for expired token")
        void should_throwException_when_tokenExpired() throws InterruptedException {
            // Arrange
            JwtUtil shortExpirationJwtUtil = new JwtUtil();
            ReflectionTestUtils.setField(shortExpirationJwtUtil, "secret", TEST_SECRET);
            ReflectionTestUtils.setField(shortExpirationJwtUtil, "expiration", SHORT_EXPIRATION);
            shortExpirationJwtUtil.init();

            UserDetails userDetails = createUserDetails("testuser");
            String token = shortExpirationJwtUtil.generateToken(userDetails);

            // Wait for token to expire
            Thread.sleep(1500);

            // Act & Assert - expired token causes RuntimeException during parsing
            assertThatThrownBy(() -> shortExpirationJwtUtil.validateToken(token, userDetails))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error while parsing JWT token");
        }

        @Test
        @DisplayName("should validate token immediately after generation")
        void should_returnTrue_when_freshToken() {
            // Arrange
            UserDetails userDetails = createUserDetails("testuser");
            String token = jwtUtil.generateToken(userDetails);

            // Act
            Boolean isValid = jwtUtil.validateToken(token, userDetails);

            // Assert
            assertThat(isValid).isTrue();
        }
    }

    @Nested
    @DisplayName("extractRoles")
    class ExtractRolesTests {

        @Test
        @DisplayName("should extract roles when present in token")
        void should_extractRoles_when_rolesInToken() {
            // Arrange
            List<String> expectedRoles = Arrays.asList("ROLE_USER", "ROLE_ADMIN");
            String token = createTokenWithRoles("testuser", expectedRoles);

            // Act
            List<String> extractedRoles = jwtUtil.extractRoles(token);

            // Assert
            assertThat(extractedRoles).containsExactlyInAnyOrderElementsOf(expectedRoles);
        }

        @Test
        @DisplayName("should return null when roles not present in token")
        void should_returnNull_when_noRoles() {
            // Arrange
            UserDetails userDetails = createUserDetails("testuser");
            String token = jwtUtil.generateToken(userDetails);

            // Act
            List<String> extractedRoles = jwtUtil.extractRoles(token);

            // Assert
            assertThat(extractedRoles).isNull();
        }

        @Test
        @DisplayName("should extract empty list when roles claim is empty")
        void should_returnEmptyList_when_emptyRoles() {
            // Arrange
            String token = createTokenWithRoles("testuser", Collections.emptyList());

            // Act
            List<String> extractedRoles = jwtUtil.extractRoles(token);

            // Assert
            assertThat(extractedRoles).isEmpty();
        }
    }

    @Nested
    @DisplayName("isTokenExpired")
    class IsTokenExpiredTests {

        @Test
        @DisplayName("should return false for non-expired token")
        void should_returnFalse_when_tokenNotExpired() {
            // Arrange
            UserDetails userDetails = createUserDetails("testuser");
            String token = jwtUtil.generateToken(userDetails);

            // Act
            Boolean isExpired = jwtUtil.isTokenExpired(token);

            // Assert
            assertThat(isExpired).isFalse();
        }

        @Test
        @DisplayName("should throw exception for expired token")
        void should_throwException_when_tokenExpired() throws InterruptedException {
            // Arrange
            JwtUtil shortExpirationJwtUtil = new JwtUtil();
            ReflectionTestUtils.setField(shortExpirationJwtUtil, "secret", TEST_SECRET);
            ReflectionTestUtils.setField(shortExpirationJwtUtil, "expiration", SHORT_EXPIRATION);
            shortExpirationJwtUtil.init();

            UserDetails userDetails = createUserDetails("testuser");
            String token = shortExpirationJwtUtil.generateToken(userDetails);

            // Wait for token to expire
            Thread.sleep(1500);

            // Act & Assert - expired token causes RuntimeException during parsing
            assertThatThrownBy(() -> shortExpirationJwtUtil.isTokenExpired(token))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error while parsing JWT token");
        }
    }

    @Nested
    @DisplayName("expirationDateFromToken")
    class ExpirationDateFromTokenTests {

        @Test
        @DisplayName("should extract expiration date from token")
        void should_extractExpirationDate_when_validToken() {
            // Arrange
            UserDetails userDetails = createUserDetails("testuser");
            String token = jwtUtil.generateToken(userDetails);

            // Act
            Date expirationDate = jwtUtil.expirationDateFromToken(token);

            // Assert
            assertThat(expirationDate).isNotNull();
            assertThat(expirationDate).isAfter(new Date());
        }

        @Test
        @DisplayName("should have expiration approximately equal to configured duration")
        void should_haveCorrectExpiration_when_tokenGenerated() {
            // Arrange
            UserDetails userDetails = createUserDetails("testuser");
            long beforeGeneration = System.currentTimeMillis();
            String token = jwtUtil.generateToken(userDetails);
            long afterGeneration = System.currentTimeMillis();

            // Act
            Date expirationDate = jwtUtil.expirationDateFromToken(token);
            long expirationTime = expirationDate.getTime();

            // Assert - JWT timestamps are truncated to seconds, so allow 1 second margin
            long expectedMinExpiration = beforeGeneration + (TEST_EXPIRATION * 1000) - 1000;
            long expectedMaxExpiration = afterGeneration + (TEST_EXPIRATION * 1000) + 1000;
            assertThat(expirationTime).isBetween(expectedMinExpiration, expectedMaxExpiration);
        }
    }

    // Helper methods

    private UserDetails createUserDetails(String username) {
        Collection<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
        return new User(username, "password", authorities);
    }

    private String createTokenWithRoles(String username, List<String> roles) {
        Key signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION * 1000))
                .signWith(signingKey)
                .compact();
    }
}
