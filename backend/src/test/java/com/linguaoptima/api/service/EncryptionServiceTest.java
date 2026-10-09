package com.linguaoptima.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @file EncryptionServiceTest.java
 * @brief Unit and slice test suite for EncryptionService.
 */
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService("test-key-32-bytes-long-super-secret!");
    }

    @Test
    void testEncryptAndDecrypt() {
        String plainText = "sk-test-ai-key-1234567890";
        String encrypted = encryptionService.encrypt(plainText);

        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testNullHandling() {
        assertNull(encryptionService.encrypt(null));
        assertNull(encryptionService.decrypt(null));
    }

    @Test
    void testInvalidDecryptionThrows() {
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt("not-a-valid-base64"));
    }
}
