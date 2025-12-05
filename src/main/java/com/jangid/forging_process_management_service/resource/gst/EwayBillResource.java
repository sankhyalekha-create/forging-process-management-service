package com.jangid.forging_process_management_service.resource.gst;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.dto.gst.EwayBillGenerateRequest;
import com.jangid.forging_process_management_service.dto.gst.EwayBillJsonFormat;
import com.jangid.forging_process_management_service.dto.gst.gsp.*;
import com.jangid.forging_process_management_service.service.gst.EwayBillSessionService;
import com.jangid.forging_process_management_service.service.gst.GspServerConfigService;
import com.jangid.forging_process_management_service.entities.gst.DeliveryChallan;
import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.TenantEwayBillCredentials;
import com.jangid.forging_process_management_service.repositories.gst.DeliveryChallanRepository;
import com.jangid.forging_process_management_service.repositories.gst.InvoiceRepository;
import com.jangid.forging_process_management_service.repository.gst.TenantEwayBillCredentialsRepository;
import com.jangid.forging_process_management_service.service.gst.EwayBillExportService;
import com.jangid.forging_process_management_service.service.gst.EwayBillGenerationService;
import com.jangid.forging_process_management_service.service.gst.GspAuthServiceWithSession;
import com.jangid.forging_process_management_service.service.gst.GspEwayBillService;
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

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified REST API for E-Way Bill operations
 * Supports both offline JSON generation and GSP API integration
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Api(tags = "E-Way Bill Management")
public class EwayBillResource {

  private final EwayBillExportService ewayBillExportService;
  private final InvoiceRepository invoiceRepository;
  private final DeliveryChallanRepository challanRepository;
  private final ObjectMapper objectMapper;
  
  // GSP Integration dependencies
  private final GspAuthServiceWithSession gspAuthServiceWithSession;
  private final GspEwayBillService gspEwayBillService;
  private final TenantEwayBillCredentialsRepository credentialsRepository;
  private final EwayBillGenerationService ewayBillGenerationService;
  private final DocumentService documentService;
  private final GspServerConfigService gspServerConfigService;

  /**
   * Get available GSP servers for E-Way Bill
   */
  @GetMapping("/eway-bill/gsp/servers")
  @ApiOperation(value = "Get available GSP servers for E-Way Bill",
                notes = "Returns list of configured GSP servers that users can select")
  public ResponseEntity<?> getAvailableServers() {
    try {
      List<GspServerDTO> servers = gspServerConfigService.getAvailableEwbServers();
      return ResponseEntity.ok(servers);
    } catch (Exception e) {
      log.error("Error fetching available E-Way Bill servers", e);
      return GenericExceptionHandler.handleException(e, "getAvailableServers",
          "Failed to fetch available servers: " + e.getMessage());
    }
  }

