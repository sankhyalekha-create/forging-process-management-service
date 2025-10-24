package com.jangid.forging_process_management_service.dto.gst;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Unified request DTO for all invoice generation scenarios:
 * 1. Single dispatch batch invoice
 * 2. Multiple dispatch batch invoice
 * 3. Manual invoice with custom parameters
 * 
 * Note: Date fields are String type to match frontend JSON format (YYYY-MM-DD).
 * Service layer converts these to LocalDate/LocalDateTime as needed.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceGenerationRequest {

  @NotEmpty(message = "At least one dispatch batch ID is required")
  private List<Long> dispatchBatchIds;

  private String invoiceType;

  /**
   * Custom invoice number (optional - if not provided, auto-generated)
   */
  private String invoiceNumber;

  /**
   * Invoice date and time in format YYYY-MM-DDTHH:mm (optional - defaults to current date and time)
   * Example: "2025-10-24T14:30"
   */
  private String invoiceDate;

  /**
   * Due date for payment in format YYYY-MM-DD (optional - defaults to invoiceDate + 30 days)
   * Example: "2025-11-23"
   */
  private String dueDate;

  /**
   * Payment terms (e.g., "Net 30 days", "Advance Payment")
   */
  private String paymentTerms;

  /**
   * Additional terms and conditions
   */
  private String termsAndConditions;

  /**
   * Transportation mode: "ROAD", "RAIL", "AIR", "SHIP"
   */
  private String transportationMode;

  /**
   * Transportation distance in kilometers
   */
  private Integer transportationDistance;

  /**
   * Transporter name
   */
  private String transporterName;

  /**
   * Transporter GSTIN
   */
  @Size(max = 15)
  private String transporterId;

  /**
   * Vehicle registration number (REQUIRED for invoice generation)
   */
  @NotBlank(message = "Vehicle number is required for invoice generation")
  @Size(max = 20, message = "Vehicle number cannot exceed 20 characters")
  private String vehicleNumber;

  /**
   * Actual dispatch date and time in format YYYY-MM-DDTHH:mm
   * Example: "2025-10-24T14:30"
   */
  private String dispatchDate;

  // ======================================================================
  // PRICING AND GST DETAILS (Required - frontend must provide these)
  // ======================================================================

  /**
   * Unit rate for invoice line item calculation (REQUIRED)
   * Frontend should populate from OrderItem pricing or user input
   */
  @DecimalMin(value = "0.01", message = "Unit rate must be greater than 0")
  private BigDecimal unitRate;

  /**
   * HSN/SAC code for the item (REQUIRED)
   * Frontend should populate from Item or user input
   */
  @Size(max = 10)
  private String hsnSacCode;

  /**
   * CGST rate percentage for intra-state transactions (REQUIRED for intra-state)
   * Frontend should populate from tenant GST settings or user input
   */
  @DecimalMin(value = "0.0", message = "CGST rate cannot be negative")
  private BigDecimal cgstRate;

  /**
   * SGST rate percentage for intra-state transactions (REQUIRED for intra-state)
   * Frontend should populate from tenant GST settings or user input
   */
  @DecimalMin(value = "0.0", message = "SGST rate cannot be negative")
  private BigDecimal sgstRate;

  /**
   * IGST rate percentage for inter-state transactions (REQUIRED for inter-state)
   * Frontend should populate from tenant GST settings or user input
   */
  @DecimalMin(value = "0.0", message = "IGST rate cannot be negative")
  private BigDecimal igstRate;

  /**
   * Discount percentage to apply on line items
   */
  @DecimalMin(value = "0.0")
  private BigDecimal discountPercentage;


  /**
   * Override recipient buyer entity ID (if different from dispatch batch)
   */
  private Long recipientBuyerEntityId;

  /**
   * Override recipient vendor entity ID (for vendor payments)
   */
  private Long recipientVendorEntityId;


  /**
   * Internal notes (not printed on invoice)
   */
  private String internalNotes;

  /**
   * Customer PO number reference
   */
  private String customerPoNumber;

  /**
   * Customer PO date in format YYYY-MM-DD
   * Example: "2025-10-19"
   */
  private String customerPoDate;

  /**
   * Check if this is a single dispatch batch invoice
   */
  public boolean isSingleDispatchBatch() {
    return dispatchBatchIds != null && dispatchBatchIds.size() == 1;
  }

  /**
   * Get effective total GST rate based on inter-state flag
   * Returns IGST for inter-state, or (CGST + SGST) for intra-state
   */
  public BigDecimal getTotalGstRate(boolean isInterState) {
    if (isInterState) {
      if (igstRate == null) {
        throw new IllegalStateException("IGST rate is required for inter-state invoice");
      }
      return igstRate;
    } else {
      if (cgstRate == null || sgstRate == null) {
        throw new IllegalStateException("CGST and SGST rates are required for intra-state invoice");
      }
      return cgstRate.add(sgstRate);
    }
  }

  /**
   * Validate that all required pricing and GST fields are provided
   */
  public void validateRequiredFields(boolean isInterState) {
    if (unitRate == null || unitRate.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Unit rate is required and must be greater than 0");
    }
    
    if (hsnSacCode == null || hsnSacCode.trim().isEmpty()) {
      throw new IllegalArgumentException("HSN/SAC code is required");
    }
    
    if (isInterState) {
      if (igstRate == null) {
        throw new IllegalArgumentException("IGST rate is required for inter-state invoice");
      }
    } else {
      if (cgstRate == null || sgstRate == null) {
        throw new IllegalArgumentException("CGST and SGST rates are required for intra-state invoice");
      }
    }
  }
}

