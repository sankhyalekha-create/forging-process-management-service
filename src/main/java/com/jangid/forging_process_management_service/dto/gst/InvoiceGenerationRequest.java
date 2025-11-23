package com.jangid.forging_process_management_service.dto.gst;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

  /**
   * Flag indicating if this is a manual invoice (not associated with dispatch batches)
   */
  private Boolean isManualInvoice;

  private List<Long> dispatchBatchIds;
  
  /**
   * Dispatch batch line items with detailed information (optional - for enhanced payload)
   * When provided, these details are used directly instead of fetching from ItemWorkflow
   */
  private List<DispatchBatchLineItem> dispatchBatchLineItems;

  private String invoiceType;

  /**
   * Work type for non-order-based dispatch batches (optional)
   * Values: "WITH_MATERIAL" or "JOB_WORK_ONLY"
   * Used to determine appropriate HSN/SAC codes and GST rates when OrderItemWorkflow is not available
   */
  private String workType;

  /**
   * Custom invoice number (optional - if not provided, auto-generated)
   */
  private String invoiceNumber;

  /**
   * Invoice date and time in format YYYY-MM-DDTHH:mm (optional - defaults to current date and time)
   * Example: "2025-10-24T14:30"
   */
  @NotBlank
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
   * Transport identifier - stores vehicle/transport number based on transportation mode (REQUIRED for E-Way Bill):
   * - ROAD: Vehicle registration number (e.g., MH12AB1234)
   * - RAIL: Railway Receipt Number or Train/Wagon Number
   * - AIR: Airway Bill Number or Flight Number
   * - SHIP: Bill of Lading Number or Ship Name
   * 
   * Note: Despite the field name "vehicleNumber", this is a generic transport identifier
   * that adapts based on the transportationMode field.
   */
  @Size(max = 20, message = "Transport identifier cannot exceed 20 characters")
  private String vehicleNumber;

  /**
   * transportDocumentNumber - Transport document number for ROAD transportation
   * Used in E-Way Bill generation as transDocNo
   */
  @Size(max = 50, message = "transportDocumentNumber cannot exceed 50 characters")
  private String transportDocumentNumber;

  /**
   * transportDocumentDate Date in format YYYY-MM-DD
   * Example: "2025-10-24"
   * Used in E-Way Bill generation as transDocDate
   */
  private String transportDocumentDate;

  /**
   * Additional remarks or special notes
   */
  @Size(max = 500, message = "Remarks cannot exceed 500 characters")
  private String remarks;

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

  // ======================================================================
  // CONSIGNEE DETAILS (Buyer/Vendor and their entities)
  // ======================================================================

  /**
   * Consignee details - Main party references
   */
  private Long buyerId;
  private Long vendorId;

  /**
   * Billing and Shipping Entity References for Buyer
   */
  private Long buyerBillingEntityId;
  private Long buyerShippingEntityId;

  /**
   * Billing and Shipping Entity References for Vendor
   */
  private Long vendorBillingEntityId;
  private Long vendorShippingEntityId;

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
   * Manual invoice line items (only for manual invoices)
   */
  private List<ManualInvoiceLineItem> manualLineItems;

  /**
   * Manual Invoice Line Item DTO
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ManualInvoiceLineItem {
    @NotBlank(message = "Item name is required")
    private String itemName;
    
    @NotBlank(message = "Work type is required")
    private String workType; // "WITH_MATERIAL" or "JOB_WORK_ONLY"
    
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;
    
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;
    
    @NotBlank(message = "HSN/SAC code is required")
    @Size(max = 10)
    private String hsnCode;
    
    @DecimalMin(value = "0.0", message = "CGST rate cannot be negative")
    private BigDecimal cgstRate;
    
    @DecimalMin(value = "0.0", message = "SGST rate cannot be negative")
    private BigDecimal sgstRate;
    
    @DecimalMin(value = "0.0", message = "IGST rate cannot be negative")
    private BigDecimal igstRate;
  }

  /**
   * Dispatch Batch Line Item DTO with enhanced details
   * Contains Finished Good (Item) and Raw Material (RM Product) traceability information
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DispatchBatchLineItem {
    @NotNull(message = "Dispatch batch ID is required")
    private Long dispatchBatchId;
    
    // Finished Good (Item) details
    private String finishedGoodName;
    private String finishedGoodCode;
    
    // Raw Material (Product) details - comma-separated for multiple RMs
    private String rmProductNames;
    private String rmProductCodes;
    private String rmInvoiceNumbers;
    private String rmHeatNumbers;
    
    // Heat traceability numbers - comma-separated for multiple heats
    private String heatTracebilityNumbers;
  }

  /**
   * Check if this is a manual invoice
   */
  public boolean isManualInvoice() {
    return Boolean.TRUE.equals(isManualInvoice);
  }

  /**
   * Check if this is a single dispatch batch invoice
   */
  public boolean isSingleDispatchBatch() {
    return dispatchBatchIds != null && dispatchBatchIds.size() == 1;
  }

  /**
   * Check if valid consignee details are provided
   */
  public boolean hasValidConsignee() {
    return (buyerId != null && buyerBillingEntityId != null) || 
           (vendorId != null && vendorBillingEntityId != null);
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

