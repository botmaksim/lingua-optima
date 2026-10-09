/**
 * @file JwtServiceTest.java
 * @brief Unit and slice test suite for JwtService.
 */
package com.linguaoptima.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @brief Unit and slice test suite for JwtService.
 */
class JwtServiceTest {

    /** @brief Test fixture or mock dependency for jwt service. */
    private JwtService jwtService;
    /** @brief Test fixture or mock dependency for test user id. */
    private final UUID testUserId = UUID.randomUUID();
    /** @brief Test fixture or mock dependency for test email. */
    private final String testEmail = "test@example.com";
    /** @brief Test fixture or mock dependency for test role. */
    private final String testRole = "STUDENT";

    /**
     * @brief Initializes test fixtures and mock state before each test in JwtServiceTest.
     */
    @BeforeEach
    void setUp() {
        jwtService = new JwtService("a-super-secret-key-that-is-at-least-256-bits-long-for-signing-jwt-tokens-safely");
    }

    /**
     * @brief Verifies unit test scenario: generate and validate access token.
     */
    @Test
    void testGenerateAndValidateAccessToken() {
        String token = jwtService.generateAccessToken(testUserId, testEmail, testRole);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals(testEmail, jwtService.extractEmail(token));
        assertEquals(testUserId, jwtService.extractUserId(token));
        assertEquals(testRole, jwtService.extractRole(token));
    }

    /**
     * @brief Verifies unit test scenario: generate refresh token.
     */
    @Test
    void testGenerateRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(testUserId, testEmail);
        assertNotNull(refreshToken);
        assertTrue(jwtService.isTokenValid(refreshToken));
        assertEquals(testEmail, jwtService.extractEmail(refreshToken));
        assertEquals(testUserId, jwtService.extractUserId(refreshToken));
    }

    /**
     * @brief Verifies unit test scenario: invalid token.
     */
    @Test
    void testInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.token.string"));
    }

    /**
     * @brief Verifies unit test scenario: token expiration getters and blank secret fallback.
     */
    @Test
    void testTokenExpirationGetters() {
        assertEquals(15 * 60 * 1000L, jwtService.getAccessTokenValidityMs());
        assertEquals(30L * 24 * 60 * 60 * 1000L, jwtService.getRefreshTokenValidityMs());
        String access = jwtService.generateAccessToken(testUserId, testEmail, testRole);
        String refresh = jwtService.generateRefreshToken(testUserId, testEmail);
        assertNull(jwtService.extractClaim(access, c -> c.get("type", String.class)));
        assertEquals("REFRESH", jwtService.extractClaim(refresh, c -> c.get("type", String.class)));

        JwtService fallbackJwt = new JwtService("   ");
        assertTrue(fallbackJwt.isTokenValid(fallbackJwt.generateAccessToken(testUserId, testEmail, testRole)));
        JwtService nullJwt = new JwtService(null);
        assertTrue(nullJwt.isTokenValid(nullJwt.generateAccessToken(testUserId, testEmail, testRole)));
    }
}
