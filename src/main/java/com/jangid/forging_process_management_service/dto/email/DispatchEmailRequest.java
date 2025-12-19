package com.jangid.forging_process_management_service.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for dispatch email configuration
 * Includes runtime SMTP configuration - no credentials stored
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchEmailRequest {
    
    /**
     * Dispatch batch ID
     */
    private Long dispatchBatchId;
    
    /**
     * Recipient email address (to)
     */
    private String toEmail;
    
    /**
     * Sender email address (from)
     */
    private String fromEmail;
    
    /**
     * SMTP Configuration (provided at send time)
     */
    private SmtpConfig smtpConfig;
    
    /**
     * List of document IDs to attach
     */
    private List<Long> documentIds;
    
    /**
     * Custom email subject (optional)
     */
    private String subject;
    
    /**
     * Custom email body/message (optional)
     */
    private String message;
    
    /**
     * Additional CC recipients (optional)
     */
    private List<String> ccRecipients;
    
    /**
     * Send email immediately (default: true)
     */
    @Builder.Default
    private Boolean sendImmediately = true;
    
    /**
     * SMTP Configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmtpConfig {
        private String host;
        private Integer port;
        private String username;
        private String password;
        private Boolean useTls;
        private Boolean useAuth;
    }
}
