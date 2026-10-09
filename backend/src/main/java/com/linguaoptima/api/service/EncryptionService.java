/**
 * @file EncryptionService.java
 * @brief Authenticated symmetric encryption service using AES-256-GCM.
 */
package com.linguaoptima.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * @brief Authenticated symmetric encryption service using AES-256-GCM.
 *
 * Employs 12-byte initialization vectors and 128-bit authentication tags to ensure
 * confidentiality and cryptographic integrity of user credentials at rest.
 */
@Service
public class EncryptionService {

    /** @brief Constant or enum value representing algorithm in EncryptionService. */
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    /** @brief Constant or enum value representing gcm tag length in EncryptionService. */
    private static final int GCM_TAG_LENGTH = 128;
    /** @brief Constant or enum value representing iv length in EncryptionService. */
    private static final int IV_LENGTH = 12;
    /** @brief Field representing secret key in EncryptionService. */
    private final SecretKey secretKey;

    /**
     * @brief Constructs an EncryptionService instance initializing a 256-bit AES secret key.
     * @param secretKeyString Configured master encryption secret string from application properties.
     * @throws IllegalStateException if key initialization fails.
     */
    public EncryptionService(@Value("${app.encryption.key:lingua-optima-default-secure-key-32b}") String secretKeyString) {
        try {
            String effectiveKey = (secretKeyString == null || secretKeyString.isBlank())
                ? "lingua-optima-default-secure-key-32b"
                : secretKeyString;
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(effectiveKey.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize EncryptionService key", e);
        }
    }

    /**
     * @brief Encrypts plaintext into a Base64-encoded string containing both IV and ciphertext.
     * @param plainText The plaintext string to encrypt.
     * @return Base64-encoded payload (12-byte IV + ciphertext with GCM tag), or null if input is null.
     */
    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * @brief Decrypts a Base64-encoded string containing IV and ciphertext.
     * @param encryptedText Base64-encoded payload containing 12-byte IV and ciphertext.
     * @return Decrypted plaintext string, or null if input is null.
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
