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
     * @brief Verifies non-obvious cryptographic invariants: IV randomness, GCM tag bit-flip detection, corrupted AES key failure, and invalid JCA digest.
     */
    @Test
    void testInvalidDecryptionThrows() {
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt("not-a-valid-base64"));
        EncryptionService fallbackSvc = new EncryptionService("");
        assertEquals("secret", fallbackSvc.decrypt(fallbackSvc.encrypt("secret")));
        EncryptionService nullKeySvc = new EncryptionService(null);
        assertEquals("secret", nullKeySvc.decrypt(nullKeySvc.encrypt("secret")));

        String c1 = encryptionService.encrypt("same-plaintext");
        String c2 = encryptionService.encrypt("same-plaintext");
        assertNotEquals(c1, c2, "AES-GCM with random 12-byte IV must produce distinct ciphertexts for identical plaintexts");
        assertEquals("same-plaintext", encryptionService.decrypt(c1));
        assertEquals("same-plaintext", encryptionService.decrypt(c2));

        byte[] rawCipher = java.util.Base64.getDecoder().decode(c1);
        rawCipher[rawCipher.length - 1] ^= 0x01;
        String tamperedCipher = java.util.Base64.getEncoder().encodeToString(rawCipher);
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt(tamperedCipher),
            "Flipping 1 bit in the 128-bit GCM authentication tag must fail decryption");

        EncryptionService corruptedKeySvc = new EncryptionService("valid-init-key");
        org.springframework.test.util.ReflectionTestUtils.setField(
            corruptedKeySvc, "secretKey", new javax.crypto.spec.SecretKeySpec(new byte[]{1, 2, 3}, "AES")
        );
        assertThrows(RuntimeException.class, () -> corruptedKeySvc.encrypt("payload"),
            "Invalid AES key length must cause encrypt() to throw RuntimeException");

        assertThrows(IllegalStateException.class,
            () -> new EncryptionService("secret", "INVALID-DIGEST-ALGO"));
    }
}