  /**
   * Download E-Way Bill JSON for Invoice
   * User downloads this JSON file and uploads it manually to ewaybillgst.gov.in
   */
  @GetMapping("/invoices/{invoiceId}/eway-bill/json")
  @ApiOperation(value = "Download E-Way Bill JSON for manual upload to GST portal",
                notes = "Generates NIC-compliant JSON file that can be uploaded to ewaybillgst.gov.in")
  public ResponseEntity<?> downloadInvoiceEwayBillJson(
      @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId) {

    Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
    log.info("Generating E-Way Bill JSON for invoice: {}, tenant: {}", invoiceId, tenantId);

    try {
      // Generate E-Way Bill JSON
      EwayBillJsonFormat ewbJson = ewayBillExportService.generateEwayBillJsonForInvoice(tenantId, invoiceId);

      // Convert to JSON string with pretty print
      String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(ewbJson);

      // Create downloadable resource
      ByteArrayResource resource = new ByteArrayResource(jsonContent.getBytes(StandardCharsets.UTF_8));

      // Generate filename
      String docNo = ewbJson.getBillLists().get(0).getDocNo();
      String filename = String.format("eway_bill_%s.json",
                                      docNo.replaceAll("[^a-zA-Z0-9]", "_"));

      log.info("Successfully generated E-Way Bill JSON for invoice: {}, filename: {}", invoiceId, filename);

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + filename)
          .contentType(MediaType.APPLICATION_JSON)
          .contentLength(resource.contentLength())
          .body(resource);

    } catch (Exception e) {
      log.error("Error generating E-Way Bill JSON for invoice: {}", invoiceId, e);
      return GenericExceptionHandler.handleException(e, "downloadInvoiceEwayBillJson");
    }
  }

  /**
   * Download E-Way Bill JSON for Delivery Challan
   */
  @GetMapping("/challans/{challanId}/eway-bill/json")
  @ApiOperation(value = "Download E-Way Bill JSON for challan",
                notes = "Generates NIC-compliant JSON file for delivery challan")
  public ResponseEntity<?> downloadChallanEwayBillJson(
      @ApiParam(value = "Challan ID", required = true) @PathVariable Long challanId) {
    Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

    log.info("Generating E-Way Bill JSON for challan: {}, tenant: {}", challanId, tenantId);

    try {
      // Generate E-Way Bill JSON format
      EwayBillJsonFormat ewbJson = ewayBillExportService.generateEwayBillJsonForChallan(tenantId, challanId);

      // Get challan for filename
      DeliveryChallan challan = challanRepository.findByIdAndTenantIdAndDeletedFalse(challanId, tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Challan not found with id: " + challanId));

      // Serialize to JSON
      ObjectMapper mapper = new ObjectMapper();
      String jsonContent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ewbJson);

      // Create resource for download
      ByteArrayResource resource = new ByteArrayResource(jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));

      // Generate filename
      String docNo = challan.getChallanNumber().replaceAll("[^a-zA-Z0-9]", "_");
      String filename = String.format("eway_bill_challan_%s.json", docNo);

      log.info("Successfully generated E-Way Bill JSON for challan: {}, filename: {}", challanId, filename);

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + filename)
          .contentType(MediaType.APPLICATION_JSON)
          .contentLength(resource.contentLength())
          .body(resource);

    } catch (Exception e) {
      log.error("Error generating E-Way Bill JSON for challan: {}", challanId, e);
      return GenericExceptionHandler.handleException(e, "downloadChallanEwayBillJson");
    }
  }


  /**
   * Get E-Way Bill status for Invoice
   */
  @GetMapping("/invoices/{invoiceId}/eway-bill")
  @ApiOperation(value = "Get E-Way Bill details for invoice")
  public ResponseEntity<?> getInvoiceEwayBillDetails(
      @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId) {

    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      Invoice invoice = invoiceRepository.findById(invoiceId)
          .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));

      if (invoice.getTenant().getId() != tenantId) {
        throw new IllegalArgumentException("Invoice does not belong to the specified tenant");
      }

      return ResponseEntity.ok(Map.of(
          "hasEwayBill", invoice.getEwayBillNumber() != null,
          "ewayBillNumber", invoice.getEwayBillNumber() != null ? invoice.getEwayBillNumber() : "",
          "ewayBillDate", invoice.getEwayBillDate() != null ? invoice.getEwayBillDate() : "",
          "validUntil", invoice.getEwayBillValidUntil() != null ? invoice.getEwayBillValidUntil() : "",
          "supplyType", invoice.getEwayBillSupplyType() != null ? invoice.getEwayBillSupplyType() : "",
          "subSupplyType", invoice.getEwayBillSubSupplyType() != null ? invoice.getEwayBillSubSupplyType() : "",
          "docType", invoice.getEwayBillDocType() != null ? invoice.getEwayBillDocType() : "",
          "transactionType", invoice.getEwayBillTransactionType() != null ? invoice.getEwayBillTransactionType() : ""
      ));

    } catch (Exception e) {
      log.error("Error fetching E-Way Bill details for invoice: {}", invoiceId, e);
      return GenericExceptionHandler.handleException(e, "getInvoiceEwayBillDetails");
    }
  }

  // ==================== GSP API Integration Endpoints ====================

  /**
   * Generate E-Way Bill via GSP API for a specific invoice with session-based credentials
   */
  @PostMapping("/invoices/{invoiceId}/eway-bill/gsp/generate")
  @ApiOperation(value = "Generate E-Way Bill via GSP API for invoice",
                notes = "Generates E-Way Bill by calling GSP API and updates invoice with the details. " +
                        "Requires either sessionToken (if session exists) or username/password (for new session).")
  public ResponseEntity<?> generateEwayBillViaGsp(
      @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId,
      @ApiParam(value = "E-Way Bill generation request with session credentials", required = true)
      @Valid @RequestBody EwayBillGenerateRequest request) {

    Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
    log.info("Generating E-Way Bill via GSP API for invoice: {}, tenant: {}", invoiceId, tenantId);

    try {
      // Check if E-Way Bill already exists
      Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));
      
      if (invoice.getEwayBillNumber() != null && !invoice.getEwayBillNumber().isEmpty()) {
        log.warn("E-Way Bill already exists for invoice: {}, E-Way Bill Number: {}", 
                invoiceId, invoice.getEwayBillNumber());
        throw new IllegalArgumentException(
            "E-Way Bill already generated for this invoice. E-Way Bill Number: " + invoice.getEwayBillNumber());
      }

      // Step 1: Resolve session token (validate existing or create new session)
      String sessionToken = resolveSessionToken(tenantId, request.getSessionCredentials());

      // Step 2: Extract GSP Server ID from request
      String gspServerId = request.getSessionCredentials().getGspServerId();

      // Step 3: Delegate to service layer with retry logic and session token
      GspEwbGenerateResponse response = ewayBillGenerationService.generateEwayBillWithRetry(
          tenantId, invoice, request.getEwayBillData(), sessionToken, gspServerId);

      // Build success response
      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "E-Way Bill generated successfully");
      result.put("sessionToken", sessionToken); // Return session token for frontend to cache
      result.put("invoiceId", invoiceId);
      result.put("ewayBillNumber", response.getEwayBillNo());
      result.put("ewayBillDate", response.getEwayBillDate());
      result.put("validUntil", response.getValidUpto());
      result.put("alert", response.getAlert());
      result.put("fullResponse", response);

      log.info("E-Way Bill generated successfully: {} for invoice: {}", response.getEwayBillNo(), invoiceId);
      return ResponseEntity.ok(result);

    } catch (Exception e) {
      log.error("E-Way Bill generation failed for invoice {}: {}", invoiceId, e.getMessage(), e);
      return GenericExceptionHandler.handleException(e, "generateEwayBillViaGsp");
    }
  }

  /**
   * Get E-Way Bill details from GSP API by E-Way Bill number with session-based credentials
   */
  @PostMapping("/invoices/{invoiceId}/eway-bill/gsp/{ewayBillNumber}/details")
  @ApiOperation(value = "Get E-Way Bill details from GSP API",
                notes = "Requires either sessionToken (if session exists) or username/password (for new session).")
  public ResponseEntity<?> getEwayBillDetailsFromGsp(
      @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId,
      @ApiParam(value = "E-Way Bill number", required = true) @PathVariable Long ewayBillNumber,
      @ApiParam(value = "Session credentials", required = true)
      @Valid @RequestBody EwayBillSessionCredentialsDTO sessionCredentials) {

    Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
    log.info("Fetching E-Way Bill details via GSP API: {} for invoice: {}", ewayBillNumber, invoiceId);

    try {
      // Verify invoice exists and belongs to tenant
      Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));

      // Verify E-Way Bill number matches invoice
      if (invoice.getEwayBillNumber() != null && !invoice.getEwayBillNumber().equals(String.valueOf(ewayBillNumber))) {
        log.warn("E-Way Bill number mismatch: invoice has {}, requested {}", 
                invoice.getEwayBillNumber(), ewayBillNumber);
      }

      // Resolve session token
      String sessionToken = resolveSessionToken(tenantId, sessionCredentials);

      GspEwbDetailResponse response = gspEwayBillService.getEwayBillDetails(tenantId, ewayBillNumber, sessionToken, sessionCredentials.getGspServerId());

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("sessionToken", sessionToken);
      result.put("invoiceId", invoiceId);
      result.put("ewayBillDetails", response);

      log.info("E-Way Bill details fetched successfully: {}", ewayBillNumber);
      return ResponseEntity.ok(result);

    } catch (Exception e) {
      log.error("Failed to fetch E-Way Bill details: {}", ewayBillNumber, e);
      return GenericExceptionHandler.handleException(e, "getEwayBillDetailsFromGsp");
    }
  }

  /**
   * Test GSP authentication with session-based credentials
   */
  @PostMapping("/eway-bill/gsp/test-auth")
  @ApiOperation(value = "Test GSP authentication",
                notes = "Tests authentication with GSP and creates a session. Requires username and password.")
  public ResponseEntity<?> testGspAuthentication(
      @ApiParam(value = "Session credentials", required = true)
      @Valid @RequestBody EwayBillSessionCredentialsDTO sessionCredentials) {
    Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
    log.info("Testing GSP authentication for tenant: {}", tenantId);

    try {
      // Authenticate and create session
      EwayBillSessionTokenResponse sessionResponse = 
          gspAuthServiceWithSession.authenticateWithSessionCredentials(tenantId, sessionCredentials);

      // Get credentials to show additional info
      TenantEwayBillCredentials credentials = credentialsRepository
          .findByTenantId(tenantId)
          .orElseThrow(() -> new IllegalStateException("GSP credentials not configured"));

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "Authentication successful");
      response.put("sessionToken", sessionResponse.getSessionToken());
      response.put("authToken", sessionResponse.getAuthToken());
      response.put("expiresAt", sessionResponse.getExpiresAt());
      response.put("createdAt", sessionResponse.getCreatedAt());
      response.put("gstin", sessionResponse.getGstin());
      response.put("isActive", credentials.getIsActive());

      log.info("GSP authentication test successful for tenant: {}", tenantId);
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("GSP authentication test failed for tenant: {}", tenantId, e);
      return GenericExceptionHandler.handleException(e, "testGspAuthentication");
    }
  }

  /**
   * Get GSP credentials configuration status
   */
  @GetMapping("/eway-bill/gsp/credentials/status")
  @ApiOperation(value = "Get GSP credentials configuration status")
  public ResponseEntity<?> getGspCredentialsStatus() {
    Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

    try {
      TenantEwayBillCredentials credentials = credentialsRepository
          .findByTenantId(tenantId)
          .orElse(null);

      Map<String, Object> status = new HashMap<>();

      if (credentials == null) {
        status.put("configured", false);
        status.put("message", "GSP credentials not configured");
        return ResponseEntity.ok(status);
      }

      status.put("configured", true);
      status.put("isActive", credentials.getIsActive());
      status.put("ewbGstin", credentials.getEwbGstin());
      status.put("hasValidCredentials", credentials.hasValidCredentials());
      status.put("isGspApiMode", credentials.isGspApiMode());
      status.put("ewbThreshold", credentials.getEwbThreshold());
      status.put("message", "GSP configuration found. Credentials are provided per-session (not stored).");

      return ResponseEntity.ok(status);

    } catch (Exception e) {
      log.error("Error fetching credentials status for tenant: {}", tenantId, e);
      return GenericExceptionHandler.handleException(e, "getGspCredentialsStatus");
    }
  }

  /**
   * Refresh session by re-authenticating
   * This replaces the old refresh token mechanism
   */
  @PostMapping("/eway-bill/gsp/refresh-session")
  @ApiOperation(value = "Refresh session by re-authenticating",
                notes = "Creates a new session by re-authenticating with provided credentials. Old session will be invalidated.")
  public ResponseEntity<?> refreshSession(
      @ApiParam(value = "Session credentials", required = true)
      @Valid @RequestBody EwayBillSessionCredentialsDTO sessionCredentials) {
    Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
    log.info("Refreshing session for tenant: {}", tenantId);

    try {
      // Force new authentication by providing credentials (not sessionToken)
      if (sessionCredentials.getEwbUsername() == null || sessionCredentials.getEwbPassword() == null) {
        throw new IllegalArgumentException("Username and password required to refresh session");
      }

      // Clear any existing session token
      sessionCredentials.setSessionToken(null);

      // Create new session
      EwayBillSessionTokenResponse sessionResponse = 
          gspAuthServiceWithSession.authenticateWithSessionCredentials(tenantId, sessionCredentials);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "Session refreshed successfully");
      response.put("sessionToken", sessionResponse.getSessionToken());
      response.put("authToken", sessionResponse.getAuthToken());
      response.put("expiresAt", sessionResponse.getExpiresAt());
      response.put("createdAt", sessionResponse.getCreatedAt());

      log.info("Session refreshed successfully for tenant: {}", tenantId);
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Session refresh failed for tenant: {}", tenantId, e);
      return GenericExceptionHandler.handleException(e, "refreshSession");
    }
  }

  /**
   * Print E-Way Bill via GSP API with session-based credentials
   * First checks if E-Way Bill PDF already exists as a document (cached)
   * If cached PDF exists, returns it directly without calling GSP API
   * If not cached, verifies E-Way Bill exists using GetEwayBill API, generates PDF, and caches it
   * User must generate E-Way Bill first using generateEwayBillViaGsp endpoint
   */
  @PostMapping("/invoices/{invoiceId}/eway-bill/gsp/print")
  @ApiOperation(value = "Print E-Way Bill via GSP API",
                notes = "Verifies E-Way Bill exists and generates PDF. E-Way Bill must be generated first. " +
                        "Requires either sessionToken (if session exists) or username/password (for new session). " +
                        "PDF is cached after first generation for performance.")
  public ResponseEntity<?> printEwayBill(
      @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId,
      @ApiParam(value = "Session credentials", required = true)
      @Valid @RequestBody EwayBillSessionCredentialsDTO sessionCredentials) {

    Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
    log.info("Printing E-Way Bill for invoice: {}, tenant: {}", invoiceId, tenantId);

    try {
      // Verify invoice exists and belongs to tenant
      Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));

      // Check if E-Way Bill number exists in invoice
      if (invoice.getEwayBillNumber() == null || invoice.getEwayBillNumber().isEmpty()) {
        log.warn("E-Way Bill not generated for invoice: {}", invoiceId);
        throw new IllegalArgumentException(
            "E-Way Bill not generated for this invoice. Please generate E-Way Bill first using the Generate E-Way Bill option.");
      }

      Long ewayBillNumber = Long.parseLong(invoice.getEwayBillNumber());
      
      // Step 1: Check if E-Way Bill PDF already exists as a document (cached)
      List<Document> existingDocs = documentService.getDocumentsForEntity(
          tenantId, 
          DocumentLink.EntityType.INVOICE, 
          invoiceId
      );
      
      // Look for E-Way Bill PDF in existing documents
      // Search criteria: DocumentCategory.INVOICE + title/description contains "E-Way Bill" + ewayBillNumber
      Document cachedEwayBillPdf = existingDocs.stream()
          .filter(doc -> doc.getDocumentCategory() == DocumentCategory.INVOICE)
          .filter(doc -> {
            String description = doc.getDescription() != null ? doc.getDescription().toLowerCase() : "";
            String fileName = doc.getOriginalFileName() != null ? doc.getOriginalFileName().toLowerCase() : "";
            String ewbNum = invoice.getEwayBillNumber();
            
            // Must contain "E-Way Bill" (or "eway") AND the specific E-Way Bill number
            boolean hasEwayKeyword = description.contains("e-way bill") || description.contains("eway") ||
                                   fileName.contains("eway_bill");
            boolean hasEwbNumber = description.contains(ewbNum) || fileName.contains(ewbNum);
            
            return hasEwayKeyword && hasEwbNumber;
          })
          .findFirst()
          .orElse(null);
      
      if (cachedEwayBillPdf != null) {
        log.info("Found cached E-Way Bill PDF for invoice: {}, returning cached version", invoiceId);
        
        // Read the cached PDF file
        byte[] cachedPdfBytes = documentService.readDocumentFile(cachedEwayBillPdf);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=eway_bill_" + ewayBillNumber + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(cachedPdfBytes);
      }
      
      // Step 2: No cached PDF found, proceed with GSP API call
      log.info("No cached E-Way Bill PDF found. Fetching from GSP for invoice: {}", invoiceId);
      
      // Step 3: Resolve session token (validate existing or create new session)
      String sessionToken = resolveSessionToken(tenantId, sessionCredentials);

      // Step 3.5: Extract GSP Server ID from request
      String gspServerId = sessionCredentials.getGspServerId();

      // Step 4: Get E-Way Bill details from GSP to verify it exists
      GspEwbDetailResponse ewbDetails;
      try {
        ewbDetails = gspEwayBillService.getEwayBillDetails(tenantId, ewayBillNumber, sessionToken, gspServerId);
        
        // Check if E-Way Bill is active
        if (!"ACT".equals(ewbDetails.getStatus())) {
          log.error("E-Way Bill is not active. Status: {}", ewbDetails.getStatus());
          throw new RuntimeException("E-Way Bill is not active. Status: " + ewbDetails.getStatus());
        }
        
        log.info("E-Way Bill details fetched successfully from GSP: {}, Status: {}", 
                ewayBillNumber, ewbDetails.getStatus());
      } catch (Exception e) {
        log.error("Error fetching E-Way Bill details from GSP for ewbNo: {}", ewayBillNumber, e);
        throw new RuntimeException("Failed to verify E-Way Bill from GSP system. " +
                                 "E-Way Bill may have been cancelled or does not exist. Error: " + e.getMessage(), e);
      }

      // Step 5: Call Print API to get PDF bytes
      byte[] pdfBytes = gspEwayBillService.printEwayBill(tenantId, ewbDetails, sessionToken, gspServerId);

      log.info("E-Way Bill printed successfully: {} for invoice: {}", ewayBillNumber, invoiceId);
      
      // Step 6: Cache the PDF as a document for future requests
      try {
        String fileName = "eway_bill_" + ewayBillNumber + ".pdf";
        String title = "E-Way Bill " + ewayBillNumber;
        String description = "E-Way Bill PDF for Invoice " + invoice.getInvoiceNumber() + 
                           " generated on " + LocalDateTime.now().toString().substring(0, 19);
        
        Document cachedDocument = documentService.attachPdfBytesToEntity(
            tenantId,
            DocumentLink.EntityType.INVOICE,
            invoiceId,
            pdfBytes,
            fileName,
            DocumentCategory.INVOICE,
            title,
            description,
            "eway-bill,gsp,auto-generated"
        );
        
        log.info("E-Way Bill PDF cached successfully as document ID: {} for invoice: {}", 
                cachedDocument.getId(), invoiceId);
      } catch (Exception e) {
        // Don't fail the request if caching fails - just log the error
        log.error("Failed to cache E-Way Bill PDF for invoice: {}, continuing with response", invoiceId, e);
      }

      // Return PDF file
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, 
                  "attachment; filename=eway_bill_" + ewayBillNumber + ".pdf")
          .contentType(MediaType.APPLICATION_PDF)
          .body(pdfBytes);

    } catch (Exception e) {
      log.error("Failed to print E-Way Bill for invoice: {}", invoiceId, e);
      return GenericExceptionHandler.handleException(e, "printEwayBill");
    }
  }

  // ==================== Challan GSP API Integration Endpoints ====================

  /**
   * Get E-Way Bill status for Delivery Challan
   */
  @GetMapping("/challans/{challanId}/eway-bill")
  @ApiOperation(value = "Get E-Way Bill details for delivery challan")
  public ResponseEntity<?> getChallanEwayBillDetails(
      @ApiParam(value = "Challan ID", required = true) @PathVariable Long challanId) {

    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      DeliveryChallan challan = challanRepository.findByIdAndTenantIdAndDeletedFalse(challanId, tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Delivery Challan not found with id: " + challanId));

      return ResponseEntity.ok(Map.of(
          "hasEwayBill", challan.getEwayBillNumber() != null,
          "ewayBillNumber", challan.getEwayBillNumber() != null ? challan.getEwayBillNumber() : "",
          "ewayBillDate", challan.getEwayBillDate() != null ? challan.getEwayBillDate() : "",
          "validUntil", challan.getEwayBillValidUntil() != null ? challan.getEwayBillValidUntil() : ""
      ));

    } catch (Exception e) {
      log.error("Error fetching E-Way Bill details for challan: {}", challanId, e);
      return GenericExceptionHandler.handleException(e, "getChallanEwayBillDetails");
    }
  }

  /**
   * Generate E-Way Bill via GSP API for a specific delivery challan with session-based credentials
   */
  @PostMapping("/challans/{challanId}/eway-bill/gsp/generate")
  @ApiOperation(value = "Generate E-Way Bill via GSP API for delivery challan",
                notes = "Generates E-Way Bill by calling GSP API and updates challan with the details. " +
                        "Requires either sessionToken (if session exists) or username/password (for new session).")
  public ResponseEntity<?> generateChallanEwayBillViaGsp(
      @ApiParam(value = "Challan ID", required = true) @PathVariable Long challanId,
      @ApiParam(value = "E-Way Bill generation request with session credentials", required = true)
      @Valid @RequestBody EwayBillGenerateRequest request) {

    Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
    log.info("Generating E-Way Bill via GSP API for challan: {}, tenant: {}", challanId, tenantId);

    try {
      // Check if E-Way Bill already exists
      DeliveryChallan challan = challanRepository.findByIdAndTenantIdAndDeletedFalse(challanId, tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Delivery Challan not found with id: " + challanId));
      
      if (challan.getEwayBillNumber() != null && !challan.getEwayBillNumber().isEmpty()) {
        log.warn("E-Way Bill already exists for challan: {}, E-Way Bill Number: {}", 
                challanId, challan.getEwayBillNumber());
        throw new IllegalArgumentException(
            "E-Way Bill already generated for this challan. E-Way Bill Number: " + challan.getEwayBillNumber());
      }

      // Step 1: Resolve session token (validate existing or create new session)
      String sessionToken = resolveSessionToken(tenantId, request.getSessionCredentials());

      // Step 1.5: Extract GSP Server ID from request
      String gspServerId = request.getSessionCredentials().getGspServerId();

      // Step 2: Delegate to service layer with retry logic and session token
      GspEwbGenerateResponse response = ewayBillGenerationService.generateEwayBillWithRetry(
          tenantId, challan, request.getEwayBillData(), sessionToken, gspServerId);

      // Build success response
      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "E-Way Bill generated successfully for challan");
      result.put("sessionToken", sessionToken); // Return session token for frontend to cache
      result.put("challanId", challanId);
      result.put("ewayBillNumber", response.getEwayBillNo());
      result.put("ewayBillDate", response.getEwayBillDate());
      result.put("validUntil", response.getValidUpto());
      result.put("alert", response.getAlert());
      result.put("fullResponse", response);

      log.info("E-Way Bill generated successfully: {} for challan: {}", response.getEwayBillNo(), challanId);
      return ResponseEntity.ok(result);

    } catch (Exception e) {
      log.error("E-Way Bill generation failed for challan {}: {}", challanId, e.getMessage(), e);
      return GenericExceptionHandler.handleException(e, "generateChallanEwayBillViaGsp");
    }
  }

  /**
   * Print E-Way Bill via GSP API for delivery challan with session-based credentials
   * First checks if E-Way Bill PDF already exists as a document (cached)
   * If cached PDF exists, returns it directly without calling GSP API
   * If not cached, verifies E-Way Bill exists using GetEwayBill API, generates PDF, and caches it
   */
  @PostMapping("/challans/{challanId}/eway-bill/gsp/print")
  @ApiOperation(value = "Print E-Way Bill via GSP API for delivery challan",
                notes = "Verifies E-Way Bill exists and generates PDF. E-Way Bill must be generated first. " +
                        "Requires either sessionToken (if session exists) or username/password (for new session). " +
                        "PDF is cached after first generation for performance.")
  public ResponseEntity<?> printChallanEwayBill(
      @ApiParam(value = "Challan ID", required = true) @PathVariable Long challanId,
      @ApiParam(value = "Session credentials", required = true)
      @Valid @RequestBody EwayBillSessionCredentialsDTO sessionCredentials) {

    Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
    log.info("Printing E-Way Bill for challan: {}, tenant: {}", challanId, tenantId);

    try {
      // Verify challan exists and belongs to tenant
      DeliveryChallan challan = challanRepository.findByIdAndTenantIdAndDeletedFalse(challanId, tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Delivery Challan not found with id: " + challanId));

      // Check if E-Way Bill number exists in challan
      if (challan.getEwayBillNumber() == null || challan.getEwayBillNumber().isEmpty()) {
        log.warn("E-Way Bill not generated for challan: {}", challanId);
        throw new IllegalArgumentException(
            "E-Way Bill not generated for this challan. Please generate E-Way Bill first.");
      }

      Long ewayBillNumber = Long.parseLong(challan.getEwayBillNumber());
      
      // Step 1: Check if E-Way Bill PDF already exists as a document (cached)
      List<Document> existingDocs = documentService.getDocumentsForEntity(
          tenantId, 
          DocumentLink.EntityType.CHALLAN, 
          challanId
      );
      
      // Look for E-Way Bill PDF in existing documents
      // Search criteria: DocumentCategory.INVOICE + title/description contains "E-Way Bill" + ewayBillNumber
      Document cachedEwayBillPdf = existingDocs.stream()
          .filter(doc -> doc.getDocumentCategory() == DocumentCategory.INVOICE)
          .filter(doc -> {
            String description = doc.getDescription() != null ? doc.getDescription().toLowerCase() : "";
            String fileName = doc.getOriginalFileName() != null ? doc.getOriginalFileName().toLowerCase() : "";
            String ewbNum = challan.getEwayBillNumber();
            
            // Must contain "E-Way Bill" (or "eway") AND the specific E-Way Bill number
            boolean hasEwayKeyword = description.contains("e-way bill") || description.contains("eway") ||
                                   fileName.contains("eway_bill");
            boolean hasEwbNumber = description.contains(ewbNum) || fileName.contains(ewbNum);
            
            return hasEwayKeyword && hasEwbNumber;
          })
          .findFirst()
          .orElse(null);
      
      if (cachedEwayBillPdf != null) {
        log.info("Found cached E-Way Bill PDF for challan: {}, returning cached version", challanId);
        
        // Read the cached PDF file
        byte[] cachedPdfBytes = documentService.readDocumentFile(cachedEwayBillPdf);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=eway_bill_challan_" + ewayBillNumber + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(cachedPdfBytes);
      }
      
      // Step 2: No cached PDF found, proceed with GSP API call
      log.info("No cached E-Way Bill PDF found. Fetching from GSP for challan: {}", challanId);
      
      // Step 3: Resolve session token (validate existing or create new session)
      String sessionToken = resolveSessionToken(tenantId, sessionCredentials);

      // Step 4: Get E-Way Bill details from GSP to verify it exists
      GspEwbDetailResponse ewbDetails;
      try {
        ewbDetails = gspEwayBillService.getEwayBillDetails(tenantId, ewayBillNumber, sessionToken, sessionCredentials.getGspServerId());
        
        // Check if E-Way Bill is active
        if (!"ACT".equals(ewbDetails.getStatus())) {
          log.error("E-Way Bill is not active. Status: {}", ewbDetails.getStatus());
          throw new RuntimeException("E-Way Bill is not active. Status: " + ewbDetails.getStatus());
        }
        
        log.info("E-Way Bill details fetched successfully from GSP: {}, Status: {}", 
                ewayBillNumber, ewbDetails.getStatus());
      } catch (Exception e) {
        log.error("Error fetching E-Way Bill details from GSP for ewbNo: {}", ewayBillNumber, e);
        throw new RuntimeException("Failed to verify E-Way Bill from GSP system. " +
                                 "E-Way Bill may have been cancelled or does not exist. Error: " + e.getMessage(), e);
      }

      // Step 5: Call Print API to get PDF bytes
      byte[] pdfBytes = gspEwayBillService.printEwayBill(tenantId, ewbDetails, sessionToken, sessionCredentials.getGspServerId());

      log.info("E-Way Bill printed successfully: {} for challan: {}", ewayBillNumber, challanId);
      
      // Step 6: Cache the PDF as a document for future requests
      try {
        String fileName = "eway_bill_challan_" + ewayBillNumber + ".pdf";
        String title = "E-Way Bill " + ewayBillNumber;
        String description = "E-Way Bill PDF for Challan " + challan.getChallanNumber() + 
                           " generated on " + LocalDateTime.now().toString().substring(0, 19);
        
        Document cachedDocument = documentService.attachPdfBytesToEntity(
            tenantId,
            DocumentLink.EntityType.CHALLAN,
            challanId,
            pdfBytes,
            fileName,
            DocumentCategory.INVOICE,
            title,
            description,
            "eway-bill,gsp,auto-generated,challan"
        );
        
        log.info("E-Way Bill PDF cached successfully as document ID: {} for challan: {}", 
                cachedDocument.getId(), challanId);
      } catch (Exception e) {
        // Don't fail the request if caching fails - just log the error
        log.error("Failed to cache E-Way Bill PDF for challan: {}, continuing with response", challanId, e);
      }

      // Return PDF file
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, 
                  "attachment; filename=eway_bill_challan_" + ewayBillNumber + ".pdf")
          .contentType(MediaType.APPLICATION_PDF)
          .body(pdfBytes);

    } catch (Exception e) {
      log.error("Failed to print E-Way Bill for challan: {}", challanId, e);
      return GenericExceptionHandler.handleException(e, "printChallanEwayBill");
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
  private String resolveSessionToken(Long tenantId, EwayBillSessionCredentialsDTO credentials) {
    // Case 1: Session token provided - validate it
    if (credentials.getSessionToken() != null && !credentials.getSessionToken().isEmpty()) {
      EwayBillSessionService.SessionData session = 
          gspAuthServiceWithSession.getSessionData(credentials.getSessionToken());
      
      if (session != null && session.getTenantId().equals(tenantId)) {
        log.debug("Using existing valid session for tenant: {}", tenantId);
        return credentials.getSessionToken();
      }
      
      log.warn("Invalid or expired session token for tenant: {}, creating new session", tenantId);
    }
    
    // Case 2: No valid session - authenticate with credentials
    if (credentials.getEwbUsername() == null || credentials.getEwbPassword() == null) {
      throw new IllegalArgumentException(
          "E-Way Bill credentials required: session expired or not found. " +
          "Please provide username and password.");
    }
    
    log.info("Creating new session for tenant: {}", tenantId);
    EwayBillSessionTokenResponse sessionResponse = 
        gspAuthServiceWithSession.authenticateWithSessionCredentials(tenantId, credentials);
    
    return sessionResponse.getSessionToken();
  }
}
