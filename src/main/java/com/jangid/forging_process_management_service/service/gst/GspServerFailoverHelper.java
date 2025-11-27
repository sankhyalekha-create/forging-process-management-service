package com.jangid.forging_process_management_service.service.gst;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.function.Function;

/**
 * Helper utility for GSP API server failover logic
 * Implements automatic retry across Primary, Backup1 (Mumbai), and Backup2 (Delhi) servers
 * 
 * Pattern: Primary (3 attempts) → Backup1 (3 attempts) → Backup2 (3 attempts)
 * 
 * Usage:
 * <pre>
 * String result = failoverHelper.executeWithFailover(
 *     new String[]{primaryUrl, backup1Url, backup2Url},
 *     serverUrl -> attemptApiCall(serverUrl)
 * );
 * </pre>
 */
@Component
@Slf4j
public class GspServerFailoverHelper {

    private static final int MAX_RETRY_ATTEMPTS_PER_SERVER = 1;
    private static final long RETRY_DELAY_MS = 1000; // 1 second between retries
    private static final String[] SERVER_NAMES = {"Primary", "Backup1 (Mumbai)", "Backup2 (Delhi)"};

    /**
     * Execute an API call with automatic failover across multiple servers
     * 
     * @param <T> Return type of the API call
     * @param serverUrls Array of server URLs to try (Primary, Backup1, Backup2)
     * @param attemptFunction Function that takes a server URL and makes the API call
     * @return Result from the successful API call
     * @throws RuntimeException if all servers fail after all retry attempts
     */
    public <T> T executeWithFailover(String[] serverUrls, Function<String, T> attemptFunction) {
        return executeWithFailover(serverUrls, attemptFunction, "API call");
    }

