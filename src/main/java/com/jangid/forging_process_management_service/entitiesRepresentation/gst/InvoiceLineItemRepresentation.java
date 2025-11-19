package com.jangid.forging_process_management_service.entitiesRepresentation.gst;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineItemRepresentation {

  @JsonProperty("id")
  private Long id;

  @JsonProperty("invoiceId")
  private Long invoiceId;

  @NotNull(message = "Line number is required")
  @JsonProperty("lineNumber")
  private Integer lineNumber;

  @NotNull(message = "Item name is required")
  @Size(max = 500, message = "Item name cannot exceed 500 characters")
  @JsonProperty("itemName")
  private String itemName;

  @JsonProperty("workType")
  private String workType;

  // Finished Good Details
  @JsonProperty("finishedGoodName")
  private String finishedGoodName;

  @JsonProperty("finishedGoodCode")
  private String finishedGoodCode;

  // Raw Material Product Details
  @JsonProperty("rmProductNames")
  private String rmProductNames;

  @JsonProperty("rmProductCodes")
  private String rmProductCodes;

  @JsonProperty("rmInvoiceNumbers")
  private String rmInvoiceNumbers;

  @JsonProperty("rmHeatNumbers")
  private String rmHeatNumbers;

  @JsonProperty("heatTracebilityNumbers")
  private String heatTracebilityNumbers;

  @JsonProperty("hsnCode")
  private String hsnCode;

  @NotNull(message = "Quantity is required")
  @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
  @JsonProperty("quantity")
  private BigDecimal quantity;

  @JsonProperty("unitOfMeasurement")
  @Builder.Default
  private String unitOfMeasurement = "PCS";

  @NotNull(message = "Unit price is required")
  @DecimalMin(value = "0.00", message = "Unit price must be non-negative")
  @JsonProperty("unitPrice")
  private BigDecimal unitPrice;

  @NotNull(message = "Taxable value is required")
  @DecimalMin(value = "0.00", message = "Taxable value must be non-negative")
  @JsonProperty("taxableValue")
  private BigDecimal taxableValue;

  // GST Details
  @JsonProperty("cgstRate")
  @Builder.Default
  private BigDecimal cgstRate = BigDecimal.ZERO;

  @JsonProperty("cgstAmount")
  @Builder.Default
  private BigDecimal cgstAmount = BigDecimal.ZERO;

  @JsonProperty("sgstRate")
  @Builder.Default
  private BigDecimal sgstRate = BigDecimal.ZERO;

  @JsonProperty("sgstAmount")
  @Builder.Default
  private BigDecimal sgstAmount = BigDecimal.ZERO;

  @JsonProperty("igstRate")
  @Builder.Default
  private BigDecimal igstRate = BigDecimal.ZERO;

  @JsonProperty("igstAmount")
  @Builder.Default
  private BigDecimal igstAmount = BigDecimal.ZERO;

  // Discount
  @JsonProperty("discountPercentage")
  @Builder.Default
  private BigDecimal discountPercentage = BigDecimal.ZERO;

  @JsonProperty("discountAmount")
  @Builder.Default
  private BigDecimal discountAmount = BigDecimal.ZERO;

  // Line Total
  @NotNull(message = "Line total is required")
  @DecimalMin(value = "0.00", message = "Line total must be non-negative")
  @JsonProperty("lineTotal")
  private BigDecimal lineTotal;

  // Traceability
  @JsonProperty("itemWorkflowId")
  private Long itemWorkflowId;

  @JsonProperty("processedItemDispatchBatchId")
  private Long processedItemDispatchBatchId;

  /**
   * Heat number(s) associated with this line item.
   * Multiple heat numbers are comma-separated (e.g., "HT-001, HT-002")
   */
  @JsonProperty("heatNumbers")
  private String heatNumbers;

  // Tenant
  @JsonProperty("tenantId")
  private Long tenantId;

  // Audit Fields
  @JsonProperty("createdAt")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;

  @JsonProperty("updatedAt")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime updatedAt;

  // Computed Properties
  @JsonProperty("totalGstAmount")
  public BigDecimal getTotalGstAmount() {
    if (cgstAmount == null && sgstAmount == null && igstAmount == null) {
      return BigDecimal.ZERO;
    }
    return (cgstAmount != null ? cgstAmount : BigDecimal.ZERO)
      .add(sgstAmount != null ? sgstAmount : BigDecimal.ZERO)
      .add(igstAmount != null ? igstAmount : BigDecimal.ZERO);
  }

  @JsonProperty("isInterState")
  public boolean isInterState() {
    return igstRate != null && igstRate.compareTo(BigDecimal.ZERO) > 0;
  }

  @JsonProperty("effectiveRate")
  public BigDecimal getEffectiveRate() {
    // Effective unit price after discount
    if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal valueAfterDiscount = (taxableValue != null ? taxableValue : BigDecimal.ZERO)
      .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
    return valueAfterDiscount.divide(quantity, 2, BigDecimal.ROUND_HALF_UP);
  }
}

