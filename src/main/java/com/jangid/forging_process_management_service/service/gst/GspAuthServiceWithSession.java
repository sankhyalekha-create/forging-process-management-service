package com.jangid.forging_process_management_service.service.gst;

import com.jangid.forging_process_management_service.dto.gst.gsp.EwayBillSessionCredentialsDTO;
import com.jangid.forging_process_management_service.dto.gst.gsp.EwayBillSessionTokenResponse;
import com.jangid.forging_process_management_service.dto.gst.gsp.GspAuthResponse;
import com.jangid.forging_process_management_service.entities.gst.TenantEwayBillCredentials;
import com.jangid.forging_process_management_service.repository.gst.TenantEwayBillCredentialsRepository;
import com.jangid.forging_process_management_service.service.security.EncryptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service for GSP Authentication with Session Support
 * Supports both session-based credentials and legacy stored credentials
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GspAuthServiceWithSession {

    private final TenantEwayBillCredentialsRepository credentialsRepository;
    private final RestTemplate restTemplate;
    private final EncryptionService encryptionService;
    private final GspServerFailoverHelper failoverHelper;
    private final EwayBillSessionService sessionService;

    @Value("${app.security.encryption.algorithm:AES}")
    private String encryptionAlgorithm;

    @Value("${app.eway-bill.gsp.auth-url}")
    private String authUrl;

    @Value("${app.eway-bill.gsp.session-timeout-hours:1}")
    private int sessionTimeoutHours;

    /**
     * Authenticate and create session using provided credentials
     * This is the NEW session-based authentication method
     * 
     * @param tenantId Tenant ID
     * @param sessionCredentials Credentials provided by user
     * @return Session token response
     */
    public EwayBillSessionTokenResponse authenticateWithSessionCredentials(
            Long tenantId, EwayBillSessionCredentialsDTO sessionCredentials) {
        
        // Check if session token is provided and valid
        if (sessionCredentials.getSessionToken() != null && 
            !sessionCredentials.getSessionToken().isEmpty()) {
            
            EwayBillSessionService.SessionData session = 
                sessionService.getSession(sessionCredentials.getSessionToken());
            
            if (session != null && session.getTenantId().equals(tenantId)) {
                // Check if session is expired or token needs renewal
                if (session.isExpired()) {
                    log.info("Session expired for tenant: {}, will re-authenticate", tenantId);
                    sessionService.invalidateSession(sessionCredentials.getSessionToken());
                } else if (session.needsTokenRenewal()) {
                    log.info("GSP auth token expiring soon for tenant: {}, will renew session", tenantId);
                    sessionService.invalidateSession(sessionCredentials.getSessionToken());
                } else {
                    log.debug("Using existing valid session for tenant: {}", tenantId);
                    return EwayBillSessionTokenResponse.builder()
                        .sessionToken(sessionCredentials.getSessionToken())
                        .authToken(session.getGspAuthToken())
                        .gstin(session.getGstin())
                        .expiresAt(session.getExpiresAt())
                        .createdAt(session.getCreatedAt())
                        .message("Existing session is valid")
                        .build();
                }
            }
        }

        // Get tenant configuration
        TenantEwayBillCredentials credentials = credentialsRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException(
                "E-Way Bill configuration not found for tenant: " + tenantId));

        if (!credentials.hasValidCredentials()) {
            throw new IllegalStateException(
                "E-Way Bill configuration incomplete or inactive for tenant: " + tenantId);
        }

        // Authenticate with GSP using provided credentials
        String gspAuthToken = authenticateWithGsp(
            credentials, 
            sessionCredentials.getEwbUsername(), 
            sessionCredentials.getEwbPassword()
        );

        // Create session with configured timeout and get session token
        String sessionToken = sessionService.createSession(
            tenantId,
            sessionCredentials.getEwbUsername(),
            encryptionService.encrypt(sessionCredentials.getEwbPassword()), // Store encrypted
            gspAuthToken,
            credentials.getEwbGstin(),
            sessionTimeoutHours
        );

        EwayBillSessionService.SessionData session = sessionService.getSession(sessionToken);

        return EwayBillSessionTokenResponse.builder()
            .sessionToken(sessionToken)
            .authToken(gspAuthToken)
            .gstin(credentials.getEwbGstin())
            .expiresAt(session.getExpiresAt())
            .createdAt(session.getCreatedAt())
            .message("Session created successfully")
            .build();
    }

    /**
     * Get valid auth token from session
     * If session token is invalid or expired, returns null
     * 
     * @param sessionToken Session token
     * @return GSP auth token or null
     */
    public String getAuthTokenFromSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isEmpty()) {
            return null;
        }

        EwayBillSessionService.SessionData session = sessionService.getSession(sessionToken);
        
        if (session == null) {
            return null;
        }

        return session.getGspAuthToken();
    }

    /**
     * Get session credentials for API calls
     * 
     * @param sessionToken Session token
     * @return SessionData or null if invalid
     */
    public EwayBillSessionService.SessionData getSessionData(String sessionToken) {
        return sessionService.getSession(sessionToken);
    }

    /**
     * Authenticate with GSP API using provided username and password
     * 
     * @param credentials Tenant credentials (for ASP and GSTIN)
     * @param ewbUsername E-Way Bill portal username
     * @param ewbPassword E-Way Bill portal password (plain text)
     * @return GSP auth token
     */
    private String authenticateWithGsp(TenantEwayBillCredentials credentials, 
                                       String ewbUsername, String ewbPassword) {
        String[] serverUrls = {authUrl};
        
        return failoverHelper.executeWithFailover(
            serverUrls,
            serverUrl -> attemptAuthentication(credentials, serverUrl, ewbUsername, ewbPassword),
            "E-Way Bill authentication for tenant: " + credentials.getTenant().getId()
        );
    }

    /**
     * Attempt authentication with a specific server URL
     * Modified to accept credentials as parameters instead of reading from entity
     */
    private String attemptAuthentication(TenantEwayBillCredentials credentials, 
                                        String serverAuthUrl,
                                        String ewbUsername,
                                        String ewbPassword) {
        try {
            // Validate that ASP credentials are configured
            if (credentials.getAspUserId() == null || credentials.getAspUserId().isEmpty()) {
                throw new IllegalStateException("ASP User ID not configured for tenant: " + 
                    credentials.getTenant().getId());
            }
            if (credentials.getAspPassword() == null || credentials.getAspPassword().isEmpty()) {
                throw new IllegalStateException("ASP Password not configured for tenant: " + 
                    credentials.getTenant().getId());
            }

            // Decrypt ASP password from database
            String aspId = credentials.getAspUserId();
            String aspPassword = encryptionService.decrypt(credentials.getAspPassword());

            // Build GET request URL with query parameters
            // ewbPassword is plain text from user input (transmitted over HTTPS)
            String authApiUrl = String.format(
                "%s?action=ACCESSTOKEN&aspid=%s&password=%s&gstin=%s&username=%s&ewbpwd=%s",
                serverAuthUrl,
                aspId,
                aspPassword,
                credentials.getEwbGstin(),
                ewbUsername,
                ewbPassword
            );
            
            log.debug("Calling GSP Auth API: {} for GSTIN: {}", serverAuthUrl, credentials.getEwbGstin());

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            // Call API using GET method
            ResponseEntity<GspAuthResponse> response = restTemplate.exchange(
                authApiUrl,
                HttpMethod.GET,
                request,
                GspAuthResponse.class
            );

            GspAuthResponse authResponse = response.getBody();

            if (authResponse == null || !"1".equals(authResponse.getStatus())) {
                String errorMsg = authResponse != null ? authResponse.getMessage() : "No response from GSP";
                log.error("GSP authentication failed: {}", errorMsg);
                throw new RuntimeException("E-Way Bill authentication failed: " + errorMsg);
            }

            log.info("Successfully authenticated for tenant: {}, GSTIN: {}", 
                     credentials.getTenant().getId(), credentials.getEwbGstin());

            return authResponse.getAuthtoken();

        } catch (Exception e) {
            log.error("Error during authentication attempt for tenant: {}", 
                     credentials.getTenant().getId(), e);
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Encrypt data using AES encryption
     */
    public String encryptData(String data, String key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, encryptionAlgorithm);
            
            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt data using AES encryption
     */
    public String decryptData(String encryptedData, String key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, encryptionAlgorithm);
            
            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }
}