    /**
     * Execute an API call with automatic failover across multiple servers
     * 
     * @param <T> Return type of the API call
     * @param serverUrls Array of server URLs to try (Primary, Backup1, Backup2)
     * @param attemptFunction Function that takes a server URL and makes the API call
     * @param operationName Descriptive name of the operation (for logging)
     * @return Result from the successful API call
     * @throws RuntimeException if all servers fail after all retry attempts
     */
    public <T> T executeWithFailover(String[] serverUrls, Function<String, T> attemptFunction, String operationName) {
        Exception lastException = null;
        
        // Try each server in sequence
        for (int serverIndex = 0; serverIndex < serverUrls.length && serverIndex < SERVER_NAMES.length; serverIndex++) {
            String currentServerUrl = serverUrls[serverIndex];
            String serverName = SERVER_NAMES[serverIndex];
            
            // Skip if backup URL is not configured
            if (currentServerUrl == null || currentServerUrl.isEmpty()) {
                log.debug("Skipping {} server - URL not configured", serverName);
                continue;
            }
            
            log.info("Attempting {} with {} server: {}", operationName, serverName, currentServerUrl);
            
            // Try this server MAX_RETRY_ATTEMPTS_PER_SERVER times
            for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS_PER_SERVER; attempt++) {
                try {
                    log.info("{} attempt {} of {} on {} server", 
                            operationName, attempt, MAX_RETRY_ATTEMPTS_PER_SERVER, serverName);
                    
                    T result = attemptFunction.apply(currentServerUrl);
                    
                    log.info("✓ Successfully completed {} on {} server (attempt {})", 
                            operationName, serverName, attempt);
                    return result;
                    
                } catch (HttpClientErrorException e) {
                    lastException = e;
                    // Don't retry on 4xx client errors (except 429 Too Many Requests)
                    if (e.getStatusCode().is4xxClientError() && e.getStatusCode().value() != 429) {
                        log.error("✗ Client error ({}), not retrying: {}", 
                                e.getStatusCode(), e.getMessage());
                        throw e;
                    }
                    log.error("✗ {} attempt {} failed on {} server: {}", 
                            operationName, attempt, serverName, e.getMessage());
                    
                } catch (Exception e) {
                    lastException = e;
                    log.error("✗ {} attempt {} failed on {} server: {}", 
                            operationName, attempt, serverName, e.getMessage());
                }
                
                // Wait before retrying (except on last attempt of this server)
                if (attempt < MAX_RETRY_ATTEMPTS_PER_SERVER) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(operationName + " interrupted", ie);
                    }
                }
            }
            
            // All attempts on this server failed, move to next server
            log.warn("All {} attempts failed on {} server, trying next server...", 
                    MAX_RETRY_ATTEMPTS_PER_SERVER, serverName);
        }
        
        // All servers exhausted
        log.error("{} failed on all servers (Primary, Backup1, Backup2) after {} attempts per server", 
                operationName, MAX_RETRY_ATTEMPTS_PER_SERVER);
        throw new RuntimeException("Failed to complete " + operationName + " on all servers: " + 
                                 (lastException != null ? lastException.getMessage() : "Unknown error"), 
                                 lastException);
    }

    /**
     * Execute an API call with automatic failover, with custom retry attempts
     * 
     * @param <T> Return type of the API call
     * @param serverUrls Array of server URLs to try (Primary, Backup1, Backup2)
     * @param attemptFunction Function that takes a server URL and makes the API call
     * @param operationName Descriptive name of the operation (for logging)
     * @param maxRetryAttemptsPerServer Maximum number of retry attempts per server
     * @return Result from the successful API call
     * @throws RuntimeException if all servers fail after all retry attempts
     */
    public <T> T executeWithFailover(String[] serverUrls, 
                                      Function<String, T> attemptFunction, 
                                      String operationName,
                                      int maxRetryAttemptsPerServer) {
        Exception lastException = null;
        
        // Try each server in sequence
        for (int serverIndex = 0; serverIndex < serverUrls.length && serverIndex < SERVER_NAMES.length; serverIndex++) {
            String currentServerUrl = serverUrls[serverIndex];
            String serverName = SERVER_NAMES[serverIndex];
            
            // Skip if backup URL is not configured
            if (currentServerUrl == null || currentServerUrl.isEmpty()) {
                log.debug("Skipping {} server - URL not configured", serverName);
                continue;
            }
            
            log.info("Attempting {} with {} server: {}", operationName, serverName, currentServerUrl);
            
            // Try this server maxRetryAttemptsPerServer times
            for (int attempt = 1; attempt <= maxRetryAttemptsPerServer; attempt++) {
                try {
                    log.info("{} attempt {} of {} on {} server", 
                            operationName, attempt, maxRetryAttemptsPerServer, serverName);
                    
                    T result = attemptFunction.apply(currentServerUrl);
                    
                    log.info("✓ Successfully completed {} on {} server (attempt {})", 
                            operationName, serverName, attempt);
                    return result;
                    
                } catch (HttpClientErrorException e) {
                    lastException = e;
                    // Don't retry on 4xx client errors (except 429 Too Many Requests)
                    if (e.getStatusCode().is4xxClientError() && e.getStatusCode().value() != 429) {
                        log.error("✗ Client error ({}), not retrying: {}", 
                                e.getStatusCode(), e.getMessage());
                        throw e;
                    }
                    log.error("✗ {} attempt {} failed on {} server: {}", 
                            operationName, attempt, serverName, e.getMessage());
                    
                } catch (Exception e) {
                    lastException = e;
                    log.error("✗ {} attempt {} failed on {} server: {}", 
                            operationName, attempt, serverName, e.getMessage());
                }
                
                // Wait before retrying (except on last attempt of this server)
                if (attempt < maxRetryAttemptsPerServer) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(operationName + " interrupted", ie);
                    }
                }
            }
            
            // All attempts on this server failed, move to next server
            log.warn("All {} attempts failed on {} server, trying next server...", 
                    maxRetryAttemptsPerServer, serverName);
        }
        
        // All servers exhausted
        log.error("{} failed on all servers (Primary, Backup1, Backup2) after {} attempts per server", 
                operationName, maxRetryAttemptsPerServer);
        throw new RuntimeException("Failed to complete " + operationName + " on all servers: " + 
                                 (lastException != null ? lastException.getMessage() : "Unknown error"), 
                                 lastException);
    }
}
