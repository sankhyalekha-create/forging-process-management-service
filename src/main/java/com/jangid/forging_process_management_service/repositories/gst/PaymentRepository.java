package com.jangid.forging_process_management_service.repositories.gst;

import com.jangid.forging_process_management_service.entities.gst.Payment;
import com.jangid.forging_process_management_service.entities.gst.PaymentStatus;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment entity operations.
 */
@Repository
public interface PaymentRepository extends CrudRepository<Payment, Long> {

  /**
   * Find all payments for a specific invoice (excluding deleted).
   */
  List<Payment> findByInvoiceIdAndDeletedFalseOrderByPaymentDateTimeDesc(Long invoiceId);

  /**
   * Find all payments for a specific invoice by status (excluding deleted).
   */
  List<Payment> findByInvoiceIdAndStatusAndDeletedFalse(Long invoiceId, PaymentStatus status);

  /**
   * Find payment by ID and tenant ID (excluding deleted).
   */
  Optional<Payment> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

  /**
   * Find all payments for a tenant within a date range (excluding deleted).
   */
  List<Payment> findByTenantIdAndPaymentDateTimeBetweenAndDeletedFalseOrderByPaymentDateTimeDesc(
      Long tenantId, LocalDateTime fromDate, LocalDateTime toDate);

  /**
   * Find all payments by tenant and status (excluding deleted).
   */
  List<Payment> findByTenantIdAndStatusAndDeletedFalseOrderByPaymentDateTimeDesc(
      Long tenantId, PaymentStatus status);

  /**
   * Count payments for an invoice (excluding deleted).
   */
  long countByInvoiceIdAndDeletedFalse(Long invoiceId);
}

