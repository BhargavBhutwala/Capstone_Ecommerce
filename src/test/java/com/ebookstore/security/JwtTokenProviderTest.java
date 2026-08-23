package com.ebookstore.security;

import com.ebookstore.common.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtTokenProvider}.
 *
 * <p>No Spring context loaded — the provider is constructed directly with a
 * test secret and short expiration windows to verify expiry rejection.
 */
class JwtTokenProviderTest {

    private static final String TEST_SECRET =
            "test-secret-key-for-unit-tests-only-not-for-production-use-at-all";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    private JwtTokenProvider jwtTokenProvider;
    private AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, EXPIRATION_MS);
        principal = new AuthenticatedUser(42L, "alice@example.com", "hashed-password", UserRole.CUSTOMER);
    }

    // =========================================================================
    // Token generation
    // =========================================================================

    @Test
    void generateToken_returnsNonBlankToken() {
        String token = jwtTokenProvider.generateToken(principal);
        assertThat(token).isNotBlank();
    }

    @Test
    void generateToken_containsEmailAsSubject() {
        String token = jwtTokenProvider.generateToken(principal);
        String email = jwtTokenProvider.extractEmail(token);
        assertThat(email).isEqualTo("alice@example.com");
    }

    @Test
    void generateToken_containsIat() {
        String token = jwtTokenProvider.generateToken(principal);
        Claims claims = parseClaims(token, TEST_SECRET);
        assertThat(claims.getIssuedAt()).isNotNull();
    }

    @Test
    void generateToken_containsExp() {
        String token = jwtTokenProvider.generateToken(principal);
        Claims claims = parseClaims(token, TEST_SECRET);
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void generateToken_doesNotContainUserId() {
        String token = jwtTokenProvider.generateToken(principal);
        Claims claims = parseClaims(token, TEST_SECRET);
        // No userId, no role, no custom business claims — only sub/iat/exp
        assertThat(claims.get("userId")).isNull();
        assertThat(claims.get("id")).isNull();
        assertThat(claims.get("role")).isNull();
        assertThat(claims.get("roles")).isNull();
    }

    // =========================================================================
    // Token validation
    // =========================================================================

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        String token = jwtTokenProvider.generateToken(principal);
        assertThat(jwtTokenProvider.isTokenValid(token, principal)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() throws InterruptedException {
        // Create a provider with 1 ms expiration
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(TEST_SECRET, 1L);
        String token = shortLivedProvider.generateToken(principal);
        // Let the token expire
        Thread.sleep(50);
        assertThat(shortLivedProvider.isTokenValid(token, principal)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        String token = jwtTokenProvider.generateToken(principal);
        // Flip a character in the signature segment
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(jwtTokenProvider.isTokenValid(tampered, principal)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForWrongSecretToken() {
        // Generate a token with a different secret
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "completely-different-secret-key-for-wrong-secret-test-padding",
                EXPIRATION_MS);
        String token = otherProvider.generateToken(principal);
        // Validate with the original provider — must reject
        assertThat(jwtTokenProvider.isTokenValid(token, principal)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForGarbageToken() {
        assertThat(jwtTokenProvider.isTokenValid("not.a.token", principal)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForWrongSubject() {
        String token = jwtTokenProvider.generateToken(principal);
        AuthenticatedUser other = new AuthenticatedUser(99L, "bob@example.com", "hash", UserRole.CUSTOMER);
        assertThat(jwtTokenProvider.isTokenValid(token, other)).isFalse();
    }

    // =========================================================================
    // Helper — parse claims independently to verify token contents
    // =========================================================================

    private Claims parseClaims(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
