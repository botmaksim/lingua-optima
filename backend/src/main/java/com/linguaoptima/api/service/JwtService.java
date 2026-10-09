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
 * @file JwtService.java
 * @brief Service responsible for JSON Web Token (JWT) generation, claims extraction, and validation.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenValidityMs = 15 * 60 * 1000L;
    private final long refreshTokenValidityMs = 30L * 24 * 60 * 60 * 1000L;

    /**
     * @brief Constructs a JwtService instance configured with a HMAC-SHA signing key.
     * @param secret The configured JWT secret key string from application properties.
     * @throws IllegalStateException if key initialization fails.
     */
    public JwtService(@Value("${app.jwt.secret:lingua-optima-super-secret-jwt-signing-key-for-auth-256}") String secret) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
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
     * @param <T> Expected type of the extracted claim.
     * @param token Compact serialized JWT token.
     * @param claimsResolver Function to resolve the target claim from Claims payload.
     * @return The resolved claim value.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * @brief Checks whether the given token is valid and unexpired.
     * @param token Compact serialized JWT token.
     * @return True if signature is valid and expiration date is in the future; false otherwise.
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @brief Checks if a token has passed its expiration timestamp.
     * @param token Compact serialized JWT token.
     * @return True if token has expired; false otherwise.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
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
