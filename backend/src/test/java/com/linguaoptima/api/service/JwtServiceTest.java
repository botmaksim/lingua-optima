package com.linguaoptima.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final UUID testUserId = UUID.randomUUID();
    private final String testEmail = "test@example.com";
    private final String testRole = "STUDENT";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("a-super-secret-key-that-is-at-least-256-bits-long-for-signing-jwt-tokens-safely");
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        String token = jwtService.generateAccessToken(testUserId, testEmail, testRole);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals(testEmail, jwtService.extractEmail(token));
        assertEquals(testUserId, jwtService.extractUserId(token));
        assertEquals(testRole, jwtService.extractRole(token));
    }

    @Test
    void testGenerateRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(testUserId, testEmail);
        assertNotNull(refreshToken);
        assertTrue(jwtService.isTokenValid(refreshToken));
        assertEquals(testEmail, jwtService.extractEmail(refreshToken));
        assertEquals(testUserId, jwtService.extractUserId(refreshToken));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.token.string"));
    }

    @Test
    void testTokenExpirationGetters() {
        assertEquals(15 * 60 * 1000L, jwtService.getAccessTokenValidityMs());
        assertEquals(30L * 24 * 60 * 60 * 1000L, jwtService.getRefreshTokenValidityMs());
    }
}
