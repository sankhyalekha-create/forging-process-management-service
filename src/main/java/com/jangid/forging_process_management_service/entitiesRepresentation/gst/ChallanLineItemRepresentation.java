package com.jangid.forging_process_management_service.entitiesRepresentation.gst;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallanLineItemRepresentation {

  @JsonProperty("id")
  private Long id;

  @JsonProperty("lineNumber")
  @NotNull(message = "Line number is required")
  private Integer lineNumber;

  @JsonProperty("itemName")
  @NotBlank(message = "Item name is required")
  private String itemName;

  @JsonProperty("hsnCode")
  @NotBlank(message = "HSN code is required")
  private String hsnCode;

  @JsonProperty("workType")
  private String workType;

  @JsonProperty("quantity")
  @NotNull(message = "Quantity is required")
  @DecimalMin(value = "0.001", message = "Quantity must be positive")
  private BigDecimal quantity;

  @JsonProperty("unitOfMeasurement")
  @NotBlank(message = "Unit of measurement is required")
  private String unitOfMeasurement;

  @JsonProperty("ratePerUnit")
  private BigDecimal ratePerUnit;

  @JsonProperty("taxableValue")
  @NotNull(message = "Taxable value is required")
  @DecimalMin(value = "0.0", message = "Taxable value must be non-negative")
  private BigDecimal taxableValue;

  // Tax rates - OPTIONAL (only for BRANCH_TRANSFER)
  @JsonProperty("cgstRate")
  @Builder.Default
  private BigDecimal cgstRate = BigDecimal.ZERO;

  @JsonProperty("sgstRate")
  @Builder.Default
  private BigDecimal sgstRate = BigDecimal.ZERO;

  @JsonProperty("igstRate")
  @Builder.Default
  private BigDecimal igstRate = BigDecimal.ZERO;

  // Tax amounts
  @JsonProperty("cgstAmount")
  @Builder.Default
  private BigDecimal cgstAmount = BigDecimal.ZERO;

  @JsonProperty("sgstAmount")
  @Builder.Default
  private BigDecimal sgstAmount = BigDecimal.ZERO;

  @JsonProperty("igstAmount")
  @Builder.Default
  private BigDecimal igstAmount = BigDecimal.ZERO;

  @JsonProperty("totalValue")
  @NotNull(message = "Total value is required")
  @DecimalMin(value = "0.0", message = "Total value must be non-negative")
  private BigDecimal totalValue;

  @JsonProperty("remarks")
  private String remarks;

  // Traceability
  @JsonProperty("itemWorkflowId")
  private Long itemWorkflowId;

  @JsonProperty("itemWorkflowName")
  private String itemWorkflowName;

  @JsonProperty("processedItemDispatchBatchId")
  private Long processedItemDispatchBatchId;

  /**
   * Heat number(s) associated with this line item.
   * Multiple heat numbers are comma-separated (e.g., "HT-001, HT-002")
   */
  @JsonProperty("heatNumbers")
  private String heatNumbers;
}


