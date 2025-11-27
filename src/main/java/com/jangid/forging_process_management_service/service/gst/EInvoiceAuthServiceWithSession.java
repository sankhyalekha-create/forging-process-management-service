package com.jangid.forging_process_management_service.service.gst;

import com.jangid.forging_process_management_service.dto.gst.einvoice.EInvoiceAuthResponse;
import com.jangid.forging_process_management_service.dto.gst.einvoice.EInvoiceSessionCredentialsDTO;
import com.jangid.forging_process_management_service.dto.gst.gsp.EwayBillSessionTokenResponse;
import com.jangid.forging_process_management_service.entities.gst.TenantEInvoiceCredentials;
import com.jangid.forging_process_management_service.repository.gst.TenantEInvoiceCredentialsRepository;
import com.jangid.forging_process_management_service.service.security.EncryptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for E-Invoice Authentication with Session Support
 * Supports session-based credentials (not stored in database)
 * Similar to GspAuthServiceWithSession but for E-Invoice
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EInvoiceAuthServiceWithSession {

    private final TenantEInvoiceCredentialsRepository credentialsRepository;
    private final EncryptionService encryptionService;
    private final EInvoiceSessionService sessionService;
    private final RestTemplate restTemplate;
    private final GspServerFailoverHelper failoverHelper;

    @Value("${app.einvoice.gsp.auth-url}")
    private String authUrl;

    private static final DateTimeFormatter TOKEN_EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Authenticate and create session using provided credentials
     * This is the session-based authentication method
     * 
     * @param tenantId Tenant ID
     * @param sessionCredentials Credentials provided by user
     * @return Session token response
     */
    public EwayBillSessionTokenResponse authenticateWithSessionCredentials(
            Long tenantId, EInvoiceSessionCredentialsDTO sessionCredentials) {
        
        // Check if session token is provided and valid
        if (sessionCredentials.getSessionToken() != null && 
            !sessionCredentials.getSessionToken().isEmpty()) {
            
            EInvoiceSessionService.SessionData session = 
                sessionService.getSession(sessionCredentials.getSessionToken());
            
            if (session != null && session.getTenantId().equals(tenantId)) {
                // Check if session is expired or GSP auth token needs renewal
                if (session.isExpired()) {
                    log.info("E-Invoice session expired for tenant: {}, will re-authenticate. " +
                            "Current expiry: {}", tenantId, session.getExpiresAt());
                    sessionService.invalidateSession(sessionCredentials.getSessionToken());
                } else if (session.needsTokenRenewal()) {
                    log.info("E-Invoice GSP auth token expiring soon for tenant: {}, will renew session. " +
                            "Current expiry: {}", tenantId, session.getGspTokenExpiry());
                    sessionService.invalidateSession(sessionCredentials.getSessionToken());
                } else {
                    // Session is valid and token is not expiring soon
                    log.debug("Using existing valid E-Invoice session for tenant: {}, expires at: {}", 
                             tenantId, session.getExpiresAt());
                    return EwayBillSessionTokenResponse.builder()
                        .sessionToken(sessionCredentials.getSessionToken())
                        .authToken(session.getGspAuthToken())
                        .gstin(session.getGstin())
                        .expiresAt(session.getExpiresAt())
                        .createdAt(session.getCreatedAt())
                        .message("Existing session is valid")
                        .build();
                }
            } else {
                log.debug("E-Invoice session not found or expired for tenant: {}, creating new session", tenantId);
            }
        }

        // Get tenant configuration
        TenantEInvoiceCredentials credentials = credentialsRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException(
                "E-Invoice configuration not found for tenant: " + tenantId));

        if (!credentials.hasValidCredentials()) {
            throw new IllegalStateException(
                "E-Invoice configuration incomplete or inactive for tenant: " + tenantId);
        }

        // Authenticate with GSP using provided credentials
        EInvoiceAuthResponse authResponse = authenticateWithGsp(
            credentials,
            sessionCredentials.getEinvUsername(),
            sessionCredentials.getEinvPassword()
        );

        // Parse token expiry from GSP response
        LocalDateTime tokenExpiry = null;
        if (authResponse.getTokenExpiry() != null && !authResponse.getTokenExpiry().isEmpty()) {
            try {
                tokenExpiry = LocalDateTime.parse(authResponse.getTokenExpiry(), TOKEN_EXPIRY_FORMATTER);
                log.debug("Parsed GSP token expiry: {}", tokenExpiry);
            } catch (Exception e) {
                log.warn("Failed to parse token expiry '{}', using default timeout", 
                        authResponse.getTokenExpiry(), e);
            }
        }

        // Create session and get session token
        String sessionToken = sessionService.createSession(
            tenantId,
            sessionCredentials.getEinvUsername(),
            encryptionService.encrypt(sessionCredentials.getEinvPassword()), // Store encrypted
            authResponse.getAuthToken(),
            credentials.getEinvGstin(),
            tokenExpiry
        );

        EInvoiceSessionService.SessionData sessionData = sessionService.getSession(sessionToken);

        log.info("E-Invoice session created successfully for tenant: {}, GSTIN: {}", 
                 tenantId, credentials.getEinvGstin());

        return EwayBillSessionTokenResponse.builder()
            .sessionToken(sessionToken)
            .authToken(authResponse.getAuthToken())
            .gstin(credentials.getEinvGstin())
            .expiresAt(sessionData.getExpiresAt())
            .createdAt(sessionData.getCreatedAt())
            .message("New session created successfully")
            .build();
    }

    /**
     * Get session data by session token
     * 
     * @param sessionToken Session token
     * @return Session data if valid, null otherwise
     */
    public EInvoiceSessionService.SessionData getSessionData(String sessionToken) {
        return sessionService.getSession(sessionToken);
    }

    /**
     * Invalidate a session
     * 
     * @param sessionToken Session token to invalidate
     */
    public void invalidateSession(String sessionToken) {
        sessionService.invalidateSession(sessionToken);
        log.info("E-Invoice session invalidated: {}", sessionToken);
    }

    /**
     * Get active session count
     * 
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return sessionService.getActiveSessionCount();
    }

    /**
     * Authenticate with GSP using provided credentials with failover support
     * 
     * @return EInvoiceAuthResponse containing auth token and token expiry
     */
    private EInvoiceAuthResponse authenticateWithGsp(TenantEInvoiceCredentials credentials, 
                                                      String username, String password) {
        log.info("Attempting E-Invoice GSP authentication for GSTIN: {}", credentials.getEinvGstin());

        String[] serverUrls = {authUrl};
        
        return failoverHelper.executeWithFailover(
            serverUrls,
            serverUrl -> attemptEInvoiceAuthentication(credentials, serverUrl, username, password),
            "E-Invoice authentication for tenant: " + credentials.getTenant().getId()
        );
    }

    /**
     * Attempt authentication with a specific GSP server
     * 
     * @return EInvoiceAuthResponse containing auth token and token expiry
     */
    private EInvoiceAuthResponse attemptEInvoiceAuthentication(TenantEInvoiceCredentials credentials,
                                                                String serverAuthUrl,
                                                                String username, 
                                                                String password) {
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

            // E-Invoice auth API uses GET with query parameters
            String url = String.format("%s?aspid=%s&password=%s&Gstin=%s&user_name=%s&eInvPwd=%s",
                serverAuthUrl,
                aspId,
                aspPassword,  // ASP password
                credentials.getEinvGstin(),
                username,  // E-Invoice username
                password   // E-Invoice password (typically same as ASP password)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<EInvoiceAuthResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                EInvoiceAuthResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                EInvoiceAuthResponse authResponse = response.getBody();

                if (authResponse.isSuccess()) {
                    log.info("E-Invoice authentication successful for tenant: {}, token expires at: {}",
                            credentials.getTenant().getId(), authResponse.getTokenExpiry());
                    return authResponse;
                } else {
                    throw new RuntimeException("E-Invoice authentication failed: " + authResponse.getErrorDetails());
                }
            }

            throw new RuntimeException("E-Invoice authentication failed with status: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("Error during E-Invoice authentication attempt for tenant: {}", 
                     credentials.getTenant().getId(), e);
            throw new RuntimeException("E-Invoice authentication failed: " + e.getMessage(), e);
        }
    }
}
