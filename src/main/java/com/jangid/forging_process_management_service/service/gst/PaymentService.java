package com.jangid.forging_process_management_service.service.gst;

import com.jangid.forging_process_management_service.assemblers.gst.PaymentAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.InvoiceStatus;
import com.jangid.forging_process_management_service.entities.gst.Payment;
import com.jangid.forging_process_management_service.entities.gst.PaymentStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.gst.PaymentRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.gst.InvoiceRepository;
import com.jangid.forging_process_management_service.repositories.gst.PaymentRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for managing invoice payments.
 * Handles payment recording, validation, and invoice status updates.
 */
@Slf4j
@Service
public class PaymentService {

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private InvoiceRepository invoiceRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private PaymentAssembler paymentAssembler;

  /**
   * Record a new payment against an invoice.
   */
  @Transactional
  public PaymentRepresentation recordPayment(Long tenantId, Long invoiceId, PaymentRepresentation paymentRequest) {
    log.info("Recording payment for invoice ID: {}, tenant ID: {}", invoiceId, tenantId);

    // Validate tenant
    Tenant tenant = tenantService.getTenantById(tenantId);

    // Fetch and validate invoice
    Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + invoiceId));

    // Validate invoice status - can only receive payments for GENERATED, SENT, or PARTIALLY_PAID invoices
    if (invoice.getStatus() != InvoiceStatus.GENERATED && 
        invoice.getStatus() != InvoiceStatus.SENT && 
        invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID) {
      throw new IllegalStateException(
          "Cannot record payment for invoice in " + invoice.getStatus() + " status. " +
          "Payments can only be recorded for GENERATED, SENT, or PARTIALLY_PAID invoices.");
    }

    // Validate payment amount
    if (paymentRequest.getAmount() == null || paymentRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Payment amount must be greater than zero");
    }

    // Check if payment amount exceeds remaining amount
    BigDecimal remainingAmount = invoice.getRemainingAmount();
    if (paymentRequest.getAmount().compareTo(remainingAmount) > 0) {
      throw new IllegalArgumentException(
          String.format("Payment amount (₹%.2f) cannot exceed remaining invoice amount (₹%.2f)",
              paymentRequest.getAmount(), remainingAmount));
    }

    // Create payment entity
    Payment payment = paymentAssembler.assemble(paymentRequest);
    payment.setInvoice(invoice);
    payment.setTenant(tenant);

    // Add payment to invoice (this will update totals and status automatically)
    invoice.addPayment(payment);

    // Save payment
    payment = paymentRepository.save(payment);

    // Save invoice with updated status and total paid amount
    invoiceRepository.save(invoice);

    log.info("Payment recorded successfully. Payment ID: {}, Invoice: {}, Amount: ₹{}, New Status: {}",
        payment.getId(), invoice.getInvoiceNumber(), payment.getAmount(), invoice.getStatus());

    return paymentAssembler.dissemble(payment);
  }

  /**
   * Get all payments for an invoice.
   */
  @Transactional(readOnly = true)
  public List<PaymentRepresentation> getPaymentsByInvoice(Long tenantId, Long invoiceId) {
    log.debug("Fetching payments for invoice ID: {}, tenant ID: {}", invoiceId, tenantId);

    // Validate invoice exists
    Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + invoiceId));

    List<Payment> payments = paymentRepository.findByInvoiceIdAndDeletedFalseOrderByPaymentDateTimeDesc(invoiceId);

    return payments.stream()
        .map(paymentAssembler::dissemble)
        .collect(Collectors.toList());
  }

  /**
   * Get a single payment by ID.
   */
  @Transactional(readOnly = true)
  public PaymentRepresentation getPaymentById(Long tenantId, Long paymentId) {
    log.debug("Fetching payment ID: {}, tenant ID: {}", paymentId, tenantId);

    Payment payment = paymentRepository.findByIdAndTenantIdAndDeletedFalse(paymentId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

    return paymentAssembler.dissemble(payment);
  }

  /**
   * Reverse a payment (mark as REVERSED).
   * This will update the invoice status accordingly.
   */
  @Transactional
  public PaymentRepresentation reversePayment(Long tenantId, Long paymentId, String reason) {
    log.info("Reversing payment ID: {}, tenant ID: {}, reason: {}", paymentId, tenantId, reason);

    Payment payment = paymentRepository.findByIdAndTenantIdAndDeletedFalse(paymentId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

    // Validate payment status
    if (payment.getStatus() != PaymentStatus.RECEIVED && 
        payment.getStatus() != PaymentStatus.PENDING_CLEARANCE) {
      throw new IllegalStateException(
          "Cannot reverse payment in " + payment.getStatus() + " status");
    }

    // Update payment status
    payment.setStatus(PaymentStatus.REVERSED);
    payment.setNotes(payment.getNotes() != null 
        ? payment.getNotes() + "\nREVERSED: " + reason 
        : "REVERSED: " + reason);

    payment = paymentRepository.save(payment);

    // Update invoice totals and status
    Invoice invoice = payment.getInvoice();
    invoice.updateTotalPaidAmount();
    invoiceRepository.save(invoice);

    log.info("Payment reversed successfully. Payment ID: {}, Invoice: {}, New Invoice Status: {}",
        payment.getId(), invoice.getInvoiceNumber(), invoice.getStatus());

    return paymentAssembler.dissemble(payment);
  }

  /**
   * Soft delete a payment.
   */
  @Transactional
  public void deletePayment(Long tenantId, Long paymentId) {
    log.info("Deleting payment ID: {}, tenant ID: {}", paymentId, tenantId);

    Payment payment = paymentRepository.findByIdAndTenantIdAndDeletedFalse(paymentId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

    // Only allow deletion of REVERSED or CANCELLED payments
    if (payment.getStatus() == PaymentStatus.RECEIVED || 
        payment.getStatus() == PaymentStatus.PENDING_CLEARANCE) {
      throw new IllegalStateException(
          "Cannot delete a " + payment.getStatus() + " payment. Reverse it first.");
    }

    // Soft delete
    payment.setDeleted(true);
    payment.setDeletedAt(LocalDateTime.now());
    paymentRepository.save(payment);

    log.info("Payment deleted successfully. Payment ID: {}", paymentId);
  }

  /**
   * Get payment count for an invoice.
   */
  @Transactional(readOnly = true)
  public long getPaymentCount(Long invoiceId) {
    return paymentRepository.countByInvoiceIdAndDeletedFalse(invoiceId);
  }
}

