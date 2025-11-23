//package com.jangid.forging_process_management_service.service.gst;
//
//import com.jangid.forging_process_management_service.dto.gst.gsp.GspAuthResponse;
//import com.jangid.forging_process_management_service.entities.gst.TenantEwayBillCredentials;
//import com.jangid.forging_process_management_service.repository.gst.TenantEwayBillCredentialsRepository;
//import com.jangid.forging_process_management_service.service.security.EncryptionService;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import javax.crypto.Cipher;
//import javax.crypto.spec.SecretKeySpec;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.util.Base64;
//
///**
// * Service for GSP Authentication
// * Handles token generation and encryption/decryption
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class GspAuthServiceBackup {
//
//    private final TenantEwayBillCredentialsRepository credentialsRepository;
//    private final RestTemplate restTemplate;
//    private final EncryptionService encryptionService;
//    private final GspServerFailoverHelper failoverHelper;
//
//    @Value("${app.security.encryption.algorithm:AES}")
//    private String encryptionAlgorithm;
//
//    @Value("${app.eway-bill.gsp.auth-url}")
//    private String authUrl;
//
//    @Value("${app.eway-bill.gsp.backup1-auth-url:}")
//    private String backup1AuthUrl;
//
//    @Value("${app.eway-bill.gsp.backup2-auth-url:}")
//    private String backup2AuthUrl;
//
//    /**
//     * Get valid auth token for tenant (auto-refresh if expired)
//     */
//    public String getValidAuthToken(Long tenantId) {
//        TenantEwayBillCredentials credentials = credentialsRepository.findByTenantId(tenantId)
//            .orElseThrow(() -> new IllegalArgumentException("E-Way Bill credentials not configured for tenant: " + tenantId));
//
//        if (!credentials.hasValidCredentials()) {
//            throw new IllegalStateException("E-Way Bill credentials are incomplete or inactive");
//        }
//
//        // Check if token is still valid
//        if (credentials.isTokenValid()) {
//            log.debug("Using existing valid token for tenant: {}", tenantId);
//            return credentials.getAuthToken();
//        }
//
//        // Token expired or not exists, get new token
//        log.info("Token expired or missing for tenant: {}. Fetching new token...", tenantId);
//        return refreshAuthToken(credentials);
//    }
//
//    /**
//     * Refresh auth token by calling GSP API
//     */
//    public String refreshAuthToken(Long tenantId) {
//        TenantEwayBillCredentials credentials = credentialsRepository.findByTenantId(tenantId)
//            .orElseThrow(() -> new IllegalArgumentException("E-Way Bill credentials not configured for tenant: " + tenantId));
//        return refreshAuthToken(credentials);
//    }
//
//    /**
//     * Refresh auth token by calling GSP API (internal method)
//     * Using new GET-based API format with automatic failover via helper
//     */
//    private String refreshAuthToken(TenantEwayBillCredentials credentials) {
//        String[] serverUrls = {authUrl, backup1AuthUrl, backup2AuthUrl};
//
//        return failoverHelper.executeWithFailover(
//            serverUrls,
//            serverUrl -> attemptAuthentication(credentials, serverUrl),
//            "E-Way Bill authentication for tenant: " + credentials.getTenant().getId()
//        );
//    }
//
//    /**
//     * Attempt authentication with a specific server URL
//     */
//    private String attemptAuthentication(TenantEwayBillCredentials credentials, String serverAuthUrl) {
//        try {
//            // Validate that ASP credentials are configured in database
//            if (credentials.getAspUserId() == null || credentials.getAspUserId().isEmpty()) {
//                throw new IllegalStateException("ASP User ID not configured for tenant: " + credentials.getTenant().getId());
//            }
//            if (credentials.getAspPassword() == null || credentials.getAspPassword().isEmpty()) {
//                throw new IllegalStateException("ASP Password not configured for tenant: " + credentials.getTenant().getId());
//            }
//
//            // Decrypt ASP password from database
//            String aspId = credentials.getAspUserId();
//            String aspPassword = encryptionService.decrypt(credentials.getAspPassword());
//            String tenantEwbPassword = encryptionService.decrypt(credentials.getEwbPassword());
//
//            // Build GET request URL with query parameters
//            // Format: GET /auth?action=ACCESSTOKEN&aspid=<aspid>&password=<asppwd>&gstin=<gstin>&username=<user>&ewbpwd=<pwd>
//            String authApiUrl = String.format(
//                "%s?action=ACCESSTOKEN&aspid=%s&password=%s&gstin=%s&username=%s&ewbpwd=%s",
//                serverAuthUrl,
//                aspId,
//                aspPassword,
//                credentials.getEwbGstin(),
//                credentials.getEwbUsername(),
//                tenantEwbPassword
//            );
//
//            log.debug("Calling GSP Auth API: {} for GSTIN: {}",
//                     serverAuthUrl, credentials.getEwbGstin());
//
//            // Set headers
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            HttpEntity<Void> request = new HttpEntity<>(headers);
//
//            // Call API using GET method
//            ResponseEntity<GspAuthResponse> response = restTemplate.exchange(
//                authApiUrl,
//                HttpMethod.GET,
//                request,
//                GspAuthResponse.class
//            );
//
//            GspAuthResponse authResponse = response.getBody();
//
//            if (authResponse == null || !"1".equals(authResponse.getStatus())) {
//                String errorMsg = authResponse != null ? authResponse.getMessage() : "No response from GSP";
//                log.error("GSP authentication failed: {}", errorMsg);
//                throw new RuntimeException("E-Way Bill authentication failed: " + errorMsg);
//            }
//
//            // Update credentials with new token
//            credentials.setAuthToken(authResponse.getAuthtoken());
//
//            // SEK is returned as empty string in v1.03, store if provided
//            if (authResponse.getSek() != null && !authResponse.getSek().isEmpty()) {
//                credentials.setSek(authResponse.getSek());
//            }
//
//            // Set token expiry (typically 6 hours from now)
//            LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(1);
//            credentials.setTokenExpiry(tokenExpiry);
//
//            // Save updated credentials
//            credentialsRepository.save(credentials);
//
//            log.info("Successfully refreshed auth token for tenant: {}. Valid until: {}",
//                     credentials.getTenant().getId(), tokenExpiry);
//
//            return authResponse.getAuthtoken();
//
//        } catch (Exception e) {
//            log.error("Error during authentication attempt for tenant: {}",
//                     credentials.getTenant().getId(), e);
//            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Encrypt data using AES encryption
//     */
//    public String encryptData(String data, String key) {
//        try {
//            byte[] keyBytes = Base64.getDecoder().decode(key);
//            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, encryptionAlgorithm);
//
//            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
//            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
//
//            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
//            return Base64.getEncoder().encodeToString(encryptedBytes);
//        } catch (Exception e) {
//            log.error("Encryption failed", e);
//            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Decrypt data using AES encryption
//     */
//    public String decryptData(String encryptedData, String key) {
//        try {
//            byte[] keyBytes = Base64.getDecoder().decode(key);
//            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, encryptionAlgorithm);
//
//            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
//            cipher.init(Cipher.DECRYPT_MODE, secretKey);
//
//            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
//            return new String(decryptedBytes, StandardCharsets.UTF_8);
//        } catch (Exception e) {
//            log.error("Decryption failed", e);
//            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Test credentials by attempting authentication
//     */
//    public boolean testCredentials(Long tenantId) {
//        try {
//            TenantEwayBillCredentials credentials = credentialsRepository.findByTenantId(tenantId)
//                .orElseThrow(() -> new IllegalArgumentException("Credentials not found for tenant: " + tenantId));
//
//            String token = refreshAuthToken(credentials);
//            return token != null && !token.isEmpty();
//        } catch (Exception e) {
//            log.error("Credentials test failed for tenant: {}", tenantId, e);
//            return false;
//        }
//    }
//}
