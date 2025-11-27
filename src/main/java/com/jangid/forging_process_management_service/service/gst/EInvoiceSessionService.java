package com.jangid.forging_process_management_service.service.gst;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing E-Invoice session tokens
 * Provides in-memory session storage with automatic expiry
 * Similar to EwayBillSessionService
 * 
 * Security Features:
 * - No database persistence of credentials
 * - 30-minute inactivity timeout
 * - Automatic cleanup of expired sessions
 * - Secure token generation
 */
@Service
@Slf4j
public class EInvoiceSessionService {

    // In-memory session storage: sessionToken -> SessionData
    private final Map<String, SessionData> sessionStore = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Session data structure
     */
    @Data
    public static class SessionData {
        private Long tenantId;
        private String einvUsername;
        private String einvPassword; // Encrypted in memory
        private String gspAuthToken;
        private String gstin;
        private LocalDateTime createdAt;
        private LocalDateTime lastAccessedAt;
        private LocalDateTime expiresAt; // Maps to TokenExpiry from GSP
        private LocalDateTime gspTokenExpiry; // Original TokenExpiry from GSP auth response

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        /**
         * Check if GSP auth token needs renewal (within 5 minutes of expiry)
         * @return true if token will expire within 5 minutes
         */
        public boolean needsTokenRenewal() {
            if (gspTokenExpiry == null) {
                return false;
            }
            LocalDateTime renewalThreshold = LocalDateTime.now().plusMinutes(5);
            return renewalThreshold.isAfter(gspTokenExpiry) || renewalThreshold.isEqual(gspTokenExpiry);
        }

        public void updateLastAccess() {
            this.lastAccessedAt = LocalDateTime.now();
            // Don't extend expiry - use GSP token expiry
            // Session should expire when GSP auth token expires
        }
    }

    /**
     * Create new session and return session token
     * 
     * @param tenantId Tenant ID
     * @param einvUsername E-Invoice username (will be encrypted)
     * @param einvPassword E-Invoice password (will be encrypted)
     * @param gspAuthToken GSP authentication token
     * @param gstin GSTIN
     * @param gspTokenExpiry GSP token expiry from authentication response
     * @return Session token
     */
    public String createSession(Long tenantId, String einvUsername, String einvPassword, 
                                String gspAuthToken, String gstin, LocalDateTime gspTokenExpiry) {
        // Generate secure random token
        String sessionToken = generateSecureToken();

        // Create session data
        SessionData sessionData = new SessionData();
        sessionData.setTenantId(tenantId);
        sessionData.setEinvUsername(einvUsername);
        sessionData.setEinvPassword(einvPassword); // Store encrypted
        sessionData.setGspAuthToken(gspAuthToken);
        sessionData.setGstin(gstin);
        sessionData.setCreatedAt(LocalDateTime.now());
        sessionData.setLastAccessedAt(LocalDateTime.now());
        
        // Use GSP token expiry if provided, otherwise fall back to 30-minute timeout
        if (gspTokenExpiry != null) {
            sessionData.setGspTokenExpiry(gspTokenExpiry);
            sessionData.setExpiresAt(gspTokenExpiry);
            log.info("Created new E-Invoice session for tenant: {}, GSTIN: {}, GSP token expires at: {}", 
                     tenantId, gstin, gspTokenExpiry);
        }

        // Store session
        sessionStore.put(sessionToken, sessionData);

        return sessionToken;
    }

    /**
     * Get session data by token
     * Updates last accessed time if session is valid
     * 
     * @param sessionToken Session token
     * @return SessionData if valid, null if expired or not found
     */
    public SessionData getSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isEmpty()) {
            return null;
        }

        SessionData sessionData = sessionStore.get(sessionToken);

        if (sessionData == null) {
            log.debug("E-Invoice session not found: {}", sessionToken);
            return null;
        }

        if (sessionData.isExpired()) {
            log.info("E-Invoice session expired for tenant: {}, GSTIN: {}", 
                     sessionData.getTenantId(), sessionData.getGstin());
            sessionStore.remove(sessionToken);
            return null;
        }

        // Update last access time and extend expiry
        sessionData.updateLastAccess();
        
        log.debug("E-Invoice session accessed for tenant: {}, GSTIN: {}, new expiry: {}", 
                  sessionData.getTenantId(), sessionData.getGstin(), sessionData.getExpiresAt());

        return sessionData;
    }

    /**
     * Validate if session token is valid
     * 
     * @param sessionToken Session token
     * @return true if valid, false otherwise
     */
    public boolean isValidSession(String sessionToken) {
        return getSession(sessionToken) != null;
    }

    /**
     * Invalidate (delete) a session
     * 
     * @param sessionToken Session token
     */
    public void invalidateSession(String sessionToken) {
        SessionData removed = sessionStore.remove(sessionToken);
        if (removed != null) {
            log.info("E-Invoice session invalidated for tenant: {}, GSTIN: {}", 
                     removed.getTenantId(), removed.getGstin());
        }
    }

    /**
     * Get session count (for monitoring)
     * 
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return sessionStore.size();
    }

    /**
     * Cleanup expired sessions
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredSessions() {
        int beforeCount = sessionStore.size();
        
        sessionStore.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired();
            if (expired) {
                log.debug("Removing expired E-Invoice session for tenant: {}", entry.getValue().getTenantId());
            }
            return expired;
        });

        int afterCount = sessionStore.size();
        int removed = beforeCount - afterCount;

        if (removed > 0) {
            log.info("Cleaned up {} expired E-Invoice sessions. Active sessions: {}", 
                     removed, afterCount);
        }
    }

    /**
     * Generate secure random session token
     * 
     * @return Base64-encoded random token
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
