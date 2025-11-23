package com.jangid.forging_process_management_service.service.gst;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jangid.forging_process_management_service.dto.gst.EwayBillData;
import com.jangid.forging_process_management_service.dto.gst.gsp.GspEwbGenerateResponse;
import com.jangid.forging_process_management_service.dto.gst.gsp.GspEwbDetailResponse;
import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.TenantEwayBillCredentials;
import com.jangid.forging_process_management_service.repositories.gst.InvoiceRepository;
import com.jangid.forging_process_management_service.repository.gst.TenantEwayBillCredentialsRepository;
import com.jangid.forging_process_management_service.service.security.EncryptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Service for GSP E-Way Bill API operations with Session Support
 * Handles generation, cancellation, vehicle update, etc.
 * Now supports session-based credentials instead of stored credentials
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GspEwayBillService {

  private final GspAuthServiceWithSession authService;
  private final TenantEwayBillCredentialsRepository credentialsRepository;
  private final InvoiceRepository invoiceRepository;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final EncryptionService encryptionService;
  private final GspServerFailoverHelper failoverHelper;

  @org.springframework.beans.factory.annotation.Value("${app.eway-bill.gsp.ewb-url}")
  private String ewbUrl;

  @org.springframework.beans.factory.annotation.Value("${app.eway-bill.gsp.print-url}")
  private String printUrl;

  @org.springframework.beans.factory.annotation.Value("${app.eway-bill.gsp.backup1-ewb-url:}")
  private String backup1EwbUrl;

  @org.springframework.beans.factory.annotation.Value("${app.eway-bill.gsp.backup2-ewb-url:}")
  private String backup2EwbUrl;

  @org.springframework.beans.factory.annotation.Value("${app.eway-bill.gsp.backup1-print-url:}")
  private String backup1PrintUrl;

  @org.springframework.beans.factory.annotation.Value("${app.eway-bill.gsp.backup2-print-url:}")
  private String backup2PrintUrl;

  private static final long RETRY_DELAY_MS = 1000; // 1 second between retries

  /**
   * Generate E-Way Bill via GSP API with Session Support
   * Using new POST API format with query parameters and automatic failover
   * 
   * @param tenantId Tenant ID
   * @param ewayBillData E-Way Bill data
   * @param sessionToken Session token from user authentication
   * @return E-Way Bill generation response
   */
  public GspEwbGenerateResponse generateEwayBill(Long tenantId, EwayBillData ewayBillData, String sessionToken) {
    // Sanitize document number for GSP API compatibility
    // GSP API error 206: "Invalid Invoice Number" - some implementations reject special characters
    // Replace forward slashes and other problematic special characters with hyphens
    if (ewayBillData.getDocNo() != null && !ewayBillData.getDocNo().isEmpty()) {
      String originalDocNo = ewayBillData.getDocNo();
      String sanitizedDocNo = originalDocNo
//          .replace("/", "-")   // Replace forward slash (most common issue)
          .replace("\\", "-")  // Replace backslash
          .replace(" ", "")    // Remove spaces
          .trim();             // Trim whitespace
      
      // Ensure max length of 16 characters as per E-Way Bill spec
//      if (sanitizedDocNo.length() > 16) {
//        sanitizedDocNo = sanitizedDocNo.substring(0, 16);
//        log.warn("Document number truncated to 16 characters: {} -> {}", originalDocNo, sanitizedDocNo);
//      }
      
      if (!originalDocNo.equals(sanitizedDocNo)) {
        log.info("Sanitized document number for GSP API: '{}' -> '{}'", originalDocNo, sanitizedDocNo);
        ewayBillData.setDocNo(sanitizedDocNo);
      }
    }
    
    String[] serverUrls = {ewbUrl, backup1EwbUrl, backup2EwbUrl};
    
    return failoverHelper.executeWithFailover(
        serverUrls,
        serverUrl -> {
          try {
            return attemptGenerateEwayBill(tenantId, ewayBillData, serverUrl, sessionToken);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        },
        "E-Way Bill generation for DocNo: " + ewayBillData.getDocNo()
    );
  }

  /**
   * Attempt E-Way Bill generation on a specific server with Session Support
   * 
   * @param tenantId Tenant ID
   * @param ewayBillData E-Way Bill data
   * @param serverEwbUrl Server URL
   * @param sessionToken Session token
   * @return E-Way Bill generation response
   */
  private GspEwbGenerateResponse attemptGenerateEwayBill(
      Long tenantId, EwayBillData ewayBillData, String serverEwbUrl, String sessionToken) throws Exception {
    
    try {
      // Get credentials from session
      EwayBillSessionService.SessionData session = authService.getSessionData(sessionToken);
      if (session == null) {
        throw new IllegalArgumentException("Invalid or expired session token");
      }

      // Validate tenant ID matches session
      if (!session.getTenantId().equals(tenantId)) {
        throw new IllegalArgumentException("Session token does not belong to this tenant");
      }

      // Get tenant configuration
      TenantEwayBillCredentials credentials = credentialsRepository.findByTenantId(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("E-Way Bill credentials not found for tenant: " + tenantId));

      // Get auth token from session
      String authToken = session.getGspAuthToken();
      String ewbUsername = session.getEwbUsername();

      // Get ASP credentials from database and decrypt password
      String aspId = credentials.getAspUserId();
      String aspPassword = encryptionService.decrypt(credentials.getAspPassword());

      // Build API URL with query parameters
      // Format: POST /ewayapi?action=GENEWAYBILL&aspid=<aspid>&password=<asppwd>&gstin=<gstin>&username=<user>&authtoken=<token>
      String generateUrl = String.format(
          "%s?action=GENEWAYBILL&aspid=%s&password=%s&gstin=%s&username=%s&authtoken=%s",
          serverEwbUrl,
          aspId,
          aspPassword,
          credentials.getEwbGstin(),
          ewbUsername,
          authToken
      );

      log.debug("Calling Generate E-Way Bill API: {} for tenant: {}, DocNo: {}",
               serverEwbUrl, tenantId, ewayBillData.getDocNo());

      // Set headers
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      // The request body is the E-Way Bill JSON data (without wrapper)
      HttpEntity<EwayBillData> httpRequest = new HttpEntity<>(ewayBillData, headers);

      // Call API - Get response as String first
      ResponseEntity<String> response = restTemplate.exchange(
          generateUrl,
          HttpMethod.POST,
          httpRequest,
          String.class
      );

      String responseBody = response.getBody();
      if (responseBody == null || responseBody.isEmpty()) {
        throw new RuntimeException("No response from GSP");
      }

      log.debug("GSP Response: {}", responseBody);

      // Parse JSON response
      GspEwbGenerateResponse ewbResponse = objectMapper.readValue(responseBody, GspEwbGenerateResponse.class);

      return ewbResponse;

    } catch (Exception e) {
      log.error("Error generating E-Way Bill for tenant: {}", tenantId, e);
      throw e; // Re-throw to be handled by caller
    }
  }

  /**
   * Cancel E-Way Bill with Session Support
   * 
   * @param tenantId Tenant ID
   * @param ewbNo E-Way Bill number
   * @param cancelReasonCode Cancel reason code
   * @param cancelRemarks Cancel remarks
   * @param sessionToken Session token
   * @return E-Way Bill cancel response
   */
  public GspEwbGenerateResponse cancelEwayBill(Long tenantId, Long ewbNo, Integer cancelReasonCode, 
                                               String cancelRemarks, String sessionToken) {
    try {
      // Get credentials from session
      EwayBillSessionService.SessionData session = authService.getSessionData(sessionToken);
      if (session == null) {
        throw new IllegalArgumentException("Invalid or expired session token");
      }

      // Validate tenant ID matches session
      if (!session.getTenantId().equals(tenantId)) {
        throw new IllegalArgumentException("Session token does not belong to this tenant");
      }

      // Get tenant configuration
      TenantEwayBillCredentials credentials = credentialsRepository.findByTenantId(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("E-Way Bill credentials not found"));

      // Get auth token and username from session
      String authToken = session.getGspAuthToken();
      String ewbUsername = session.getEwbUsername();

      // Get ASP credentials from database and decrypt password
      String aspId = credentials.getAspUserId();
      String aspPassword = encryptionService.decrypt(credentials.getAspPassword());

      // Build API URL with query parameters
      String cancelUrl = String.format(
          "%s?action=CANEWB&aspid=%s&password=%s&gstin=%s&username=%s&authtoken=%s",
          ewbUrl,
          aspId,
          aspPassword,
          credentials.getEwbGstin(),
          ewbUsername,
          authToken
      );

      log.info("Cancelling E-Way Bill: {} for tenant: {}", ewbNo, tenantId);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      // Prepare cancel request
      String cancelRequest = String.format(
          "{\"ewbNo\": %d, \"cancelRsnCode\": %d, \"cancelRmrk\": \"%s\"}",
          ewbNo, cancelReasonCode, cancelRemarks
      );

      HttpEntity<String> request = new HttpEntity<>(cancelRequest, headers);

      ResponseEntity<String> response = restTemplate.exchange(
          cancelUrl,
          HttpMethod.POST,
          request,
          String.class
      );

      log.info("E-Way Bill cancelled successfully: {}", ewbNo);
      return objectMapper.readValue(response.getBody(), GspEwbGenerateResponse.class);

    } catch (Exception e) {
      log.error("Error cancelling E-Way Bill: {}", ewbNo, e);
      throw new RuntimeException("E-Way Bill cancellation failed: " + e.getMessage(), e);
    }
  }

  /**
   * Update Vehicle Number with Session Support
   * 
   * @param tenantId Tenant ID
   * @param ewbNo E-Way Bill number
   * @param newVehicleNo New vehicle number
   * @param fromPlace From place
   * @param fromState From state code
   * @param reasonCode Reason code
   * @param reasonRemarks Reason remarks
   * @param sessionToken Session token
   * @return E-Way Bill update response
   */
  public GspEwbGenerateResponse updateVehicleNumber(Long tenantId, Long ewbNo, String newVehicleNo,
                                                    String fromPlace, Integer fromState,
                                                    String reasonCode, String reasonRemarks, String sessionToken) {
    try {
      // Get credentials from session
      EwayBillSessionService.SessionData session = authService.getSessionData(sessionToken);
      if (session == null) {
        throw new IllegalArgumentException("Invalid or expired session token");
      }

      // Validate tenant ID matches session
      if (!session.getTenantId().equals(tenantId)) {
        throw new IllegalArgumentException("Session token does not belong to this tenant");
      }

      // Get tenant configuration
      TenantEwayBillCredentials credentials = credentialsRepository.findByTenantId(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("E-Way Bill credentials not found"));

      // Get auth token and username from session
      String authToken = session.getGspAuthToken();
      String ewbUsername = session.getEwbUsername();

      // Get ASP credentials from database and decrypt password
      String aspId = credentials.getAspUserId();
      String aspPassword = encryptionService.decrypt(credentials.getAspPassword());

      // Build API URL with query parameters
      String updateUrl = String.format(
          "%s?action=VEHEWB&aspid=%s&password=%s&gstin=%s&username=%s&authtoken=%s",
          ewbUrl,
          aspId,
          aspPassword,
          credentials.getEwbGstin(),
          ewbUsername,
          authToken
      );

      log.info("Updating vehicle for E-Way Bill: {} to vehicle: {}", ewbNo, newVehicleNo);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      // Prepare update request
      String updateRequest = String.format(
          "{\"ewbNo\": %d, \"vehicleNo\": \"%s\", \"fromPlace\": \"%s\", \"fromState\": %d, \"reasonCode\": \"%s\", \"reasonRem\": \"%s\"}",
          ewbNo, newVehicleNo, fromPlace, fromState, reasonCode, reasonRemarks
      );

      HttpEntity<String> request = new HttpEntity<>(updateRequest, headers);

      ResponseEntity<String> response = restTemplate.exchange(
          updateUrl,
          HttpMethod.POST,
          request,
          String.class
      );

      log.info("Vehicle updated successfully for E-Way Bill: {}", ewbNo);
      return objectMapper.readValue(response.getBody(), GspEwbGenerateResponse.class);

    } catch (Exception e) {
      log.error("Error updating vehicle for E-Way Bill: {}", ewbNo, e);
      throw new RuntimeException("Vehicle update failed: " + e.getMessage(), e);
    }
  }

  /**
   * Get E-Way Bill Details with retry logic and DB caching, with Session Support
   * Returns complete E-Way Bill details including items and vehicle history
   * First checks if details are cached in DB, if not fetches from GSP API and caches
   * 
   * @param tenantId Tenant ID
   * @param ewbNo E-Way Bill Number
   * @param forceRefresh If true, bypasses cache and fetches fresh data from GSP API
   * @param sessionToken Session token
   * @return Complete E-Way Bill details
   */
  public GspEwbDetailResponse getEwayBillDetails(Long tenantId, Long ewbNo, boolean forceRefresh, String sessionToken) {
    // Try to get from database cache first (unless force refresh)
    if (!forceRefresh) {
      try {
        Invoice invoice = invoiceRepository.findByEwayBillNumberAndTenantId(String.valueOf(ewbNo), tenantId);
        if (invoice != null && invoice.getEwayBillDetailsJson() != null && !invoice.getEwayBillDetailsJson().isEmpty()) {
          log.info("E-Way Bill details found in DB cache for ewbNo: {}", ewbNo);
          GspEwbDetailResponse cachedDetails = objectMapper.readValue(
              invoice.getEwayBillDetailsJson(), 
              GspEwbDetailResponse.class
          );
          return cachedDetails;
        }
      } catch (Exception e) {
        log.warn("Failed to retrieve E-Way Bill details from DB cache for ewbNo: {}, will fetch from API", ewbNo, e);
      }
    }

    String[] serverUrls = {ewbUrl, backup1EwbUrl, backup2EwbUrl};
    
    GspEwbDetailResponse ewbDetails = failoverHelper.executeWithFailover(
        serverUrls,
        serverUrl -> {
          try {
            return attemptGetEwayBillDetails(tenantId, ewbNo, serverUrl, sessionToken);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        },
        "E-Way Bill details fetch for ewbNo: " + ewbNo
    );
    
    // Cache the response in database
    cacheEwayBillDetails(tenantId, ewbNo, ewbDetails);
    
    return ewbDetails;
  }

  /**
   * Attempt to get E-Way Bill details from a specific server with Session Support
   * 
   * @param tenantId Tenant ID
   * @param ewbNo E-Way Bill number
   * @param serverEwbUrl Server URL
   * @param sessionToken Session token
   * @return E-Way Bill details
   */
  private GspEwbDetailResponse attemptGetEwayBillDetails(Long tenantId, Long ewbNo, String serverEwbUrl, 
                                                         String sessionToken) throws Exception {
    try {
      // Get credentials from session
      EwayBillSessionService.SessionData session = authService.getSessionData(sessionToken);
      if (session == null) {
        throw new IllegalArgumentException("Invalid or expired session token");
      }

      // Validate tenant ID matches session
      if (!session.getTenantId().equals(tenantId)) {
        throw new IllegalArgumentException("Session token does not belong to this tenant");
      }

      // Get tenant configuration
      TenantEwayBillCredentials credentials = credentialsRepository.findByTenantId(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("E-Way Bill credentials not found"));

      // Get auth token and username from session
      String authToken = session.getGspAuthToken();
      String ewbUsername = session.getEwbUsername();

      // Get ASP credentials from database and decrypt password
      String aspId = credentials.getAspUserId();
      String aspPassword = encryptionService.decrypt(credentials.getAspPassword());

      // Build API URL with query parameters
      String detailsUrl = String.format(
          "%s?action=GetEwayBill&aspid=%s&password=%s&gstin=%s&username=%s&authtoken=%s&ewbNo=%d",
          serverEwbUrl,
          aspId,
          aspPassword,
          credentials.getEwbGstin(),
          ewbUsername,
          authToken,
          ewbNo
      );

      log.debug("Calling GetEwayBill API: {} for ewbNo: {}", serverEwbUrl, ewbNo);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<Void> request = new HttpEntity<>(headers);

      ResponseEntity<String> response = restTemplate.exchange(
          detailsUrl,
          HttpMethod.GET,
          request,
          String.class
      );

      String responseBody = response.getBody();
      if (responseBody == null || responseBody.isEmpty()) {
        throw new RuntimeException("No response from GSP GetEwayBill API");
      }

      log.debug("GetEwayBill API Response: {}", responseBody);

      GspEwbDetailResponse ewbDetails = objectMapper.readValue(responseBody, GspEwbDetailResponse.class);
      
      return ewbDetails;

    } catch (Exception e) {
      log.error("Error fetching E-Way Bill details for ewbNo: {}", ewbNo, e);
      throw e; // Re-throw to be handled by caller
    }
  }

  /**
   * Cache E-Way Bill details in database
   */
  private void cacheEwayBillDetails(Long tenantId, Long ewbNo, GspEwbDetailResponse ewbDetails) {
    try {
      String responseBody = objectMapper.writeValueAsString(ewbDetails);
      Invoice invoice = invoiceRepository.findByEwayBillNumberAndTenantId(String.valueOf(ewbNo), tenantId);
      if (invoice != null) {
        invoice.setEwayBillDetailsJson(responseBody);
        invoiceRepository.save(invoice);
        log.info("E-Way Bill details cached in DB for ewbNo: {}", ewbNo);
      } else {
        log.warn("Invoice not found for E-Way Bill number: {}, cannot cache details", ewbNo);
      }
    } catch (Exception e) {
      log.error("Failed to cache E-Way Bill details in DB for ewbNo: {}", ewbNo, e);
      // Don't fail the operation if caching fails
    }
  }

  /**
   * Get E-Way Bill Details - convenience method without force refresh
   * 
   * @param tenantId Tenant ID
   * @param ewbNo E-Way Bill number
   * @param sessionToken Session token
   * @return E-Way Bill details
   */
  public GspEwbDetailResponse getEwayBillDetails(Long tenantId, Long ewbNo, String sessionToken) {
    return getEwayBillDetails(tenantId, ewbNo, false, sessionToken);
  }

  /**
   * Print E-Way Bill via GSP Print API with Session Support
   * First fetches E-Way Bill details using GetEwayBill API, then calls Print API
   * 
   * @param tenantId Tenant ID
   * @param ewbDetails E-Way Bill details
   * @param sessionToken Session token
   * @return PDF bytes
   */
  public byte[] printEwayBill(Long tenantId, GspEwbDetailResponse ewbDetails, String sessionToken) {
    // Check if E-Way Bill is active
    if (!"ACT".equals(ewbDetails.getStatus())) {
      throw new RuntimeException("E-Way Bill is not active. Status: " + ewbDetails.getStatus());
    }

    String[] serverUrls = {printUrl, backup1PrintUrl, backup2PrintUrl};
    
    return failoverHelper.executeWithFailover(
        serverUrls,
        serverUrl -> {
          try {
            return attemptPrintEwayBill(tenantId, ewbDetails, serverUrl, sessionToken);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        },
        "E-Way Bill print for ewbNo: " + ewbDetails.getEwbNo()
    );
  }

  /**
   * Attempt to print E-Way Bill on a specific server with Session Support
   * 
   * @param tenantId Tenant ID
   * @param ewbDetails E-Way Bill details
   * @param serverPrintUrl Server print URL
   * @param sessionToken Session token
   * @return PDF bytes
   */
  private byte[] attemptPrintEwayBill(Long tenantId, GspEwbDetailResponse ewbDetails, String serverPrintUrl, 
                                      String sessionToken) throws Exception {
    try {
      // Get credentials from session (validate session)
      EwayBillSessionService.SessionData session = authService.getSessionData(sessionToken);
      if (session == null) {
        throw new IllegalArgumentException("Invalid or expired session token");
      }

      // Validate tenant ID matches session
      if (!session.getTenantId().equals(tenantId)) {
        throw new IllegalArgumentException("Session token does not belong to this tenant");
      }

      // Get tenant configuration
      TenantEwayBillCredentials credentials = credentialsRepository.findByTenantId(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("E-Way Bill credentials not found"));

      // Get ASP credentials from database and decrypt password
      String aspId = credentials.getAspUserId();
      String aspPassword = encryptionService.decrypt(credentials.getAspPassword());

      // Build Print API URL with query parameters
      String printApiUrl = String.format(
          "%s?aspid=%s&password=%s&gstin=%s",
          serverPrintUrl,
          aspId,
          aspPassword,
          credentials.getEwbGstin()
      );

      log.debug("Calling Print E-Way Bill API: {} for ewbNo: {}", serverPrintUrl, ewbDetails.getEwbNo());

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setAccept(List.of(MediaType.APPLICATION_PDF, MediaType.APPLICATION_JSON));

      // Send the complete E-Way Bill details as the request body
      HttpEntity<GspEwbDetailResponse> request = new HttpEntity<>(ewbDetails, headers);

      ResponseEntity<byte[]> response = restTemplate.exchange(
          printApiUrl,
          HttpMethod.POST,
          request,
          byte[].class
      );

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        return response.getBody();
      } else {
        throw new RuntimeException("Print API returned empty response");
      }

    } catch (Exception e) {
      log.error("Error printing E-Way Bill for ewbNo: {}", ewbDetails.getEwbNo(), e);
      throw e; // Re-throw to be handled by caller
    }
  }

}
