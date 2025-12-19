package com.jangid.forging_process_management_service.resource.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.dto.email.DispatchEmailRequest;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.dispatch.DispatchBatchService;
import com.jangid.forging_process_management_service.service.email.DispatchEmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.MessagingException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * REST API for dispatch email management
 */
@Slf4j
@RestController
@RequestMapping("/api/dispatch-email")
@RequiredArgsConstructor
public class DispatchEmailResource {

    private final DispatchEmailService dispatchEmailService;
    private final DispatchBatchService dispatchBatchService;
    private final TenantService tenantService;

    /**
     * Send dispatch notification email manually
     * POST /api/dispatch-email/send
     * Accepts multipart/form-data for file uploads
     */
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendDispatchEmail(
            @RequestParam("dispatchBatchId") Long dispatchBatchId,
            @RequestParam("toEmail") String toEmail,
            @RequestParam(value = "fromEmail", required = false) String fromEmail,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "documentIds", required = false) String documentIdsJson,
            @RequestParam(value = "ccRecipients", required = false) String ccRecipientsJson,
            @RequestParam(value = "sendImmediately", required = false, defaultValue = "true") Boolean sendImmediately,
            @RequestParam("smtpHost") String smtpHost,
            @RequestParam("smtpPort") Integer smtpPort,
            @RequestParam(value = "smtpUsername", required = false) String smtpUsername,
            @RequestParam(value = "smtpPassword", required = false) String smtpPassword,
            @RequestParam(value = "smtpUseTls", required = false, defaultValue = "true") Boolean smtpUseTls,
            @RequestParam(value = "smtpUseAuth", required = false, defaultValue = "true") Boolean smtpUseAuth,
            @RequestParam(value = "uploadedFiles", required = false) MultipartFile[] uploadedFiles) {
        
        try {
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
            ObjectMapper objectMapper = new ObjectMapper();
            
            // Parse document IDs from JSON string
            List<Long> documentIds = new ArrayList<>();
            if (documentIdsJson != null && !documentIdsJson.trim().isEmpty()) {
                try {
                    documentIds = objectMapper.readValue(documentIdsJson, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
                } catch (Exception e) {
                    log.warn("Failed to parse documentIds JSON: {}", documentIdsJson, e);
                }
            }
            
            // Parse CC recipients from JSON string
            List<String> ccRecipients = new ArrayList<>();
            if (ccRecipientsJson != null && !ccRecipientsJson.trim().isEmpty()) {
                try {
                    ccRecipients = objectMapper.readValue(ccRecipientsJson, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                } catch (Exception e) {
                    log.warn("Failed to parse ccRecipients JSON: {}", ccRecipientsJson, e);
                }
            }
            
            // Build SMTP config
            DispatchEmailRequest.SmtpConfig smtpConfig = DispatchEmailRequest.SmtpConfig.builder()
                    .host(smtpHost)
                    .port(smtpPort)
                    .username(smtpUsername)
                    .password(smtpPassword)
                    .useTls(smtpUseTls)
                    .useAuth(smtpUseAuth)
                    .build();
            
            // Validate to email
            if (toEmail == null || toEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Recipient email address (To) is required"));
            }

            // Validate SMTP configuration
            if (smtpHost == null || smtpHost.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "SMTP host is required"));
            }

            if (smtpPort == null || smtpPort <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Valid SMTP port is required"));
            }

            // Get dispatch batch
            DispatchBatch dispatchBatch = dispatchBatchService.getDispatchBatchById(dispatchBatchId);
            
            // Get tenant for default from email if not provided
            Tenant tenant = tenantService.getTenantById(tenantId);
            String resolvedFromEmail = fromEmail;
            if (resolvedFromEmail == null || resolvedFromEmail.trim().isEmpty()) {
                // Try SMTP username or tenant email as fallback
                if (smtpUsername != null && !smtpUsername.trim().isEmpty()) {
                    resolvedFromEmail = smtpUsername;
                } else if (tenant.getEmail() != null && !tenant.getEmail().trim().isEmpty()) {
                    resolvedFromEmail = tenant.getEmail();
                } else {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Sender email address (From) is required"));
                }
            }
            
            // Log uploaded files info
            if (uploadedFiles != null && uploadedFiles.length > 0) {
                log.info("Received {} uploaded files: {}", uploadedFiles.length, 
                    Arrays.stream(uploadedFiles)
                        .map(MultipartFile::getOriginalFilename)
                        .collect(Collectors.joining(", ")));
            }

            // Send email
            if (Boolean.TRUE.equals(sendImmediately)) {
                // Synchronous send
                dispatchEmailService.sendDispatchNotification(
                        dispatchBatch,
                        tenantId,
                        toEmail,
                        resolvedFromEmail,
                        documentIds,
                        subject,
                        message,
                        ccRecipients,
                        smtpConfig,
                        uploadedFiles
                );
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Email sent successfully");
                response.put("recipient", toEmail);
                response.put("sender", resolvedFromEmail);
                response.put("dispatchBatchNumber", dispatchBatch.getDispatchBatchNumber());
                if (uploadedFiles != null && uploadedFiles.length > 0) {
                    response.put("uploadedFilesCount", uploadedFiles.length);
                }
                
                return ResponseEntity.ok(response);
            } else {
                // Asynchronous send with credential validation first
                log.info("Validating SMTP credentials before async email send...");
                
                // Quick validation (2-3 seconds) - will throw exception if credentials are wrong
                dispatchEmailService.validateSmtpCredentials(smtpConfig);
                
                log.info("SMTP credentials validated. Queueing email for async send...");
                
                // Credentials are valid, now send async
                dispatchEmailService.sendDispatchNotificationAsync(
                        dispatchBatch,
                        tenantId,
                        toEmail,
                        resolvedFromEmail,
                        documentIds,
                        subject,
                        message,
                        ccRecipients,
                        smtpConfig,
                        uploadedFiles
                );
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Email credentials verified. Email queued for sending.");
                response.put("recipient", toEmail);
                response.put("sender", resolvedFromEmail);
                response.put("dispatchBatchNumber", dispatchBatch.getDispatchBatchNumber());
                if (uploadedFiles != null && uploadedFiles.length > 0) {
                    response.put("uploadedFilesCount", uploadedFiles.length);
                }
                
                return ResponseEntity.accepted().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error sending dispatch email: {}", e.getMessage(), e);
            
            String errorMessage = e.getMessage();
            
            // Extract root cause for better error messages
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause().getMessage() != null) {
                rootCause = rootCause.getCause();
            }
            
            // Use the most descriptive error message available
            if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
                errorMessage = e.getMessage();
            } else if (rootCause != e && rootCause.getMessage() != null && !rootCause.getMessage().trim().isEmpty()) {
                errorMessage = rootCause.getMessage();
            } else {
                errorMessage = "An unexpected error occurred while sending the email.";
            }
            
            // Determine appropriate HTTP status based on error type
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            
            // Authentication/credential errors
            if (e instanceof MessagingException || 
                errorMessage.contains("authentication") || errorMessage.contains("Authentication") || 
                errorMessage.contains("credentials") || errorMessage.contains("Credentials") ||
                errorMessage.contains("535") || errorMessage.contains("BadCredentials")) {
                status = HttpStatus.UNAUTHORIZED;
            }
            // Connection/network errors  
            else if (errorMessage.contains("Could not connect") || errorMessage.contains("Connection refused") ||
                     errorMessage.contains("Connection timed out") || errorMessage.contains("timeout")) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
            }
            // SSL/TLS errors - keep as 500 but with detailed message
            else if (errorMessage.contains("SSL") || errorMessage.contains("TLS") || 
                     errorMessage.contains("certificate") || errorMessage.contains("PKIX")) {
                status = HttpStatus.INTERNAL_SERVER_ERROR; // SSL config issues
            }
            
            return ResponseEntity.status(status)
                    .body(Map.of("error", errorMessage));
        }
    }

}
