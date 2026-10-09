/**
 * @file EncryptionServiceTest.java
 * @brief Unit and slice test suite for EncryptionService.
 */
package com.linguaoptima.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @brief Unit and slice test suite for EncryptionService.
 */
class EncryptionServiceTest {

    /** @brief Test fixture or mock dependency for encryption service. */
    private EncryptionService encryptionService;

    /**
     * @brief Initializes test fixtures and mock state before each test in EncryptionServiceTest.
     */
    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService("test-key-32-bytes-long-super-secret!");
    }

    /**
     * @brief Verifies unit test scenario: encrypt and decrypt.
     */
    @Test
    void testEncryptAndDecrypt() {
        String plainText = "sk-test-ai-key-1234567890";
        String encrypted = encryptionService.encrypt(plainText);

        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    /**
     * @brief Verifies unit test scenario: null handling.
     */
    @Test
    void testNullHandling() {
        assertNull(encryptionService.encrypt(null));
        assertNull(encryptionService.decrypt(null));
    }

    /**
     * @brief Verifies unit test scenario: invalid decryption throws and blank key fallback.
     */
    @Test
    void testInvalidDecryptionThrows() {
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt("not-a-valid-base64"));
        EncryptionService fallbackSvc = new EncryptionService("");
        assertEquals("secret", fallbackSvc.decrypt(fallbackSvc.encrypt("secret")));
    }
}
