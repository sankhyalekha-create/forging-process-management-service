package com.jangid.forging_process_management_service.service.gst;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jangid.forging_process_management_service.dto.gst.einvoice.*;
import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.TenantEInvoiceCredentials;
import com.jangid.forging_process_management_service.repositories.gst.InvoiceRepository;
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
 * Service for E-Invoice GSP API operations
 * Handles E-Invoice generation, IRN retrieval, and E-Way Bill generation from IRN
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GspEInvoiceService {

    private final EInvoiceAuthServiceWithSession authServiceWithSession;
    private final TenantEInvoiceCredentialsRepository credentialsRepository;
    private final InvoiceRepository invoiceRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EncryptionService encryptionService;
    private final GspServerFailoverHelper failoverHelper;

    @Value("${app.einvoice.gsp.generate-url}")
    private String generateUrl;

    @Value("${app.einvoice.gsp.irn-url}")
    private String irnUrl;

    @Value("${app.einvoice.gsp.ewb-by-irn-url}")
    private String ewbByIrnUrl;

    @Value("${app.einvoice.gsp.cancel-url}")
    private String cancelUrl;

    @Value("${app.einvoice.gsp.backup1-generate-url:}")
    private String backup1GenerateUrl;

    @Value("${app.einvoice.gsp.backup1-irn-url:}")
    private String backup1IrnUrl;

    @Value("${app.einvoice.gsp.backup1-ewb-by-irn-url:}")
    private String backup1EwbByIrnUrl;

    @Value("${app.einvoice.gsp.backup1-cancel-url:}")
    private String backup1CancelUrl;

    @Value("${app.einvoice.gsp.backup2-generate-url:}")
    private String backup2GenerateUrl;

    @Value("${app.einvoice.gsp.backup2-irn-url:}")
    private String backup2IrnUrl;

    @Value("${app.einvoice.gsp.backup2-ewb-by-irn-url:}")
    private String backup2EwbByIrnUrl;

    @Value("${app.einvoice.gsp.backup2-cancel-url:}")
    private String backup2CancelUrl;

    private static final long RETRY_DELAY_MS = 1000; // 1 second between retries
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Generate E-Invoice via GSP API with automatic failover
     * API: POST /eicore/dec/v1.03/Invoice?aspid=xxx&password=xxx&Gstin=xxx&AuthToken=xxx&user_name=xxx&QrCodeSize=250
     */
    public EInvoiceGenerateResponse generateEInvoice(Long tenantId, Long invoiceId, Object invoiceData, String sessionToken) {
        String[] serverUrls = {generateUrl, backup1GenerateUrl, backup2GenerateUrl};
        
        EInvoiceGenerateResponse response = failoverHelper.executeWithFailover(
            serverUrls,
            serverUrl -> {
                try {
                    return attemptGenerateEInvoice(tenantId, invoiceData, serverUrl, sessionToken);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            },
            "E-Invoice generation for tenant: " + tenantId
        );
        
        // Update invoice with E-Invoice details if successful
        if (response != null && response.isSuccess()) {
            updateInvoiceWithEInvoiceDetails(tenantId, invoiceId, response);
        }
        
        return response;
    }

    /**
     * Attempt E-Invoice generation on a specific server
     */
    private EInvoiceGenerateResponse attemptGenerateEInvoice(
            Long tenantId, Object invoiceData, String serverGenerateUrl, String sessionToken) throws Exception {
        try {
            // Get credentials
            TenantEInvoiceCredentials credentials = credentialsRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("E-Invoice credentials not found for tenant: " + tenantId));

            // Get auth token from session
            EInvoiceSessionService.SessionData session = authServiceWithSession.getSessionData(sessionToken);
            if (session == null) {
                throw new IllegalStateException("Invalid or expired session token");
            }
            String authToken = session.getGspAuthToken();

            // Get ASP credentials from database and decrypt password
            String aspId = credentials.getAspUserId();
            String aspPassword = encryptionService.decrypt(credentials.getAspPassword());

            // Build API URL with query parameters
            String apiUrl = String.format(
                "%s?aspid=%s&password=%s&Gstin=%s&AuthToken=%s&user_name=%s&QrCodeSize=250",
                serverGenerateUrl,
                aspId,
                aspPassword,
                credentials.getEinvGstin(),
                authToken,
                session.getEinvUsername()
            );

            log.debug("Calling Generate E-Invoice API: {} for tenant: {}", serverGenerateUrl, tenantId);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // The request body is the E-Invoice JSON data (NIC schema format)
            HttpEntity<Object> httpRequest = new HttpEntity<>(invoiceData, headers);

            // Call API
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                httpRequest,
                String.class
            );

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                throw new RuntimeException("No response from GSP");
            }

            log.debug("E-Invoice GSP Response: {}", responseBody);

            // Parse JSON response
            EInvoiceGenerateResponse einvResponse = objectMapper.readValue(responseBody, EInvoiceGenerateResponse.class);

            return einvResponse;

        } catch (Exception e) {
            log.error("Error generating E-Invoice for tenant: {}", tenantId, e);
            throw e; // Re-throw to be handled by caller
        }
    }

    /**
     * Get E-Invoice details by IRN with retry logic, DB caching, and automatic failover
     * API: GET /eicore/dec/v1.03/Invoice/irn/{irn}?aspid=xxx&password=xxx&Gstin=xxx&user_name=xxx&AuthToken=xxx
     */
    public EInvoiceIrnDetailsResponse getIrnDetails(Long tenantId, String irn, boolean forceRefresh, String sessionToken) {
        // Try to get from database cache first (unless force refresh)
        if (!forceRefresh) {
            try {
                Invoice invoice = invoiceRepository.findByIrnAndTenantIdAndDeletedFalse(irn, tenantId);
                if (invoice != null && invoice.getEinvoiceDetailsJson() != null && !invoice.getEinvoiceDetailsJson().isEmpty()) {
                    log.info("E-Invoice details found in DB cache for IRN: {}", irn);
                    EInvoiceIrnDetailsResponse cachedDetails = objectMapper.readValue(
                        invoice.getEinvoiceDetailsJson(), 
                        EInvoiceIrnDetailsResponse.class
                    );
                    return cachedDetails;
                }
            } catch (Exception e) {
                log.warn("Failed to retrieve E-Invoice details from DB cache for IRN: {}, will fetch from API", irn, e);
            }
        }

        String[] serverUrls = {irnUrl, backup1IrnUrl, backup2IrnUrl};
        
        EInvoiceIrnDetailsResponse irnDetails = failoverHelper.executeWithFailover(
            serverUrls,
            serverUrl -> {
                try {
                    return attemptGetIrnDetails(tenantId, irn, serverUrl, sessionToken);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            },
            "IRN details fetch for IRN: " + irn
        );
        
        // Cache the response in database
        cacheIrnDetails(tenantId, irn, irnDetails);
        
        return irnDetails;
    }

    /**
     * Attempt to get IRN details from a specific server
     */
    private EInvoiceIrnDetailsResponse attemptGetIrnDetails(
            Long tenantId, String irn, String serverIrnUrl, String sessionToken) throws Exception {
        try {
            TenantEInvoiceCredentials credentials = credentialsRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("E-Invoice credentials not found"));

            // Get auth token from session
            EInvoiceSessionService.SessionData session = authServiceWithSession.getSessionData(sessionToken);
            if (session == null) {
                throw new IllegalStateException("Invalid or expired session token");
            }
            String authToken = session.getGspAuthToken();

            // Get ASP credentials from database and decrypt password
            String aspId = credentials.getAspUserId();
            String aspPassword = encryptionService.decrypt(credentials.getAspPassword());

            // Build API URL with query parameters
            String apiUrl = String.format(
                "%s/%s?aspid=%s&password=%s&Gstin=%s&user_name=%s&AuthToken=%s",
                serverIrnUrl,
                irn,
                aspId,
                aspPassword,
                credentials.getEinvGstin(),
                session.getEinvUsername(),
                authToken
            );

            log.debug("Calling Get IRN API: {} for IRN: {}", serverIrnUrl, irn);

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                request,
                String.class
            );

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                throw new RuntimeException("No response from GSP Get IRN API");
            }

            log.debug("Get IRN API Response: {}", responseBody);

            EInvoiceIrnDetailsResponse irnDetails = objectMapper.readValue(responseBody, EInvoiceIrnDetailsResponse.class);
            
            return irnDetails;

        } catch (Exception e) {
            log.error("Error fetching IRN details", e);
            throw e; // Re-throw to be handled by caller
        }
    }

    /**
     * Cache IRN details in database
     */
    private void cacheIrnDetails(Long tenantId, String irn, EInvoiceIrnDetailsResponse irnDetails) {
        try {
            Invoice invoice = invoiceRepository.findByIrnAndTenantIdAndDeletedFalse(irn, tenantId);
            if (invoice != null) {
                String responseBody = objectMapper.writeValueAsString(irnDetails);
                invoice.setEinvoiceDetailsJson(responseBody);
                invoiceRepository.save(invoice);
                log.info("E-Invoice IRN details cached in DB for IRN: {}", irn);
            } else {
                log.warn("Invoice not found for IRN: {}, cannot cache details", irn);
            }
        } catch (Exception e) {
            log.error("Failed to cache E-Invoice IRN details in DB for IRN: {}", irn, e);
            // Don't fail the operation if caching fails
        }
    }

    /**
     * Get IRN details - convenience method without force refresh
     * Note: This method requires a sessionToken parameter
     */
    public EInvoiceIrnDetailsResponse getIrnDetails(Long tenantId, String irn, String sessionToken) {
        return getIrnDetails(tenantId, irn, false, sessionToken);
    }

    /**
     * Generate E-Way Bill from existing E-Invoice (by IRN)
     * API: POST /eiewb/dec/v1.03/ewaybill?aspid=xxx&password=xxx&Gstin=xxx&eInvPwd=xxx&AuthToken=xxx&user_name=xxx
     */
    public EInvoiceEwbByIrnResponse generateEwayBillByIrn(Long tenantId, EInvoiceGenerateEwbByIrnRequest request, String sessionToken) {
        String[] serverUrls = {ewbByIrnUrl, backup1EwbByIrnUrl, backup2EwbByIrnUrl};
        
        EInvoiceEwbByIrnResponse ewbResponse = failoverHelper.executeWithFailover(
            serverUrls,
            serverUrl -> {
                try {
                    return attemptGenerateEwayBillByIrn(tenantId, request, serverUrl, sessionToken);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            },
            "E-Way Bill generation from IRN: " + request.getIrn()
        );
        
        // Update invoice with E-Way Bill details if successful
        if (ewbResponse.isSuccess()) {
            updateInvoiceWithEwbDetails(tenantId, request.getIrn(), ewbResponse);
        }
        
        return ewbResponse;
    }

    /**
     * Attempt to generate E-Way Bill by IRN on a specific server
     */
    private EInvoiceEwbByIrnResponse attemptGenerateEwayBillByIrn(
            Long tenantId, EInvoiceGenerateEwbByIrnRequest request, String serverEwbByIrnUrl, String sessionToken) throws Exception {
        try {
            // Get credentials
            TenantEInvoiceCredentials credentials = credentialsRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("E-Invoice credentials not found for tenant: " + tenantId));

            // Get auth token from session
            EInvoiceSessionService.SessionData session = authServiceWithSession.getSessionData(sessionToken);
            if (session == null) {
                throw new IllegalStateException("Invalid or expired session token");
            }
            String authToken = session.getGspAuthToken();

            // Get ASP credentials from database and decrypt password
            String aspId = credentials.getAspUserId();
            String aspPassword = encryptionService.decrypt(credentials.getAspPassword());
            
            // Get E-Invoice password from session (decrypted)
            String einvPassword = encryptionService.decrypt(session.getEinvPassword());

            // Build API URL with query parameters
            String apiUrl = String.format(
                "%s?aspid=%s&password=%s&Gstin=%s&eInvPwd=%s&AuthToken=%s&user_name=%s",
                serverEwbByIrnUrl,
                aspId,
                aspPassword,
                credentials.getEinvGstin(),
                einvPassword,
                authToken,
                session.getEinvUsername()
            );

            log.debug("Calling Generate E-Way Bill by IRN API: {} for tenant: {}, IRN: {}", 
                     serverEwbByIrnUrl, tenantId, request.getIrn());

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<EInvoiceGenerateEwbByIrnRequest> httpRequest = new HttpEntity<>(request, headers);

            // Call API
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                httpRequest,
                String.class
            );

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                throw new RuntimeException("No response from GSP");
            }

            log.debug("Generate EWB by IRN Response: {}", responseBody);

            // Parse JSON response
            EInvoiceEwbByIrnResponse ewbResponse = objectMapper.readValue(responseBody, EInvoiceEwbByIrnResponse.class);

            return ewbResponse;

        } catch (Exception e) {
            log.error("Error generating E-Way Bill from IRN for tenant: {}, IRN: {}", tenantId, request.getIrn(), e);
            throw e; // Re-throw to be handled by caller
        }
    }

    /**
     * Update invoice with E-Way Bill details after generation from IRN
     */
    private void updateInvoiceWithEwbDetails(Long tenantId, String irn, EInvoiceEwbByIrnResponse ewbResponse) {
        try {
            Invoice invoice = invoiceRepository.findByIrnAndTenantIdAndDeletedFalse(irn, tenantId);
            if (invoice != null) {
                invoice.setEwayBillNumber(String.valueOf(ewbResponse.getEwbNo()));
                
                // Parse and set dates
                if (ewbResponse.getEwbDt() != null) {
                    try {
                        invoice.setEwayBillDate(LocalDateTime.parse(ewbResponse.getEwbDt(), DATE_TIME_FORMATTER));
                    } catch (Exception e) {
                        log.warn("Failed to parse E-Way Bill date: {}", ewbResponse.getEwbDt());
                    }
                }
                
                if (ewbResponse.getEwbValidTill() != null) {
                    try {
                        invoice.setEwayBillValidUntil(LocalDateTime.parse(ewbResponse.getEwbValidTill(), DATE_TIME_FORMATTER));
                    } catch (Exception e) {
                        log.warn("Failed to parse E-Way Bill valid until date: {}", ewbResponse.getEwbValidTill());
                    }
                }
                
                if (ewbResponse.getAlert() != null) {
                    invoice.setEwayBillAlertMessage(ewbResponse.getAlert());
                }
                
                invoiceRepository.save(invoice);
                log.info("Updated invoice with E-Way Bill details: EwbNo={} for IRN={}", 
                        ewbResponse.getEwbNo(), irn);
            } else {
                log.warn("Invoice not found for IRN: {}, cannot update with E-Way Bill details", irn);
            }
        } catch (Exception e) {
            log.error("Failed to update invoice with E-Way Bill details for IRN: {}", irn, e);
            // Don't fail the operation if update fails
        }
    }

    /**
     * Update invoice with E-Invoice generation response details
     */
    private void updateInvoiceWithEInvoiceDetails(Long tenantId, Long invoiceId, EInvoiceGenerateResponse response) {
        try {
            Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));
            
            // Set IRN
            if (response.getIrn() != null) {
                invoice.setIrn(response.getIrn());
            }
            
            // Set Acknowledgement Number
            if (response.getAckNo() != null) {
                invoice.setAckNo(String.valueOf(response.getAckNo()));
            }
            
            // Parse and set Acknowledgement Date
            if (response.getAckDt() != null) {
                try {
                    invoice.setAckDate(LocalDateTime.parse(response.getAckDt(), DATE_TIME_FORMATTER));
                    invoice.setEinvoiceDate(LocalDateTime.parse(response.getAckDt(), DATE_TIME_FORMATTER));
                } catch (Exception e) {
                    log.warn("Failed to parse acknowledgement date: {}", response.getAckDt(), e);
                }
            }
            
            // Set QR Code Data
            if (response.getSignedQRCode() != null) {
                invoice.setQrCodeData(response.getSignedQRCode());
            }
            
            // Set Alert Message
            if (response.getAlert() != null) {
                invoice.setEinvoiceAlertMessage(response.getAlert());
            }
            
            // If E-Way Bill was also generated along with E-Invoice
            if (response.hasEwayBill()) {
                invoice.setEwayBillNumber(String.valueOf(response.getEwbNo()));
                
                // Parse and set E-Way Bill Date
                if (response.getEwbDt() != null) {
                    try {
                        invoice.setEwayBillDate(LocalDateTime.parse(response.getEwbDt(), DATE_TIME_FORMATTER));
                    } catch (Exception e) {
                        log.warn("Failed to parse E-Way Bill date: {}", response.getEwbDt(), e);
                    }
                }
                
                // Parse and set E-Way Bill Valid Until
                if (response.getEwbValidTill() != null) {
                    try {
                        invoice.setEwayBillValidUntil(LocalDateTime.parse(response.getEwbValidTill(), DATE_TIME_FORMATTER));
                    } catch (Exception e) {
                        log.warn("Failed to parse E-Way Bill valid until date: {}", response.getEwbValidTill(), e);
                    }
                }
                
                // E-Way Bill alert message (if different from general alert)
                if (response.getAlert() != null) {
                    invoice.setEwayBillAlertMessage(response.getAlert());
                }
            }
            
            // Save the invoice with updated E-Invoice details
            invoiceRepository.save(invoice);
            
            log.info("Successfully updated invoice {} with E-Invoice details: IRN={}, AckNo={}, hasEwayBill={}", 
                    invoiceId, response.getIrn(), response.getAckNo(), response.hasEwayBill());
                    
        } catch (Exception e) {
            log.error("Failed to update invoice {} with E-Invoice details: {}", invoiceId, e.getMessage(), e);
            // Don't fail the E-Invoice generation if DB update fails
            // The response still contains all the details that can be shown to the user
        }
    }
}
