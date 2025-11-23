package com.jangid.forging_process_management_service.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data
 * Uses AES encryption with a master key from configuration
 */
@Service
@Slf4j
public class EncryptionService {

    @Value("${app.security.encryption.master-key}")
    private String masterKey;

    @Value("${app.security.encryption.algorithm:AES}")
    private String algorithm;

    /**
     * Encrypt sensitive data (e.g., passwords, tokens)
     *
     * @param plainText The plain text to encrypt
     * @return Base64 encoded encrypted string
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            SecretKeySpec secretKey = generateSecretKey();
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (Exception e) {
            log.error("Failed to encrypt data", e);
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt encrypted data
     *
     * @param encryptedText The encrypted text (Base64 encoded)
     * @return Decrypted plain text
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            SecretKeySpec secretKey = generateSecretKey();
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Failed to decrypt data", e);
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate AES secret key from master key
     * Uses SHA-256 to create a 256-bit key from the master key string
     */
    private SecretKeySpec generateSecretKey() {
        try {
            byte[] key = masterKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // Use only first 128 bits for AES-128
            return new SecretKeySpec(key, algorithm);
        } catch (Exception e) {
            log.error("Failed to generate secret key", e);
            throw new RuntimeException("Key generation failed: " + e.getMessage(), e);
        }
    }
}
