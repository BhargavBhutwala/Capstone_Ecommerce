package com.ebookstore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Generates and validates HS256 JWT tokens.
 *
 * <p>Token claims: {@code sub} (email), {@code iat}, {@code exp} only.
 * No {@code userId} or role claims are added — the database user id is
 * available through {@link AuthenticatedUser}, not the token.
 *
 * <p>Secret and expiry are read from application properties:
 * <ul>
 *   <li>{@code app.jwt.secret} — from environment variable {@code JWT_SECRET}</li>
 *   <li>{@code app.jwt.expiration-ms} — default 24 h</li>
 * </ul>
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT for the given principal.
     *
     * <p>Claims: {@code sub} = email, {@code iat} = now, {@code exp} = now + expirationMs.
     * No {@code userId} claim is added.
     */
    public String generateToken(AuthenticatedUser principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(principal.getUsername())   // sub = email
                .issuedAt(now)                      // iat
                .expiration(expiry)                 // exp
                .signWith(signingKey)               // HS256
                .compact();
    }

    /**
     * Extracts the email (subject) from a valid token.
     *
     * @throws JwtException if the token is malformed, expired, or tampered
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Returns {@code true} if the token is structurally valid, signed with the
     * correct secret, not expired, and its subject matches {@code userDetails.getUsername()}.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
