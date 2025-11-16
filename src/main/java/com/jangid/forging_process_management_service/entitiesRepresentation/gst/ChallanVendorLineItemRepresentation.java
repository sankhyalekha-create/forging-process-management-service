package com.jangid.forging_process_management_service.entitiesRepresentation.gst;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallanVendorLineItemRepresentation {

  private Long id;
  private Long deliveryChallanId;
  private Long vendorDispatchBatchId;
  private Integer lineNumber;
  private String itemName;
  private String hsnCode;
  private String workType;
  private BigDecimal quantity;
  private String unitOfMeasurement;
  private BigDecimal ratePerUnit;
  private BigDecimal taxableValue;
  private BigDecimal cgstRate;
  private BigDecimal sgstRate;
  private BigDecimal igstRate;
  private BigDecimal cgstAmount;
  private BigDecimal sgstAmount;
  private BigDecimal igstAmount;
  private BigDecimal totalValue;
  private String remarks;
  private Long itemWorkflowId;
  private String itemWorkflowName;
  private Long processedItemVendorDispatchBatchId;
  private String heatNumbers;
}