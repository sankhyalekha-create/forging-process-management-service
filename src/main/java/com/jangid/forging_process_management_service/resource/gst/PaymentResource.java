package com.jangid.forging_process_management_service.resource.gst;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entitiesRepresentation.gst.PaymentRepresentation;
import com.jangid.forging_process_management_service.service.gst.PaymentService;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

/**
 * REST API Resource for payment management operations.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@Api(value = "Payment Management", tags = {"Payment Management"})
public class PaymentResource {

  @Autowired
  private PaymentService paymentService;

  /**
   * Record a new payment for an invoice.
   */
  @PostMapping("/payments/invoices/{invoiceId}")
  @ApiOperation(value = "Record a payment for an invoice", 
    notes = "Records a new payment against an invoice. Supports partial payments and automatically updates invoice status.")
  public ResponseEntity<?> recordPayment(
      @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId,
      @ApiParam(value = "Payment details", required = true) @Valid @RequestBody PaymentRepresentation paymentRequest) {
    
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("REST: Record payment - Tenant: {}, Invoice: {}, Amount: ₹{}",
          tenantId, invoiceId, paymentRequest.getAmount());

      PaymentRepresentation payment = paymentService.recordPayment(tenantId, invoiceId, paymentRequest);
      
      return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "recordPayment");
    }
  }

  /**
   * Get all payments for an invoice.
   */
  @GetMapping("/payments/invoices/{invoiceId}")
  @ApiOperation(value = "Get all payments for an invoice",
    notes = "Retrieves payment history for a specific invoice ordered by payment date (newest first).")
  public ResponseEntity<?> getPaymentsByInvoice(
      @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId) {
    
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.debug("REST: Get payments for invoice - Tenant: {}, Invoice: {}", tenantId, invoiceId);

      List<PaymentRepresentation> payments = paymentService.getPaymentsByInvoice(tenantId, invoiceId);
      
      return ResponseEntity.ok(payments);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getPaymentsByInvoice");
    }
  }

  /**
   * Get a specific payment by ID.
   */
  @GetMapping("/payments/{paymentId}")
  @ApiOperation(value = "Get payment by ID",
    notes = "Retrieves details of a specific payment.")
  public ResponseEntity<?> getPaymentById(
      @ApiParam(value = "Payment ID", required = true) @PathVariable Long paymentId) {
    
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.debug("REST: Get payment - Tenant: {}, Payment ID: {}", tenantId, paymentId);

      PaymentRepresentation payment = paymentService.getPaymentById(tenantId, paymentId);
      
      return ResponseEntity.ok(payment);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getPaymentById");
    }
  }

  /**
   * Reverse a payment (mark as REVERSED).
   */
  @PutMapping("/payments/{paymentId}/reverse")
  @ApiOperation(value = "Reverse a payment",
    notes = "Marks a payment as REVERSED. This will update the invoice status and total paid amount accordingly.")
  public ResponseEntity<?> reversePayment(
      @ApiParam(value = "Payment ID", required = true) @PathVariable Long paymentId,
      @ApiParam(value = "Reason for reversal", required = true) @RequestParam String reason) {
    
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("REST: Reverse payment - Tenant: {}, Payment ID: {}, Reason: {}", 
          tenantId, paymentId, reason);

      PaymentRepresentation payment = paymentService.reversePayment(tenantId, paymentId, reason);
      
      return ResponseEntity.ok(payment);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "reversePayment");
    }
  }

  /**
   * Delete a payment (soft delete).
   */
  @DeleteMapping("/payments/{paymentId}")
  @ApiOperation(value = "Delete a payment",
    notes = "Soft deletes a payment. Only REVERSED or CANCELLED payments can be deleted.")
  public ResponseEntity<?> deletePayment(
      @ApiParam(value = "Payment ID", required = true) @PathVariable Long paymentId) {
    
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("REST: Delete payment - Tenant: {}, Payment ID: {}", tenantId, paymentId);

      paymentService.deletePayment(tenantId, paymentId);
      
      return ResponseEntity.noContent().build();
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "deletePayment");
    }
  }
}

