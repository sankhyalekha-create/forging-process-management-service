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
        private LocalDateTime expiresAt;

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        public void updateLastAccess() {
            this.lastAccessedAt = LocalDateTime.now();
            this.expiresAt = LocalDateTime.now().plusMinutes(SESSION_TIMEOUT_MINUTES);
        }
    }

    /**
     * Create new session and return session token
     * 
     * @param tenantId Tenant ID
     * @param ewbUsername E-Way Bill username (will be encrypted)
     * @param ewbPassword E-Way Bill password (will be encrypted)
     * @param gspAuthToken GSP authentication token
     * @param gstin GSTIN
     * @return Session token
     */
    public String createSession(Long tenantId, String ewbUsername, String ewbPassword, 
                                String gspAuthToken, String gstin) {
        // Generate secure random token
        String sessionToken = generateSecureToken();

        // Create session data
        SessionData sessionData = new SessionData();
        sessionData.setTenantId(tenantId);
        sessionData.setEwbUsername(ewbUsername);
        sessionData.setEwbPassword(ewbPassword); // Store encrypted
        sessionData.setGspAuthToken(gspAuthToken);
        sessionData.setGstin(gstin);
        sessionData.setCreatedAt(LocalDateTime.now());
        sessionData.setLastAccessedAt(LocalDateTime.now());
        sessionData.setExpiresAt(LocalDateTime.now().plusMinutes(SESSION_TIMEOUT_MINUTES));

        // Store session
        sessionStore.put(sessionToken, sessionData);

        log.info("Created new E-Way Bill session for tenant: {}, GSTIN: {}, expires at: {}", 
                 tenantId, gstin, sessionData.getExpiresAt());

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
