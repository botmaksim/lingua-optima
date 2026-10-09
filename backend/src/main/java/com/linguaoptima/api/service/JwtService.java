/**
 * @file JwtService.java
 * @brief Service responsible for JSON Web Token (JWT) generation, claims extraction, and validation.
 */
package com.linguaoptima.api.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * @brief Service responsible for JSON Web Token (JWT) generation, claims extraction, and validation.
 */
@Service
public class JwtService {

    /** @brief Field representing signing key in JwtService. */
    private final SecretKey signingKey;
    /** @brief Field representing access token validity ms in JwtService. */
    private final long accessTokenValidityMs = 15 * 60 * 1000L;
    /** @brief Field representing refresh token validity ms in JwtService. */
    private final long refreshTokenValidityMs = 30L * 24 * 60 * 60 * 1000L;

    /**
     * @brief Constructs a JwtService instance configured with a SHA-256 derived HMAC signing key.
     * @param secret The configured JWT secret key string from application properties.
     * @throws IllegalStateException if key initialization fails.
     */
    public JwtService(@Value("${app.jwt.secret:lingua-optima-super-secret-jwt-signing-key-for-auth-256}") String secret) {
        this(secret, "SHA-256");
    }

    /**
     * @brief Constructs a JwtService instance with an explicit message digest algorithm for key derivation.
     * @param secret The configured JWT secret key string.
     * @param digestAlgorithm The JCA message digest algorithm name (e.g. SHA-256).
     * @throws IllegalStateException if the digest algorithm is unavailable or key initialization fails.
     */
    JwtService(String secret, String digestAlgorithm) {
        try {
            String effectiveSecret = (secret == null || secret.isBlank())
                ? "lingua-optima-super-secret-jwt-signing-key-for-auth-256"
                : secret;
            MessageDigest sha = MessageDigest.getInstance(digestAlgorithm);
            byte[] keyBytes = sha.digest(effectiveSecret.getBytes(StandardCharsets.UTF_8));
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure JwtService signing key", e);
        }
    }

    /**
     * @brief Generates a signed short-lived JWT access token (15-minute validity).
     * @param userId The unique identifier of the authenticated user.
     * @param email The email address of the authenticated user.
     * @param role The role assigned to the user (e.g. STUDENT, TEACHER, ADMIN).
     * @return Compact serialized JWT access token string.
     */
    public String generateAccessToken(UUID userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("role", role);
        return buildToken(claims, email, accessTokenValidityMs);
    }

    /**
     * @brief Generates a signed long-lived JWT refresh token (30-day validity).
     * @param userId The unique identifier of the authenticated user.
     * @param email The email address of the authenticated user.
     * @return Compact serialized JWT refresh token string.
     */
    public String generateRefreshToken(UUID userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("type", "REFRESH");
        return buildToken(claims, email, refreshTokenValidityMs);
    }

    /**
     * @brief Builds and signs a compact JWT string with the specified claims, subject, and expiration.
     * @param extraClaims Additional key-value claims included in the JWT payload.
     * @param subject The subject (user email) for the token.
     * @param expirationMs Token validity duration in milliseconds.
     * @return Serialized JWT token string.
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
            .claims(extraClaims)
            .subject(subject)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(signingKey)
            .compact();
    }

    /**
     * @brief Extracts the email subject from a signed JWT token.
     * @param token Compact serialized JWT token.
     * @return The user email extracted from the token subject.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * @brief Extracts the user UUID claim from a signed JWT token.
     * @param token Compact serialized JWT token.
     * @return The UUID of the user, or null if absent.
     */
    public UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * @brief Extracts the user role claim from a signed JWT token.
     * @param token Compact serialized JWT token.
     * @return The role name as a string.
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * @brief Resolves an arbitrary claim from a token using a functional resolver.
     * @param token Compact serialized JWT token.
     * @param claimsResolver Function to resolve the target claim from Claims payload.
     * @return The resolved claim value.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * @brief Checks whether the given token has a valid signature, is unexpired, and contains an expiration claim.
     * @param token Compact serialized JWT token.
     * @return True if signature is valid and a future expiration claim is present; false otherwise.
     */
    public boolean isTokenValid(String token) {
        try {
            return extractExpiration(token) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @brief Extracts the expiration timestamp from a token.
     * @param token Compact serialized JWT token.
     * @return Expiration Date object.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * @brief Parses and cryptographically validates all claims from a JWT token.
     * @param token Compact serialized JWT token.
     * @return Claims payload.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * @brief Returns the configured access token validity duration in milliseconds.
     * @return Access token lifetime in ms (900,000 ms = 15 minutes).
     */
    public long getAccessTokenValidityMs() {
        return accessTokenValidityMs;
    }

    /**
     * @brief Returns the configured refresh token validity duration in milliseconds.
     * @return Refresh token lifetime in ms (2,592,000,000 ms = 30 days).
     */
    public long getRefreshTokenValidityMs() {
        return refreshTokenValidityMs;
    }
}
