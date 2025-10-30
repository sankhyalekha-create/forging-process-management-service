package com.jangid.forging_process_management_service.assemblers.gst;

import com.jangid.forging_process_management_service.entities.gst.Payment;
import com.jangid.forging_process_management_service.entities.gst.PaymentMethod;
import com.jangid.forging_process_management_service.entities.gst.PaymentStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.gst.PaymentRepresentation;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Assembler to convert between Payment entity and PaymentRepresentation DTO.
 */
@Slf4j
@Component
public class PaymentAssembler {

  /**
   * Convert Payment entity to PaymentRepresentation.
   */
  public PaymentRepresentation dissemble(Payment payment) {
    if (payment == null) {
      return null;
    }

    return PaymentRepresentation.builder()
        .id(payment.getId())
        .invoiceId(payment.getInvoice() != null ? payment.getInvoice().getId() : null)
        .invoiceNumber(payment.getInvoice() != null ? payment.getInvoice().getInvoiceNumber() : null)
        .amount(payment.getAmount())
        .paymentDateTime(payment.getPaymentDateTime() != null ? payment.getPaymentDateTime().toString() : null)
        .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
        .paymentReference(payment.getPaymentReference())
        .status(payment.getStatus() != null ? payment.getStatus().name() : PaymentStatus.RECEIVED.name())
        .recordedBy(payment.getRecordedBy())
        .paymentProofPath(payment.getPaymentProofPath())
        .tdsAmount(payment.getTdsAmount())
        .tdsReference(payment.getTdsReference())
        .notes(payment.getNotes())
        .tenantId(payment.getTenant() != null ? payment.getTenant().getId() : null)
        .createdAt(payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null)
        .updatedAt(payment.getUpdatedAt() != null ? payment.getUpdatedAt().toString() : null)
        .build();
  }

  /**
   * Convert PaymentRepresentation to Payment entity.
   * Note: Invoice and Tenant relationships must be set separately.
   */
  public Payment assemble(PaymentRepresentation representation) {
    if (representation == null) {
      return null;
    }

    return Payment.builder()
        .id(representation.getId())
        .amount(representation.getAmount())
        .paymentDateTime(representation.getPaymentDateTime() != null
            ? ConvertorUtils.convertStringToLocalDateTime(representation.getPaymentDateTime())
            : LocalDateTime.now())
        .paymentMethod(representation.getPaymentMethod() != null 
            ? PaymentMethod.valueOf(representation.getPaymentMethod())
            : null)
        .paymentReference(representation.getPaymentReference())
        .status(representation.getStatus() != null 
            ? PaymentStatus.valueOf(representation.getStatus())
            : PaymentStatus.RECEIVED)
        .recordedBy(representation.getRecordedBy())
        .paymentProofPath(representation.getPaymentProofPath())
        .tdsAmount(representation.getTdsAmount())
        .tdsReference(representation.getTdsReference())
        .notes(representation.getNotes())
        .build();
  }
}

