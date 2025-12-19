package com.jangid.forging_process_management_service.service.email;

import com.jangid.forging_process_management_service.dto.email.DispatchEmailRequest;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.document.Document;
import com.jangid.forging_process_management_service.entities.document.DocumentLink;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.document.DocumentService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

/**
 * Service for sending dispatch notification emails with attachments
 * Uses runtime SMTP configuration - no credentials stored
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchEmailService {

    private final DocumentService documentService;
    private final TenantService tenantService;

    @org.springframework.beans.factory.annotation.Value("${fopmas.documents.storage.base-path:/var/fopmas/documents}")
    private String basePath;

    /**
     * Create JavaMailSender with runtime SMTP configuration
     */
    private JavaMailSender createMailSender(DispatchEmailRequest.SmtpConfig smtpConfig) {
        if (smtpConfig == null) {
            throw new IllegalArgumentException("SMTP configuration is required");
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpConfig.getHost());
        mailSender.setPort(smtpConfig.getPort() != null ? smtpConfig.getPort() : 587);
        
        if (smtpConfig.getUsername() != null && !smtpConfig.getUsername().trim().isEmpty()) {
            mailSender.setUsername(smtpConfig.getUsername());
        }
        
        if (smtpConfig.getPassword() != null && !smtpConfig.getPassword().trim().isEmpty()) {
            // Remove spaces from password (common issue with Gmail App Passwords)
            String cleanPassword = smtpConfig.getPassword().replaceAll("\\s+", "");
            mailSender.setPassword(cleanPassword);
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", smtpConfig.getUseAuth() != null ? smtpConfig.getUseAuth() : true);
        props.put("mail.smtp.starttls.enable", smtpConfig.getUseTls() != null ? smtpConfig.getUseTls() : true);
        props.put("mail.smtp.starttls.required", smtpConfig.getUseTls() != null ? smtpConfig.getUseTls() : true);
        
        // Increased timeouts to handle large attachments and slower SMTP servers
        // These values are standard for production email sending
        props.put("mail.smtp.connectiontimeout", "30000");  // 30 seconds to establish connection
        props.put("mail.smtp.timeout", "60000");            // 60 seconds for server responses
        props.put("mail.smtp.writetimeout", "60000");       // 60 seconds for writing data
        
        props.put("mail.debug", "false");
        
        // For custom domains with Zoho/other hosting - trust SSL certificates
        // This handles cases where SMTP host (smtp.customdomain.com) has a certificate 
        // issued for a different hostname (smtp.zoho.com)
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.ssl.checkserveridentity", "false");

        return mailSender;
    }

    /**
     * Validate SMTP credentials by testing connection
     * This is a quick test (2-3 seconds) to verify credentials before async send
     */
    public void validateSmtpCredentials(DispatchEmailRequest.SmtpConfig smtpConfig) throws MessagingException {
        if (smtpConfig == null) {
            throw new IllegalArgumentException("SMTP configuration is required");
        }

        log.info("Validating SMTP credentials for host: {}", smtpConfig.getHost());
        
        JavaMailSenderImpl mailSender = (JavaMailSenderImpl) createMailSender(smtpConfig);
        
        try {
            // Test connection by getting a transport and connecting
            mailSender.testConnection();
            log.info("SMTP credentials validated successfully for: {}", smtpConfig.getUsername());
        } catch (MessagingException e) {
            log.error("SMTP credential validation failed for: {}. Error: {}", 
                    smtpConfig.getUsername(), e.getMessage());
            
            // Extract root cause for better error reporting
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            
            String errorDetail = e.getMessage();
            if (rootCause != e && rootCause.getMessage() != null) {
                errorDetail = rootCause.getMessage();
            }
            
            // Provide specific error messages based on the failure type
            String userMessage;
            
            if (errorDetail.contains("SSLHandshakeException") || errorDetail.contains("PKIX path building failed") ||
                errorDetail.contains("unable to find valid certification path") || 
                errorDetail.contains("No subject alternative DNS name matching")) {
                userMessage = String.format(
                    "SSL/TLS certificate error for %s. This may occur with custom domain emails hosted by third-party providers (like Zoho). " +
                    "Please verify: 1) Correct SMTP host (try smtp.zoho.com instead of smtp.%s if Zoho-hosted), " +
                    "2) Correct port (587 for STARTTLS, 465 for SSL), 3) Email credentials are correct.",
                    smtpConfig.getHost(), smtpConfig.getHost().replaceFirst("^smtp\\.", "")
                );
            } else if (errorDetail.contains("535") || errorDetail.contains("authentication failed") || 
                       errorDetail.contains("Invalid credentials") || errorDetail.contains("BadCredentials")) {
                userMessage = String.format(
                    "Authentication failed for %s. Please check: 1) Email address is correct, " +
                    "2) Password is correct (For Gmail use App Password, not regular password), " +
                    "3) SMTP access is enabled in your email provider settings.",
                    smtpConfig.getUsername()
                );
            } else if (errorDetail.contains("Could not connect to SMTP host") || 
                       errorDetail.contains("Connection refused") || errorDetail.contains("Connection timed out")) {
                userMessage = String.format(
                    "Cannot connect to SMTP server %s:%d. Please check: 1) SMTP host is correct, " +
                    "2) Port is correct (587 for STARTTLS, 465 for SSL), 3) Firewall allows SMTP connections.",
                    smtpConfig.getHost(), smtpConfig.getPort()
                );
            } else {
                userMessage = String.format(
                    "SMTP validation failed: %s. Please verify your email settings and credentials.",
                    errorDetail
                );
            }
            
            throw new MessagingException(userMessage, e);
        }
    }

    /**
     * Send dispatch notification email with selected documents
     * Uses runtime SMTP configuration provided by user
     * Async method - errors are logged but not propagated
     */
    @Async
    public void sendDispatchNotificationAsync(
            DispatchBatch dispatchBatch,
            Long tenantId,
            String toEmail,
            String fromEmail,
            List<Long> documentIds,
            String customSubject,
            String customMessage,
            List<String> ccRecipients,
            DispatchEmailRequest.SmtpConfig smtpConfig,
            MultipartFile[] uploadedFiles) {
        
        try {
            log.info("Starting async email send for batch: {} to: {}", 
                    dispatchBatch.getDispatchBatchNumber(), toEmail);
            
            sendDispatchNotification(dispatchBatch, tenantId, toEmail, fromEmail, documentIds, 
                                   customSubject, customMessage, ccRecipients, smtpConfig, uploadedFiles);
            
            log.info("Async email send completed successfully for batch: {}", 
                    dispatchBatch.getDispatchBatchNumber());
                    
        } catch (Exception e) {
            // Check if it's a timeout after successful send
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            boolean isTimeout = errorMsg.contains("timeout") || errorMsg.contains("timed out");
            
            if (isTimeout) {
                // Timeout likely occurred AFTER email was sent successfully
                // This is common with some SMTP servers that don't send final ACK quickly
                log.warn("SMTP timeout occurred for batch: {}. Email likely sent successfully but server didn't respond in time. " +
                        "Consider this a successful send unless recipient reports non-delivery.", 
                        dispatchBatch.getDispatchBatchNumber());
            } else {
                // Actual failure - log as error
                log.error("Failed to send dispatch notification email for batch: {} to: {}. Error: {}", 
                        dispatchBatch.getDispatchBatchNumber(), toEmail, e.getMessage(), e);
            }
        }
    }

    /**
     * Send dispatch notification email (synchronous version)
     * Creates JavaMailSender dynamically from provided SMTP configuration
     */
    public void sendDispatchNotification(
            DispatchBatch dispatchBatch,
            Long tenantId,
            String toEmail,
            String fromEmail,
            List<Long> documentIds,
            String customSubject,
            String customMessage,
            List<String> ccRecipients,
            DispatchEmailRequest.SmtpConfig smtpConfig,
            MultipartFile[] uploadedFiles) throws MessagingException, IOException {

        // Validate inputs
        if (dispatchBatch == null) {
            throw new IllegalArgumentException("Dispatch batch information is missing");
        }

        if (toEmail == null || toEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email address is required");
        }

        if (smtpConfig == null) {
            throw new IllegalArgumentException("SMTP configuration is required");
        }

        // Create mail sender with runtime SMTP config
        JavaMailSender mailSender = createMailSender(smtpConfig);

        // Get tenant details for default from email if not provided
        Tenant tenant = tenantService.getTenantById(tenantId);
        String senderEmail = (fromEmail != null && !fromEmail.trim().isEmpty()) 
                ? fromEmail 
                : (tenant.getEmail() != null && !tenant.getEmail().trim().isEmpty()) 
                    ? tenant.getEmail() 
                    : smtpConfig.getUsername(); // Use SMTP username as fallback

        // Prepare email content
        String subject = (customSubject != null && !customSubject.trim().isEmpty()) 
                ? customSubject 
                : buildDefaultSubject(dispatchBatch, tenant);
        
        String messageBody = (customMessage != null && !customMessage.trim().isEmpty()) 
                ? customMessage 
                : buildDefaultMessage(dispatchBatch, tenant);

        // Create email message
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(senderEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(messageBody, true); // true = HTML content

        // Add CC recipients if provided
        if (ccRecipients != null && !ccRecipients.isEmpty()) {
            helper.setCc(ccRecipients.toArray(new String[0]));
        }

        // Attach documents from system if provided
        if (documentIds != null && !documentIds.isEmpty()) {
            attachDocuments(helper, documentIds, tenantId);
        }
        
        // Attach uploaded files if provided
        if (uploadedFiles != null && uploadedFiles.length > 0) {
            attachUploadedFiles(helper, uploadedFiles);
        }

        // Send email
        try {
            mailSender.send(mimeMessage);
            log.info("Dispatch notification email sent successfully for batch: {} to: {}", 
                    dispatchBatch.getDispatchBatchNumber(), toEmail);
        } catch (Exception e) {
            // Check if it's a timeout exception
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            boolean isTimeout = errorMsg.contains("timeout") || errorMsg.contains("timed out");
            
            if (isTimeout) {
                // Log as warning - email was likely sent but server response was slow
                log.warn("SMTP timeout for batch: {} to: {}. Email likely delivered but server response was slow. " +
                        "Message: {}", dispatchBatch.getDispatchBatchNumber(), toEmail, e.getMessage());
                // Don't rethrow - consider this a success
            } else {
                // Actual send failure - rethrow
                log.error("Failed to send email for batch: {} to: {}. Error: {}", 
                        dispatchBatch.getDispatchBatchNumber(), toEmail, e.getMessage());
                throw e;
            }
        }
    }
    
    /**
     * Attach uploaded files to email
     */
    private void attachUploadedFiles(MimeMessageHelper helper, MultipartFile[] uploadedFiles)
            throws MessagingException, IOException {
        
        for (MultipartFile file : uploadedFiles) {
            if (file != null && !file.isEmpty()) {
                String fileName = file.getOriginalFilename();
                if (fileName == null || fileName.trim().isEmpty()) {
                    fileName = "uploaded_file_" + System.currentTimeMillis();
                }
                
                log.debug("Attaching uploaded file: {} (size: {} bytes)", fileName, file.getSize());
                
                // Add file as attachment using InputStreamSource
                helper.addAttachment(fileName, file);
            }
        }
    }

    /**
     * Attach documents to email
     */
    private void attachDocuments(MimeMessageHelper helper, List<Long> documentIds, Long tenantId) 
            throws MessagingException, IOException {
        
        for (Long documentId : documentIds) {
            try {
              if (documentId != null) {
                Document document = documentService.getDocumentByIdAndTenantId(documentId, tenantId);

                // Get file path - document.getFilePath() already contains the complete path
                Path filePath = Paths.get(document.getFilePath());

                if (!Files.exists(filePath)) {
                  log.warn("Document file not found: {}. Skipping attachment.", filePath);
                  continue;
                }

                // Read file content
                byte[] fileContent = Files.readAllBytes(filePath);

                // Decompress if needed
                if (Boolean.TRUE.equals(document.getIsCompressed())) {
                  fileContent = decompressFile(fileContent);
                }

                // Add attachment
                ByteArrayResource resource = new ByteArrayResource(fileContent);
                helper.addAttachment(document.getOriginalFileName(), resource);

                log.debug("Attached document: {} to email", document.getOriginalFileName());
              }
            } catch (Exception e) {
                log.error("Failed to attach document ID: {}. Continuing with other attachments.", documentId, e);
                // Continue with other attachments even if one fails
            }
        }
    }

    /**
     * Build default email subject
     */
    private String buildDefaultSubject(DispatchBatch dispatchBatch, Tenant tenant) {
        return String.format("Dispatch Notification - Batch %s from %s", 
                dispatchBatch.getDispatchBatchNumber(), 
                tenant.getTenantName());
    }

    /**
     * Build default email message body (HTML)
     */
    private String buildDefaultMessage(DispatchBatch dispatchBatch, Tenant tenant) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
        
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head><style>");
        sb.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        sb.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        sb.append(".header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }");
        sb.append(".content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; }");
        sb.append(".info-row { margin: 10px 0; }");
        sb.append(".label { font-weight: bold; color: #555; }");
        sb.append(".footer { margin-top: 20px; padding: 10px; text-align: center; font-size: 12px; color: #777; }");
        sb.append("</style></head>");
        sb.append("<body>");
        sb.append("<div class='container'>");
        
        // Header
        sb.append("<div class='header'>");
        sb.append("<h2>Dispatch Notification</h2>");
        sb.append("</div>");
        
        // Content
        sb.append("<div class='content'>");
        sb.append("<p>Dear ").append(dispatchBatch.getBuyer().getBuyerName()).append(",</p>");
        sb.append("<p>This is to inform you that the following dispatch has been completed:</p>");
        
        sb.append("<div class='info-row'>");
        sb.append("<span class='label'>Dispatch Batch Number:</span> ");
        sb.append(dispatchBatch.getDispatchBatchNumber());
        sb.append("</div>");
        
        if (dispatchBatch.getInvoiceNumber() != null) {
            sb.append("<div class='info-row'>");
            sb.append("<span class='label'>Invoice Number:</span> ");
            sb.append(dispatchBatch.getInvoiceNumber());
            sb.append("</div>");
        }
        
        if (dispatchBatch.getChallanNumber() != null) {
            sb.append("<div class='info-row'>");
            sb.append("<span class='label'>Challan Number:</span> ");
            sb.append(dispatchBatch.getChallanNumber());
            sb.append("</div>");
        }
        
        if (dispatchBatch.getDispatchedAt() != null) {
            sb.append("<div class='info-row'>");
            sb.append("<span class='label'>Dispatched On:</span> ");
            sb.append(dispatchBatch.getDispatchedAt().format(dateFormatter));
            sb.append("</div>");
        }
        
        if (dispatchBatch.getOrderPoNumber() != null) {
            sb.append("<div class='info-row'>");
            sb.append("<span class='label'>PO Number:</span> ");
            sb.append(dispatchBatch.getOrderPoNumber());
            sb.append("</div>");
        }
        
        sb.append("<div class='info-row'>");
        sb.append("<span class='label'>Shipping To:</span> ");
        sb.append(dispatchBatch.getShippingEntity().getBuyerEntityName());
        sb.append("</div>");
        
        sb.append("<p>Please find the attached documents for your reference.</p>");
        sb.append("<p>Thank you for your business.</p>");
        sb.append("</div>");
        
        // Footer
        sb.append("<div class='footer'>");
        sb.append("<p><strong>").append(tenant.getTenantName()).append("</strong></p>");
        if (tenant.getAddress() != null) {
            sb.append("<p>").append(tenant.getAddress()).append("</p>");
        }
        if (tenant.getPhoneNumber() != null) {
            sb.append("<p>Phone: ").append(tenant.getPhoneNumber()).append("</p>");
        }
        if (tenant.getEmail() != null) {
            sb.append("<p>Email: ").append(tenant.getEmail()).append("</p>");
        }
        sb.append("<p style='margin-top: 10px; font-size: 11px;'>This is an automated message from FOPMAS. Please do not reply to this email.</p>");
        sb.append("</div>");
        
        sb.append("</div>");
        sb.append("</body>");
        sb.append("</html>");
        
        return sb.toString();
    }

    /**
     * Decompress file content (simplified - assuming GZIP)
     */
    private byte[] decompressFile(byte[] compressedData) throws IOException {
        try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(compressedData);
             java.util.zip.GZIPInputStream gis = new java.util.zip.GZIPInputStream(bis);
             java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }

}
