package com.jangid.forging_process_management_service.dto.gst;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for challan generation from dispatch batches.
 * Supports generating a single challan for multiple dispatch batches (e.g., same vendor, same order).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChallanGenerationRequest {

  /**
   * List of dispatch batch IDs to include in this challan
   * Multiple batches can be sent together (e.g., to same vendor for job work)
   * Note: Either dispatchBatchIds or vendorDispatchBatchId must be provided, but not both
   */
  private List<Long> dispatchBatchIds;

  /**
   * Vendor dispatch batch ID to include in this challan
   * Note: Either dispatchBatchIds or vendorDispatchBatchId must be provided, but not both
   */
  private Long vendorDispatchBatchId;

  /**
   * Type of challan
   * Values: JOB_WORK, BRANCH_TRANSFER, SAMPLE_DISPATCH, RETURN_GOODS, OTHER
   */
  @NotBlank(message = "Challan type is required")
  private String challanType;

  /**
   * Details for "OTHER" challan type (required when challanType is "OTHER")
   */
  @Size(max = 500, message = "Other challan type details cannot exceed 500 characters")
  private String otherChallanTypeDetails;

  /**
   * Order ID (for traceability when dispatch batches are order-based)
   */
  private Long orderId;

  /**
   * Reason for transportation (mandatory for GST compliance)
   */
  @NotBlank(message = "Transportation reason is required")
  @Size(max = 500, message = "Transportation reason cannot exceed 500 characters")
  private String transportationReason;

  /**
   * Consignee details - either buyer or vendor entity ID
   */
  private Long buyerId;
  private Long vendorId;
  private Long buyerBillingEntityId;
  private Long buyerShippingEntityId;
  private Long vendorBillingEntityId;
  private Long vendorShippingEntityId;

  // Transportation Details
  private String transportationMode; // Default: ROAD
  private String expectedDeliveryDate; // Format: yyyy-MM-dd
  private String transporterName;
  private String transporterId; // GSTIN
  private String vehicleNumber;
  private Integer transportationDistance; // in kilometers

  /**
   * Custom challan number (optional - if not provided, auto-generated)
   */
  private String challanNumber;

  /**
   * Custom challan date and time (optional - if not provided, current date/time used)
   * Format: yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss
   */
  private String challanDateTime;

  /**
   * Manual line items (for custom pricing or non-dispatch-batch challans)
   * If provided, these override dispatch batch items
   */
  private List<ChallanLineItemRequest> manualLineItems;

  /**
   * Item overrides for batch-based challans (allows editing unit price, HSN, GST rates per batch)
   * Maps dispatchBatchId to override values
   */
  private List<ItemOverride> itemOverrides;

  /**
   * Value option for challan - TAXABLE or ESTIMATED
   * TAXABLE: Items valued with taxable value + tax rates (HSN/SAC, CGST, SGST, IGST)
   * ESTIMATED: Items valued with estimated prices only (no tax calculations)
   */
  private String valueOption; // Default: TAXABLE

  /**
   * Flag to indicate if this is a manual challan (without dispatch batches)
   */
  private Boolean isManualChallan;

  // Validation helper
  public boolean hasValidConsignee() {
    return buyerId != null || vendorId != null;
  }

  /**
   * Validates that either dispatchBatchIds or vendorDispatchBatchId is provided, but not both
   * @return true if validation passes, false otherwise
   */
  public boolean isValid() {
    boolean hasDispatchBatches = dispatchBatchIds != null && !dispatchBatchIds.isEmpty();
    boolean hasVendorDispatchBatch = vendorDispatchBatchId != null;
    
    // Either one must be provided, but not both
    return (hasDispatchBatches && !hasVendorDispatchBatch) || (!hasDispatchBatches && hasVendorDispatchBatch);
  }

  /**
   * Item override for batch-based challan generation
   * Allows editing specific fields without replacing entire line item
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ItemOverride {
    
    @NotNull(message = "Dispatch/VendorDispatch batch ID is required")
    private Long batchId;
    
    // Optional overrides - if null, use default values from batch/settings
    private Double unitPrice;
    private String hsnCode;
    private Double cgstRate;
    private Double sgstRate;
    private Double igstRate;
  }

  /**
   * Line item request for manual challan generation
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChallanLineItemRequest {
    
    @NotBlank(message = "Item name is required")
    private String itemName;
    
    private String hsnCode; // Optional - will be derived from WorkType and Invoice Settings if not provided
    
    @NotNull(message = "Quantity is required")
    private String quantity; // String for frontend compatibility
    
    private String unitOfMeasurement; // Optional - defaults to PIECES if not provided
    
    private String ratePerUnit; // Optional for non-taxable challans
    
    @NotNull(message = "Taxable value is required")
    private String taxableValue;
    
    private String cgstRate; // Optional - will be derived from WorkType and Invoice Settings if not provided
    private String sgstRate; // Optional - will be derived from WorkType and Invoice Settings if not provided
    private String igstRate; // Optional - will be derived from WorkType and Invoice Settings if not provided
    
    private String workType; // Optional - JOB_WORK or WITH_MATERIAL (defaults to WITH_MATERIAL if not provided)
    
    private String remarks;
  }
}


