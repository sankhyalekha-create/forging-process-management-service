package com.jangid.forging_process_management_service.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

/**
 * Standalone utility class for encrypting and decrypting sensitive data
 * Uses the same encryption logic as EncryptionService
 * 
 * Can be run as a command-line tool:
 * - To encrypt: java EncryptionUtil encrypt <plainText> <masterKey>
 * - To decrypt: java EncryptionUtil decrypt <cipherText> <masterKey>
 * - Interactive mode: java EncryptionUtil
 * 
 * Usage in code:
 * <pre>
 * String masterKey = "FOPMAS_PROD_MASTER_KEY";
 * 
 * // Encrypt
 * String encrypted = EncryptionUtil.encrypt("sdfsdfsf", masterKey);
 * 
 * // Decrypt
 * String decrypted = EncryptionUtil.decrypt(encrypted, masterKey);
 * </pre>
 */
@Slf4j
public class EncryptionUtil {

    private static final String DEFAULT_ALGORITHM = "AES";

    /**
     * Encrypt plain text using AES encryption
     *
     * @param plainText The plain text to encrypt
     * @param masterKey The master encryption key
     * @return Base64 encoded encrypted string
     */
    public static String encrypt(String plainText, String masterKey) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            SecretKeySpec secretKey = generateSecretKey(masterKey);
            Cipher cipher = Cipher.getInstance(DEFAULT_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (Exception e) {
            System.err.println("Failed to encrypt data: " + e.getMessage());
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt encrypted text using AES decryption
     *
     * @param encryptedText The encrypted text (Base64 encoded)
     * @param masterKey The master encryption key (must be same as used for encryption)
     * @return Decrypted plain text
     */
    public static String decrypt(String encryptedText, String masterKey) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            SecretKeySpec secretKey = generateSecretKey(masterKey);
            Cipher cipher = Cipher.getInstance(DEFAULT_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            System.err.println("Failed to decrypt data: " + e.getMessage());
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate AES secret key from master key
     * Uses SHA-256 to create a 256-bit key from the master key string
     */
    private static SecretKeySpec generateSecretKey(String masterKey) {
        try {
            byte[] key = masterKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // Use only first 128 bits for AES-128
            return new SecretKeySpec(key, DEFAULT_ALGORITHM);
        } catch (Exception e) {
            System.err.println("Failed to generate secret key: " + e.getMessage());
            throw new RuntimeException("Key generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Main method for command-line usage
     * 
     * Usage:
     * 1. Encrypt: java EncryptionUtil encrypt "plainText" "masterKey"
     * 2. Decrypt: java EncryptionUtil decrypt "cipherText" "masterKey"
     * 3. Interactive: java EncryptionUtil
     */
    public static void main(String[] args) {
        if (args.length == 3) {
            // Command line mode
            String operation = args[0].toLowerCase();
            String text = args[1];
            String masterKey = args[2];

            try {
                if ("encrypt".equals(operation)) {
                    String encrypted = encrypt(text, masterKey);
                    System.out.println("\n=== ENCRYPTION RESULT ===");
                    System.out.println("Plain Text: " + text);
                    System.out.println("Encrypted:  " + encrypted);
                    System.out.println("========================\n");
                } else if ("decrypt".equals(operation)) {
                    String decrypted = decrypt(text, masterKey);
                    System.out.println("\n=== DECRYPTION RESULT ===");
                    System.out.println("Encrypted:  " + text);
                    System.out.println("Decrypted:  " + decrypted);
                    System.out.println("========================\n");
                } else {
                    printUsage();
                }
            } catch (Exception e) {
                System.err.println("\nError: " + e.getMessage());
                System.exit(1);
            }
        } else {
            // Interactive mode
            runInteractiveMode();
        }
    }

    /**
     * Run interactive mode with menu
     */
    private static void runInteractiveMode() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║         FOPMAS Encryption/Decryption Utility          ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        while (true) {
            System.out.println("Select an option:");
            System.out.println("  1. Encrypt text");
            System.out.println("  2. Decrypt text");
            System.out.println("  3. Encrypt ASP Password (Sandbox)");
            System.out.println("  4. Exit");
            System.out.print("\nEnter choice (1-4): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    encryptInteractive(scanner);
                    break;
                case "2":
                    decryptInteractive(scanner);
                    break;
                case "3":
                    System.out.println("\nGoodbye!");
                    scanner.close();
                    return;
                default:
                    System.out.println("\nInvalid choice. Please try again.\n");
            }
        }
    }

    /**
     * Interactive encryption
     */
    private static void encryptInteractive(Scanner scanner) {
        System.out.print("\nEnter text to encrypt: ");
        String plainText = scanner.nextLine();

        System.out.print("Enter master encryption key: ");
        String masterKey = scanner.nextLine();

        if (masterKey.isEmpty()) {
            masterKey = "FOPMAS_DEFAULT_MASTER_KEY_CHANGE_IN_PRODUCTION";
            System.out.println("(Using default master key)");
        }

        try {
            String encrypted = encrypt(plainText, masterKey);
            System.out.println("\n╔════════════════════════════════════════════════════════╗");
            System.out.println("║                  ENCRYPTION RESULT                     ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            System.out.println("Plain Text:  " + plainText);
            System.out.println("Encrypted:   " + encrypted);
            System.out.println("\n✓ Copy the encrypted text above to use in your database");
            System.out.println("════════════════════════════════════════════════════════\n");
        } catch (Exception e) {
            System.err.println("\n✗ Encryption failed: " + e.getMessage() + "\n");
        }
    }

    /**
     * Interactive decryption
     */
    private static void decryptInteractive(Scanner scanner) {
        System.out.print("\nEnter encrypted text to decrypt: ");
        String encryptedText = scanner.nextLine();

        System.out.print("Enter master encryption key: ");
        String masterKey = scanner.nextLine();

        if (masterKey.isEmpty()) {
            masterKey = "FOPMAS_DEFAULT_MASTER_KEY_CHANGE_IN_PRODUCTION";
            System.out.println("(Using default master key)");
        }

        try {
            String decrypted = decrypt(encryptedText, masterKey);
            System.out.println("\n╔════════════════════════════════════════════════════════╗");
            System.out.println("║                  DECRYPTION RESULT                     ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            System.out.println("Encrypted:   " + encryptedText);
            System.out.println("Decrypted:   " + decrypted);
            System.out.println("════════════════════════════════════════════════════════\n");
        } catch (Exception e) {
            System.err.println("\n✗ Decryption failed: " + e.getMessage() + "\n");
        }
    }

    /**
     * Print usage instructions
     */
    private static void printUsage() {
        System.out.println("\nUsage:");
        System.out.println("  java EncryptionUtil encrypt <plainText> <masterKey>");
        System.out.println("  java EncryptionUtil decrypt <cipherText> <masterKey>");
        System.out.println("  java EncryptionUtil  (for interactive mode)");
        System.out.println("\nExamples:");
        System.out.println("  # Encrypt");
        System.out.println("  java EncryptionUtil encrypt \"Kriyansh@190221\" \"FOPMAS_MASTER_KEY\"");
        System.out.println("\n  # Decrypt");
        System.out.println("  java EncryptionUtil decrypt \"<base64_encrypted>\" \"FOPMAS_MASTER_KEY\"");
        System.out.println();
    }
}
