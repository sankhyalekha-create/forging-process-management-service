package com.jangid.forging_process_management_service.resource.gst;

import com.jangid.forging_process_management_service.assemblers.gst.InvoiceAssembler;
import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.dto.gst.einvoice.*;
import com.jangid.forging_process_management_service.dto.gst.gsp.EwayBillSessionTokenResponse;
import com.jangid.forging_process_management_service.dto.gst.gsp.GspServerDTO;
import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.TenantEInvoiceCredentials;
import com.jangid.forging_process_management_service.repositories.gst.InvoiceRepository;
import com.jangid.forging_process_management_service.repository.gst.TenantEInvoiceCredentialsRepository;
import com.jangid.forging_process_management_service.service.gst.EInvoiceAuthServiceWithSession;
import com.jangid.forging_process_management_service.service.gst.EInvoiceSessionService;
import com.jangid.forging_process_management_service.service.gst.GspEInvoiceService;
import com.jangid.forging_process_management_service.service.gst.GspServerConfigService;
import com.jangid.forging_process_management_service.service.document.DocumentService;
import com.jangid.forging_process_management_service.entities.document.Document;
import com.jangid.forging_process_management_service.entities.document.DocumentLink;
import com.jangid.forging_process_management_service.entities.document.DocumentCategory;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
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
    private final DocumentService documentService;
    private final GspServerConfigService gspServerConfigService;

    /**
     * Get available GSP servers for E-Invoice
     */
    @GetMapping("/einvoice/gsp/servers")
    @ApiOperation(value = "Get available GSP servers for E-Invoice",
                  notes = "Returns list of configured GSP servers that users can select")
    public ResponseEntity<?> getAvailableServers() {
        try {
            List<GspServerDTO> servers = gspServerConfigService.getAvailableEinvServers();
            return ResponseEntity.ok(servers);
        } catch (Exception e) {
            log.error("Error fetching available E-Invoice servers", e);
            return GenericExceptionHandler.handleException(e, "getAvailableServers",
                "Failed to fetch available servers: " + e.getMessage());
        }
    }

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
     * Returns complete InvoiceRepresentation with all E-Invoice details
     * Frontend will generate and cache E-Invoice PDF
     */
    @PostMapping("/einvoice/invoices/{invoiceId}/generate")
    @ApiOperation(value = "Generate E-Invoice via GSP API for invoice",
                  notes = "Generates E-Invoice by calling GSP API, updates invoice with IRN and other details, " +
                          "and returns complete invoice representation. " +
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
                tenantId, invoiceId, request.getEinvoiceData(), sessionToken, request.getSessionCredentials().getGspServerId());

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
                // Frontend will use this to generate and cache E-Invoice PDF
                result.put("invoice", invoiceAssembler.disassemble(updatedInvoice));
                
                log.info("E-Invoice data prepared successfully for invoice: {}", invoiceId);
                
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

            EInvoiceIrnDetailsResponse response = einvoiceService.getIrnDetails(tenantId, irn, forceRefresh, sessionToken, sessionCredentials.getGspServerId());

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
                tenantId, request.getEwbData(), sessionToken, request.getSessionCredentials().getGspServerId());

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

    /**
     * Generate E-Invoice with E-Way Bill and return merged PDF
     * 
     * This endpoint orchestrates the complete workflow:
     * 1. Generate E-Invoice via GSP API
     * 2. Generate E-Way Bill by IRN using the generated IRN
     * 3. Update invoice with E-Way Bill details
     * 4. Generate E-Invoice PDF (3 pages: ORIGINAL, DUPLICATE, TRIPLICATE)
     * 5. Print E-Way Bill PDF (1 page)
     * 6. Merge both PDFs (E-Invoice 3 pages + E-Way Bill 1 page = 4 pages total)
     * 7. Return merged PDF as base64 string
     * 
     * @param invoiceId Invoice ID
     * @param request Combined request with E-Invoice data, E-Way Bill data, and session credentials
     * @return Response with merged PDF as base64 string
     */
    @PostMapping("/einvoice/invoices/{invoiceId}/generate-with-ewb")
    @ApiOperation(value = "Generate E-Invoice with E-Way Bill and return merged PDF",
                  notes = "Generates E-Invoice, E-Way Bill by IRN, creates 3-page E-Invoice PDF, " +
                          "prints E-Way Bill PDF, merges them, and returns as base64 string. " +
                          "Requires either sessionToken (if session exists) or username/password (for new session).")
    public ResponseEntity<?> generateEInvoiceWithEwayBill(
        @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId,
        @ApiParam(value = "Combined E-Invoice and E-Way Bill generation request", required = true)
        @Valid @RequestBody EInvoiceWithEwbGenerateRequest request) {

        Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
        log.info("Starting combined E-Invoice and E-Way Bill generation for invoice: {}, tenant: {}", 
                invoiceId, tenantId);

        try {
            // ============ STEP 1: Validate invoice ============
            Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));
            
            if (invoice.getIrn() != null && !invoice.getIrn().isEmpty()) {
                log.warn("E-Invoice already exists for invoice: {}, IRN: {}", invoiceId, invoice.getIrn());
                throw new IllegalArgumentException(
                    "E-Invoice already generated for this invoice. IRN: " + invoice.getIrn());
            }

            // ============ STEP 2: Resolve session token ============
            String sessionToken = resolveSessionToken(tenantId, request.getSessionCredentials());
            log.info("Session token resolved for tenant: {}", tenantId);

            // ============ STEP 3: Generate E-Invoice ============
            log.info("Step 1/5: Generating E-Invoice for invoice: {}", invoiceId);
            EInvoiceGenerateResponse einvoiceResponse = einvoiceService.generateEInvoice(
                tenantId, invoiceId, request.getEinvoiceData(), sessionToken, request.getSessionCredentials().getGspServerId());

            if (!einvoiceResponse.isSuccess()) {
                log.error("E-Invoice generation failed: {}", einvoiceResponse.getErrorDetails());
                throw new RuntimeException("E-Invoice generation failed: " + einvoiceResponse.getErrorDetails());
            }

            String irn = einvoiceResponse.getIrn();
            log.info("Step 1/5 completed: E-Invoice generated successfully, IRN: {}", irn);

            // ============ STEP 4: Generate E-Way Bill by IRN ============
            log.info("Step 2/5: Generating E-Way Bill by IRN: {}", irn);
            
            // Set IRN in E-Way Bill request
            request.getEwbData().setIrn(irn);
            
            EInvoiceEwbByIrnResponse ewbResponse = einvoiceService.generateEwayBillByIrn(
                tenantId, request.getEwbData(), sessionToken, request.getSessionCredentials().getGspServerId());

            if (!ewbResponse.isSuccess()) {
                log.error("E-Way Bill generation failed: {}", ewbResponse.getErrorDetails());
                throw new RuntimeException("E-Way Bill generation failed: " + ewbResponse.getErrorDetails());
            }

            Long ewbNo = ewbResponse.getEwbNo();
            log.info("Step 2/5 completed: E-Way Bill generated successfully, EwbNo: {}", ewbNo);

            // Reload invoice to get updated E-Way Bill details
            invoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found after E-Way Bill generation"));

            // ============ STEP 5: Generate E-Invoice PDF (3 pages) ============
            log.info("Step 3/5: Generating E-Invoice PDF (3 pages) for invoice: {}", invoiceId);
            byte[] einvoicePdfBytes = null;
            int einvoicePageCount = 0;
            log.info("Step 3/5 completed: E-Invoice PDF generated, pages: {}, size: {} bytes", 
                    einvoicePageCount, einvoicePdfBytes.length);

            // ============ STEP 6: Print E-Way Bill PDF (1 page) ============
            log.info("Step 4/5: Printing E-Way Bill PDF for EwbNo: {}", ewbNo);
            byte[] ewbPdfBytes = einvoiceService.printEwayBillByIrn(tenantId, ewbNo, sessionToken, request.getSessionCredentials().getGspServerId());
            int ewbPageCount = 0;
            log.info("Step 4/5 completed: E-Way Bill PDF printed, pages: {}, size: {} bytes", 
                    ewbPageCount, ewbPdfBytes.length);

            // ============ STEP 7: Merge PDFs ============
            log.info("Step 5/5: Merging E-Invoice PDF ({} pages) and E-Way Bill PDF ({} page)", 
                    einvoicePageCount, ewbPageCount);
            byte[] mergedPdfBytes = null;
            int totalPageCount = 0;
            log.info("Step 5/5 completed: PDFs merged successfully, total pages: {}, size: {} bytes", 
                    totalPageCount, mergedPdfBytes.length);

            // ============ STEP 8: Convert to Base64 ============
            String mergedPdfBase64 = Base64.getEncoder().encodeToString(mergedPdfBytes);
            
            // ============ STEP 9: Build response ============
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "E-Invoice and E-Way Bill generated successfully with merged PDF");
            result.put("sessionToken", sessionToken);
            
            // E-Invoice details
            result.put("irn", irn);
            result.put("ackNo", einvoiceResponse.getAckNo());
            result.put("ackDt", einvoiceResponse.getAckDt());
            result.put("signedQRCode", einvoiceResponse.getSignedQRCode());
            
            // E-Way Bill details
            result.put("ewbNo", ewbNo);
            result.put("ewbDt", ewbResponse.getEwbDt());
            result.put("ewbValidTill", ewbResponse.getEwbValidTill());
            result.put("ewbAlert", ewbResponse.getAlert());
            
            // PDF details
            result.put("mergedPdfBase64", mergedPdfBase64);
            result.put("mergedPdfSizeBytes", mergedPdfBytes.length);
            result.put("einvoicePages", einvoicePageCount);
            result.put("ewbPages", ewbPageCount);
            result.put("totalPages", totalPageCount);
            
            // Complete invoice data
            Invoice finalInvoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
            result.put("invoice", invoiceAssembler.disassemble(finalInvoice));

            log.info("Successfully completed combined E-Invoice and E-Way Bill generation: " +
                    "Invoice: {}, IRN: {}, EwbNo: {}, PDF pages: {}", 
                    invoiceId, irn, ewbNo, totalPageCount);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Combined E-Invoice and E-Way Bill generation failed for invoice {}: {}", 
                    invoiceId, e.getMessage(), e);
            return GenericExceptionHandler.handleException(e, "generateEInvoiceWithEwayBill");
        }
    }

    /**
     * Print E-Invoice PDF
     * First checks if E-Invoice PDF already exists as a cached document
     * If cached PDF exists, returns it directly as PDF file
     * If not cached, returns invoice data as JSON for frontend to generate and cache PDF
     */
    @GetMapping("/einvoice/invoices/{invoiceId}/print")
    @ApiOperation(value = "Print E-Invoice PDF",
                  notes = "Returns cached E-Invoice PDF if available, otherwise returns invoice data for frontend PDF generation. " +
                          "E-Invoice must be generated first (IRN must exist).")
    public ResponseEntity<?> printEInvoice(
        @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId) {

        Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
        log.info("Printing E-Invoice for invoice: {}, tenant: {}", invoiceId, tenantId);

        try {
            // Verify invoice exists and belongs to tenant
            Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));

            // Check if E-Invoice exists (IRN must be present)
            if (invoice.getIrn() == null || invoice.getIrn().isEmpty()) {
                log.warn("E-Invoice not generated for invoice: {}", invoiceId);
                throw new IllegalArgumentException(
                    "E-Invoice not generated for this invoice. Please generate E-Invoice first.");
            }

            String irn = invoice.getIrn();
            
            // Check if E-Invoice PDF already exists as a cached document
            List<Document> existingDocs = documentService.getDocumentsForEntity(
                tenantId, 
                DocumentLink.EntityType.INVOICE, 
                invoiceId
            );
            
            // Look for E-Invoice PDF in existing documents
            Document cachedEInvoicePdf = existingDocs.stream()
                .filter(doc -> doc.getDocumentCategory() == DocumentCategory.INVOICE)
                .filter(doc -> doc.getDescription() != null && doc.getDescription().contains("E-Invoice"))
                .filter(doc -> doc.getDescription() != null && doc.getDescription().contains(irn))
                .filter(doc -> "application/pdf".equals(doc.getMimeType()))
                .findFirst()
                .orElse(null);
            
            if (cachedEInvoicePdf != null) {
                log.info("Found cached E-Invoice PDF for invoice: {}, returning cached PDF", invoiceId);
                
                // Read the cached PDF file
                byte[] cachedPdfBytes = documentService.readDocumentFile(cachedEInvoicePdf);
                
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=einvoice_" + invoice.getInvoiceNumber().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(cachedPdfBytes);
            }
            
            // No cached PDF found - return invoice data for frontend to generate PDF
            log.info("No cached E-Invoice PDF found for invoice: {}, returning invoice data for frontend generation", invoiceId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "E-Invoice PDF not cached. Frontend will generate and cache it.");
            result.put("pdfCached", false);
            result.put("invoice", invoiceAssembler.disassemble(invoice));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to print E-Invoice for invoice: {}", invoiceId, e);
            return GenericExceptionHandler.handleException(e, "printEInvoice");
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
