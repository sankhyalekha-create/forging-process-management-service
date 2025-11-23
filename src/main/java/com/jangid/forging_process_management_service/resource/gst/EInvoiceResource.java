package com.jangid.forging_process_management_service.resource.gst;

import com.jangid.forging_process_management_service.assemblers.gst.InvoiceAssembler;
import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.dto.gst.einvoice.*;
import com.jangid.forging_process_management_service.dto.gst.gsp.EwayBillSessionTokenResponse;
import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.TenantEInvoiceCredentials;
import com.jangid.forging_process_management_service.repositories.gst.InvoiceRepository;
import com.jangid.forging_process_management_service.repository.gst.TenantEInvoiceCredentialsRepository;
import com.jangid.forging_process_management_service.service.gst.EInvoiceAuthServiceWithSession;
import com.jangid.forging_process_management_service.service.gst.EInvoiceSessionService;
import com.jangid.forging_process_management_service.service.gst.GspEInvoiceService;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API for E-Invoice operations
 * Supports E-Invoice generation, IRN retrieval, and E-Way Bill generation from IRN
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Api(tags = "E-Invoice Management")
public class EInvoiceResource {

    private final EInvoiceAuthServiceWithSession authServiceWithSession;
    private final GspEInvoiceService einvoiceService;
    private final TenantEInvoiceCredentialsRepository credentialsRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceAssembler invoiceAssembler;
    private final EInvoiceSessionService sessionService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Test E-Invoice authentication with session-based credentials
     */
    @PostMapping("/einvoice/test-auth-session")
    @ApiOperation(value = "Test E-Invoice authentication with session",
                  notes = "Tests authentication with E-Invoice GSP and creates a session. Requires username and password.")
    public ResponseEntity<?> testAuthenticationWithSession(
        @ApiParam(value = "Session credentials", required = true)
        @Valid @RequestBody EInvoiceSessionCredentialsDTO sessionCredentials) {
        Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
        log.info("Testing E-Invoice authentication with session for tenant: {}", tenantId);

        try {
            // Authenticate and create session
            EwayBillSessionTokenResponse sessionResponse =
                authServiceWithSession.authenticateWithSessionCredentials(tenantId, sessionCredentials);

            // Get credentials to show additional info
            TenantEInvoiceCredentials credentials = credentialsRepository
                .findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("E-Invoice configuration not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Authentication successful");
            response.put("sessionToken", sessionResponse.getSessionToken());
            response.put("authToken", sessionResponse.getAuthToken());
            response.put("expiresAt", sessionResponse.getExpiresAt());
            response.put("createdAt", sessionResponse.getCreatedAt());
            response.put("gstin", sessionResponse.getGstin());
            response.put("isActive", credentials.getIsActive());

            log.info("E-Invoice authentication test with session successful for tenant: {}", tenantId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("E-Invoice authentication test with session failed for tenant: {}", tenantId, e);
            return GenericExceptionHandler.handleException(e, "testAuthenticationWithSession");
        }
    }

    /**
     * Get E-Invoice credentials status
     */
    @GetMapping("/einvoice/credentials/status")
    @ApiOperation(value = "Get E-Invoice credentials configuration status")
    public ResponseEntity<?> getCredentialsStatus() {
        Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

        try {
            TenantEInvoiceCredentials credentials = credentialsRepository
                .findByTenantId(tenantId)
                .orElse(null);

            Map<String, Object> status = new HashMap<>();

            if (credentials == null) {
                status.put("configured", false);
                status.put("message", "E-Invoice credentials not configured");
                return ResponseEntity.ok(status);
            }

            status.put("configured", true);
            status.put("isActive", credentials.getIsActive());
            status.put("einvGstin", credentials.getEinvGstin());
            status.put("hasAuthToken", credentials.getAuthToken() != null);
            status.put("isTokenValid", credentials.isTokenValid());
            status.put("tokenExpiry", credentials.getTokenExpiry());
            status.put("hasValidCredentials", credentials.hasValidCredentials());
            status.put("isGspApiMode", credentials.isGspApiMode());
            status.put("einvThreshold", credentials.getEinvThreshold());
            status.put("message", "E-Invoice configuration found. For session-based auth, credentials are provided per-session (not stored).");

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Error fetching E-Invoice credentials status for tenant: {}", tenantId, e);
            return GenericExceptionHandler.handleException(e, "getCredentialsStatus");
        }
    }

    /**
     * Refresh session by re-authenticating
     */
    @PostMapping("/einvoice/refresh-session")
    @ApiOperation(value = "Refresh E-Invoice session by re-authenticating",
                  notes = "Creates a new session by re-authenticating with provided credentials. Old session will be invalidated.")
    public ResponseEntity<?> refreshSession(
        @ApiParam(value = "Session credentials", required = true)
        @Valid @RequestBody EInvoiceSessionCredentialsDTO sessionCredentials) {
        Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
        log.info("Refreshing E-Invoice session for tenant: {}", tenantId);

        try {
            // Force new authentication by providing credentials (not sessionToken)
            if (sessionCredentials.getEinvUsername() == null || sessionCredentials.getEinvPassword() == null) {
                throw new IllegalArgumentException("Username and password required to refresh session");
            }

            // Clear any existing session token
            sessionCredentials.setSessionToken(null);

            // Create new session
            EwayBillSessionTokenResponse sessionResponse = 
                authServiceWithSession.authenticateWithSessionCredentials(tenantId, sessionCredentials);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Session refreshed successfully");
            response.put("sessionToken", sessionResponse.getSessionToken());
            response.put("authToken", sessionResponse.getAuthToken());
            response.put("expiresAt", sessionResponse.getExpiresAt());
            response.put("createdAt", sessionResponse.getCreatedAt());

            log.info("E-Invoice session refreshed successfully for tenant: {}", tenantId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("E-Invoice session refresh failed for tenant: {}", tenantId, e);
            return GenericExceptionHandler.handleException(e, "refreshSession");
        }
    }

    /**
     * Generate E-Invoice via GSP API for a specific invoice with session-based credentials
     * Returns complete InvoiceRepresentation with all E-Invoice details for PDF generation
     */
    @PostMapping("/einvoice/invoices/{invoiceId}/generate")
    @ApiOperation(value = "Generate E-Invoice via GSP API for invoice",
                  notes = "Generates E-Invoice by calling GSP API, updates invoice with IRN and other details, " +
                          "and returns complete invoice representation for PDF generation. " +
                          "Requires either sessionToken (if session exists) or username/password (for new session).")
    public ResponseEntity<?> generateEInvoice(
        @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId,
        @ApiParam(value = "E-Invoice generation request with session credentials", required = true)
        @Valid @RequestBody EInvoiceGenerateRequest request) {

        Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
        log.info("Generating E-Invoice via GSP API for invoice: {}, tenant: {}", invoiceId, tenantId);

        try {
            // Check if E-Invoice already exists
            Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));
            
            if (invoice.getIrn() != null && !invoice.getIrn().isEmpty()) {
                log.warn("E-Invoice already exists for invoice: {}, IRN: {}", 
                        invoiceId, invoice.getIrn());
                throw new IllegalArgumentException(
                    "E-Invoice already generated for this invoice. IRN: " + invoice.getIrn());
            }

            // Step 1: Resolve session token (validate existing or create new session)
            String sessionToken = resolveSessionToken(tenantId, request.getSessionCredentials());

            // Step 2: Generate E-Invoice via GSP API (service will update invoice with details)
            EInvoiceGenerateResponse response = einvoiceService.generateEInvoice(
                tenantId, invoiceId, request.getEinvoiceData(), sessionToken);

            // Build response with GSP response details and complete invoice data
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("sessionToken", sessionToken); // Return session token for frontend to cache
            result.put("irn", response.getIrn());
            result.put("ackNo", response.getAckNo());
            result.put("ackDt", response.getAckDt());
            result.put("signedQRCode", response.getSignedQRCode());
            result.put("hasEwayBill", response.hasEwayBill());
            
            if (response.hasEwayBill()) {
                result.put("ewbNo", response.getEwbNo());
                result.put("ewbDt", response.getEwbDt());
                result.put("ewbValidTill", response.getEwbValidTill());
            }
            
            result.put("alert", response.getAlert());

            if (response.isSuccess()) {
                log.info("E-Invoice generated successfully: IRN={} for invoice: {}", 
                        response.getIrn(), invoiceId);
                
                // Fetch updated invoice and convert to representation
                Invoice updatedInvoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found after E-Invoice generation"));
                
                // Use InvoiceAssembler to get complete invoice representation with all details
                result.put("invoice", invoiceAssembler.disassemble(updatedInvoice));
                
            } else {
                log.error("E-Invoice generation failed: {}", response.getErrorDetails());
                result.put("error", response.getErrorDetails());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("E-Invoice generation failed for invoice {}: {}", invoiceId, e.getMessage(), e);
            return GenericExceptionHandler.handleException(e, "generateEInvoice");
        }
    }

    /**
     * Get E-Invoice details by IRN with session-based credentials
     */
    @PostMapping("/einvoice/irn/{irn}")
    @ApiOperation(value = "Get E-Invoice details by IRN",
                  notes = "Retrieves complete E-Invoice details from GSP API using IRN. Requires session credentials.")
    public ResponseEntity<?> getIrnDetails(
        @ApiParam(value = "Invoice Reference Number (IRN)", required = true) @PathVariable String irn,
        @ApiParam(value = "Session credentials", required = true)
        @Valid @RequestBody EInvoiceSessionCredentialsDTO sessionCredentials,
        @ApiParam(value = "Force refresh from API (bypass cache)") @RequestParam(defaultValue = "false") boolean forceRefresh) {

        Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
        log.info("Fetching E-Invoice details for IRN: {}, tenant: {}, forceRefresh: {}", 
                irn, tenantId, forceRefresh);

        try {
            // Resolve session token
            String sessionToken = resolveSessionToken(tenantId, sessionCredentials);

            EInvoiceIrnDetailsResponse response = einvoiceService.getIrnDetails(tenantId, irn, forceRefresh, sessionToken);

            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("irnDetails", response);

            if (response.isSuccess()) {
                log.info("E-Invoice IRN details fetched successfully: IRN={}, Status={}", 
                        irn, response.getStatus());
            } else {
                log.error("Failed to fetch IRN details: {}", response.getErrorDetails());
                result.put("error", response.getErrorDetails());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to fetch E-Invoice IRN details: {}", irn, e);
            return GenericExceptionHandler.handleException(e, "getIrnDetails");
        }
    }

    /**
     * Get E-Invoice details for an invoice
     */
    @GetMapping("/einvoice/invoices/{invoiceId}")
    @ApiOperation(value = "Get E-Invoice details for invoice")
    public ResponseEntity<?> getInvoiceEInvoiceDetails(
        @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId) {

        try {
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
            Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));

            Map<String, Object> result = new HashMap<>();
            result.put("invoiceId", invoiceId);
            result.put("hasEInvoice", invoice.getIrn() != null && !invoice.getIrn().isEmpty());
            result.put("irn", invoice.getIrn());
            result.put("ackNo", invoice.getAckNo());
            result.put("ackDate", invoice.getAckDate());
            result.put("einvoiceDate", invoice.getEinvoiceDate());
            result.put("qrCodeData", invoice.getQrCodeData());
            result.put("hasEwayBill", invoice.getEwayBillNumber() != null);
            result.put("ewayBillNumber", invoice.getEwayBillNumber());
            result.put("ewayBillDate", invoice.getEwayBillDate());
            result.put("ewayBillValidUntil", invoice.getEwayBillValidUntil());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error fetching E-Invoice details for invoice: {}", invoiceId, e);
            return GenericExceptionHandler.handleException(e, "getInvoiceEInvoiceDetails");
        }
    }

