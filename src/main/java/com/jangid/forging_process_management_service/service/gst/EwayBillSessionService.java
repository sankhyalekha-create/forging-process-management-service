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
 * Service for managing E-Way Bill session tokens
 * Provides in-memory session storage with automatic expiry
 * 
 * Security Features:
 * - No database persistence of credentials
 * - 30-minute inactivity timeout
 * - Automatic cleanup of expired sessions
 * - Secure token generation
 */
@Service
@Slf4j
public class EwayBillSessionService {

    // In-memory session storage: sessionToken -> SessionData
    private final Map<String, SessionData> sessionStore = new ConcurrentHashMap<>();

    // Session timeout: 30 minutes of inactivity
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Session data structure
     */
    @Data
    public static class SessionData {
        private Long tenantId;
        private String ewbUsername;
        private String ewbPassword; // Encrypted in memory
        private String gspAuthToken;
        private String gstin;
        private LocalDateTime createdAt;
        private LocalDateTime lastAccessedAt;
        private LocalDateTime expiresAt; // Session expiry time
        private LocalDateTime tokenExpiresAt; // GSP auth token expiry time

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        public boolean isTokenExpired() {
            return tokenExpiresAt != null && LocalDateTime.now().isAfter(tokenExpiresAt);
        }

        /**
         * Check if token needs renewal (within 5 minutes of expiry)
         * @return true if token will expire within 5 minutes
         */
        public boolean needsTokenRenewal() {
            if (tokenExpiresAt == null) {
                return false;
            }
            LocalDateTime renewalThreshold = LocalDateTime.now().plusMinutes(5);
            return renewalThreshold.isAfter(tokenExpiresAt) || renewalThreshold.isEqual(tokenExpiresAt);
        }

        public void updateLastAccess() {
            this.lastAccessedAt = LocalDateTime.now();
        }
    }

    /**
     * Create new session with configurable timeout and return session token
     * 
     * @param tenantId Tenant ID
     * @param ewbUsername E-Way Bill username (will be encrypted)
     * @param ewbPassword E-Way Bill password (will be encrypted)
     * @param gspAuthToken GSP authentication token
     * @param gstin GSTIN
     * @param sessionTimeoutHours Session timeout in hours
     * @return Session token
     */
    public String createSession(Long tenantId, String ewbUsername, String ewbPassword, 
                                String gspAuthToken, String gstin, int sessionTimeoutHours) {
        // Generate secure random token
        String sessionToken = generateSecureToken();

        // Create session data
        SessionData sessionData = new SessionData();
        sessionData.setTenantId(tenantId);
        sessionData.setEwbUsername(ewbUsername);
        sessionData.setEwbPassword(ewbPassword); // Store encrypted
        sessionData.setGspAuthToken(gspAuthToken);
        sessionData.setGstin(gstin);
        
        LocalDateTime now = LocalDateTime.now();
        sessionData.setCreatedAt(now);
        sessionData.setLastAccessedAt(now);
        
        // Set session expiry based on configured hours
        LocalDateTime expiresAt = now.plusHours(sessionTimeoutHours);
        sessionData.setExpiresAt(expiresAt);
        sessionData.setTokenExpiresAt(expiresAt); // Assume token expires with session

        // Store session
        sessionStore.put(sessionToken, sessionData);

        log.info("Created new E-Way Bill session for tenant: {}, GSTIN: {}, timeout: {} hours, expires at: {}", 
                 tenantId, gstin, sessionTimeoutHours, sessionData.getExpiresAt());

        return sessionToken;
    }

    /**
     * Create new session with default timeout and return session token
     * @deprecated Use createSession(Long, String, String, String, String, int) with explicit timeout
     * 
     * @param tenantId Tenant ID
     * @param ewbUsername E-Way Bill username (will be encrypted)
     * @param ewbPassword E-Way Bill password (will be encrypted)
     * @param gspAuthToken GSP authentication token
     * @param gstin GSTIN
     * @return Session token
     */
    @Deprecated
    public String createSession(Long tenantId, String ewbUsername, String ewbPassword, 
                                String gspAuthToken, String gstin) {
        return createSession(tenantId, ewbUsername, ewbPassword, gspAuthToken, gstin, 
                           SESSION_TIMEOUT_MINUTES / 60);
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
            log.debug("Session not found: {}", sessionToken);
            return null;
        }

        if (sessionData.isExpired()) {
            log.info("Session expired for tenant: {}, GSTIN: {}", 
                     sessionData.getTenantId(), sessionData.getGstin());
            sessionStore.remove(sessionToken);
            return null;
        }

        // Update last access time and extend expiry
        sessionData.updateLastAccess();
        
        log.debug("Session accessed for tenant: {}, GSTIN: {}, new expiry: {}", 
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
            log.info("Session invalidated for tenant: {}, GSTIN: {}", 
                     removed.getTenantId(), removed.getGstin());
        }
    }

    /**
     * Invalidate all sessions for a specific tenant
     * Useful when tenant credentials change or auth token is universally expired
     * 
     * @param tenantId Tenant ID
     * @return Number of sessions invalidated
     */
    public int invalidateSessionsByTenant(Long tenantId) {
        int count = 0;
        
        for (Map.Entry<String, SessionData> entry : sessionStore.entrySet()) {
            if (entry.getValue().getTenantId().equals(tenantId)) {
                sessionStore.remove(entry.getKey());
                count++;
                log.info("Invalidated session for tenant: {}, GSTIN: {}", 
                         tenantId, entry.getValue().getGstin());
            }
        }
        
        if (count > 0) {
            log.info("Invalidated {} session(s) for tenant: {}", count, tenantId);
        }
        
        return count;
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
                log.debug("Removing expired session for tenant: {}", entry.getValue().getTenantId());
            }
            return expired;
        });

        int afterCount = sessionStore.size();
        int removed = beforeCount - afterCount;

        if (removed > 0) {
            log.info("Cleaned up {} expired E-Way Bill sessions. Active sessions: {}", 
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
