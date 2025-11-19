package com.jangid.forging_process_management_service.resource.gst;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.dto.gst.EwayBillJsonFormat;
import com.jangid.forging_process_management_service.dto.gst.EwayBillUpdateRequest;
import com.jangid.forging_process_management_service.entities.gst.DeliveryChallan;
import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.repositories.gst.DeliveryChallanRepository;
import com.jangid.forging_process_management_service.repositories.gst.InvoiceRepository;
import com.jangid.forging_process_management_service.service.gst.EwayBillExportService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * REST API for E-Way Bill offline JSON generation and management
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

  /**
   * Download E-Way Bill JSON for Invoice
   * User downloads this JSON file and uploads it manually to ewaybillgst.gov.in
   */
  @GetMapping("/invoices/{invoiceId}/eway-bill/json")
  @ApiOperation(value = "Download E-Way Bill JSON for manual upload to GST portal",
                notes = "Generates NIC-compliant JSON file that can be uploaded to ewaybillgst.gov.in")
  public ResponseEntity<Resource> downloadInvoiceEwayBillJson(
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

    } catch (IllegalArgumentException e) {
      log.error("Validation error while generating E-Way Bill JSON: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("Error generating E-Way Bill JSON for invoice: {}", invoiceId, e);
      throw new RuntimeException("Failed to generate E-Way Bill JSON: " + e.getMessage(), e);
    }
  }

  /**
   * Download E-Way Bill JSON for Delivery Challan
   */
  @GetMapping("/challans/{challanId}/eway-bill/json")
  @ApiOperation(value = "Download E-Way Bill JSON for challan",
                notes = "Generates NIC-compliant JSON file for delivery challan")
  public ResponseEntity<Resource> downloadChallanEwayBillJson(
      @ApiParam(value = "Challan ID", required = true) @PathVariable Long challanId) {
    Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

    log.info("Generating E-Way Bill JSON for challan: {}, tenant: {}", challanId, tenantId);

    // TODO: Implement challan mapping in Phase 2
    throw new UnsupportedOperationException("Challan E-Way Bill generation will be available in Phase 2");
  }

  /**
   * Update E-Way Bill details after manual generation on GST portal
   * User enters the E-Way Bill number and validity details from GST portal
   */
  @PatchMapping("/invoices/{invoiceId}/eway-bill")
  @ApiOperation(value = "Update E-Way Bill details after manual generation",
                notes = "After uploading JSON to GST portal and receiving E-Way Bill number, update invoice with the details")
  public ResponseEntity<?> updateInvoiceEwayBill(
      @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId,
      @ApiParam(value = "E-Way Bill details", required = true) @Valid @RequestBody EwayBillUpdateRequest request) {

    Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
    log.info("Updating E-Way Bill details for invoice: {}, EWB No: {}", invoiceId, request.getEwayBillNumber());

    try {
      Invoice invoice = invoiceRepository.findById(invoiceId)
          .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));

      if (invoice.getTenant().getId() != tenantId) {
        throw new IllegalArgumentException("Invoice does not belong to the specified tenant");
      }

      // Update E-Way Bill details
      invoice.setEwayBillNumber(request.getEwayBillNumber());
      invoice.setEwayBillDate(request.getEwayBillDate());
      invoice.setEwayBillValidUntil(request.getEwayBillValidUntil());

      invoiceRepository.save(invoice);

      log.info("Successfully updated E-Way Bill {} for invoice {}", request.getEwayBillNumber(), invoiceId);

      return ResponseEntity.ok(Map.of(
          "message", "E-Way Bill details updated successfully",
          "invoiceId", invoiceId,
          "ewayBillNumber", request.getEwayBillNumber(),
          "validUntil", request.getEwayBillValidUntil()
      ));

    } catch (IllegalArgumentException e) {
      log.error("Validation error: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Error updating E-Way Bill for invoice: {}", invoiceId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to update E-Way Bill details: " + e.getMessage()));
    }
  }

  /**
   * Update E-Way Bill details for Challan
   */
  @PatchMapping("/challans/{challanId}/eway-bill")
  @ApiOperation(value = "Update E-Way Bill details for challan")
  public ResponseEntity<?> updateChallanEwayBill(
      @ApiParam(value = "Challan ID", required = true) @PathVariable Long challanId,
      @ApiParam(value = "E-Way Bill details", required = true) @Valid @RequestBody EwayBillUpdateRequest request) {

    Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
    log.info("Updating E-Way Bill details for challan: {}, EWB No: {}", challanId, request.getEwayBillNumber());

    try {
      DeliveryChallan challan = challanRepository.findById(challanId)
          .orElseThrow(() -> new IllegalArgumentException("Challan not found with id: " + challanId));

      if (challan.getTenant().getId() != tenantId) {
        throw new IllegalArgumentException("Challan does not belong to the specified tenant");
      }

      // Update E-Way Bill details
      challan.setEwayBillNumber(request.getEwayBillNumber());
      challan.setEwayBillDate(request.getEwayBillDate());
      challan.setEwayBillValidUntil(request.getEwayBillValidUntil());

      challanRepository.save(challan);

      log.info("Successfully updated E-Way Bill {} for challan {}", request.getEwayBillNumber(), challanId);

      return ResponseEntity.ok(Map.of(
          "message", "E-Way Bill details updated successfully",
          "challanId", challanId,
          "ewayBillNumber", request.getEwayBillNumber(),
          "validUntil", request.getEwayBillValidUntil()
      ));

    } catch (IllegalArgumentException e) {
      log.error("Validation error: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Error updating E-Way Bill for challan: {}", challanId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to update E-Way Bill details: " + e.getMessage()));
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
          "validUntil", invoice.getEwayBillValidUntil() != null ? invoice.getEwayBillValidUntil() : ""
      ));

    } catch (Exception e) {
      log.error("Error fetching E-Way Bill details for invoice: {}", invoiceId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }
}