    /**
     * Generate E-Way Bill from existing E-Invoice (by IRN) with session-based credentials
     */
    @PostMapping("/einvoice/irn/{irn}/generate-ewb")
    @ApiOperation(value = "Generate E-Way Bill from existing E-Invoice",
                  notes = "Generates E-Way Bill using existing IRN and transportation details. " +
                          "Requires either sessionToken (if session exists) or username/password (for new session).")
    public ResponseEntity<?> generateEwayBillByIrn(
        @ApiParam(value = "Invoice Reference Number (IRN)", required = true) @PathVariable String irn,
        @ApiParam(value = "E-Way Bill generation request with session credentials", required = true)
        @Valid @RequestBody EInvoiceEwbByIrnRequest request) {

        Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
        log.info("Generating E-Way Bill from IRN: {} for tenant: {}", irn, tenantId);

        try {
            // Step 1: Resolve session token (validate existing or create new session)
            String sessionToken = resolveSessionToken(tenantId, request.getSessionCredentials());

            // Set IRN in request data
            request.getEwbData().setIrn(irn);

            // Step 2: Generate E-Way Bill
            EInvoiceEwbByIrnResponse response = einvoiceService.generateEwayBillByIrn(
                tenantId, request.getEwbData(), sessionToken);

            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("sessionToken", sessionToken); // Return session token for frontend to cache
            result.put("irn", irn);
            
            if (response.isSuccess()) {
                result.put("ewbNo", response.getEwbNo());
                result.put("ewbDt", response.getEwbDt());
                result.put("ewbValidTill", response.getEwbValidTill());
                result.put("alert", response.getAlert());
                
                log.info("E-Way Bill generated successfully from IRN: EwbNo={}, IRN={}", 
                        response.getEwbNo(), irn);
            } else {
                result.put("error", response.getErrorDetails());
                log.error("E-Way Bill generation from IRN failed: {}", response.getErrorDetails());
            }
            
            result.put("fullResponse", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error generating E-Way Bill from IRN {}: {}", irn, e.getMessage(), e);
            return GenericExceptionHandler.handleException(e, "generateEwayBillByIrn");
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Resolve session token from credentials
     * If sessionToken provided and valid → use it
     * If NOT → authenticate and create new session
     * 
     * @param tenantId Tenant ID
     * @param credentials Session credentials (may contain sessionToken or username/password)
     * @return Valid session token
     */
    private String resolveSessionToken(Long tenantId, EInvoiceSessionCredentialsDTO credentials) {
        // Case 1: Session token provided - validate it
        if (credentials.getSessionToken() != null && !credentials.getSessionToken().isEmpty()) {
            EInvoiceSessionService.SessionData session = 
                authServiceWithSession.getSessionData(credentials.getSessionToken());
            
            if (session != null && session.getTenantId().equals(tenantId)) {
                log.debug("Using existing valid E-Invoice session for tenant: {}", tenantId);
                return credentials.getSessionToken();
            }
            
            log.warn("Invalid or expired E-Invoice session token for tenant: {}, creating new session", tenantId);
        }
        
        // Case 2: No valid session - authenticate with credentials
        if (credentials.getEinvUsername() == null || credentials.getEinvPassword() == null) {
            throw new IllegalArgumentException(
                "E-Invoice credentials required: session expired or not found. " +
                "Please provide username and password.");
        }
        
        log.info("Creating new E-Invoice session for tenant: {}", tenantId);
        EwayBillSessionTokenResponse sessionResponse = 
            authServiceWithSession.authenticateWithSessionCredentials(tenantId, credentials);
        
        return sessionResponse.getSessionToken();
    }
}
