package com.jangid.forging_process_management_service.entitiesRepresentation.gst;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Representation class for Payment entity.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Payment representation for invoice payment tracking")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Payment ID", example = "123")
  private Long id;

  @JsonProperty(value = "invoiceId")
  @ApiModelProperty(value = "Invoice ID this payment belongs to", example = "456", required = true)
  private Long invoiceId;

  @JsonProperty(value = "invoiceNumber")
  @ApiModelProperty(value = "Invoice number (for display)", example = "INV-2025-001")
  private String invoiceNumber;

  @JsonProperty(value = "amount")
  @ApiModelProperty(value = "Payment amount", example = "50000.00", required = true)
  private BigDecimal amount;

  @JsonProperty(value = "paymentDateTime")
  @ApiModelProperty(value = "Payment date and time (ISO 8601 format)", example = "2025-01-15T14:30:00", required = true)
  private String paymentDateTime;

  @JsonProperty(value = "paymentMethod")
  @ApiModelProperty(value = "Payment method", example = "BANK_TRANSFER", required = true, 
    allowableValues = "CASH, CHEQUE, BANK_TRANSFER, UPI, NEFT_RTGS, DEMAND_DRAFT, CREDIT_CARD, DEBIT_CARD, OTHER")
  private String paymentMethod;

  @JsonProperty(value = "paymentReference")
  @ApiModelProperty(value = "Payment reference number (UTR, Cheque no., Transaction ID)", example = "UTR123456789")
  private String paymentReference;

  @JsonProperty(value = "status")
  @ApiModelProperty(value = "Payment status", example = "RECEIVED", 
    allowableValues = "RECEIVED, PENDING_CLEARANCE, REVERSED, CANCELLED")
  private String status;

  @JsonProperty(value = "recordedBy")
  @ApiModelProperty(value = "Name of person who recorded the payment", example = "John Doe")
  private String recordedBy;

  @JsonProperty(value = "paymentProofPath")
  @ApiModelProperty(value = "Path to payment proof document", example = "/documents/payments/proof_123.pdf")
  private String paymentProofPath;

  @JsonProperty(value = "tdsAmount")
  @ApiModelProperty(value = "TDS (Tax Deducted at Source) amount", example = "1000.00")
  private BigDecimal tdsAmount;

  @JsonProperty(value = "tdsReference")
  @ApiModelProperty(value = "TDS reference/certificate number", example = "TDS-2025-001")
  private String tdsReference;

  @JsonProperty(value = "notes")
  @ApiModelProperty(value = "Additional notes about the payment", example = "Payment received via NEFT. TDS deducted as per section 194C.")
  private String notes;

  @JsonProperty(value = "tenantId")
  @ApiModelProperty(value = "Tenant ID", example = "1")
  private Long tenantId;

  @JsonProperty(value = "createdAt")
  @ApiModelProperty(value = "Payment creation timestamp", example = "2025-01-15T14:30:00")
  private String createdAt;

  @JsonProperty(value = "updatedAt")
  @ApiModelProperty(value = "Payment last update timestamp", example = "2025-01-15T14:30:00")
  private String updatedAt;
}

