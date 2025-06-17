package com.jangid.forging_process_management_service.entitiesRepresentation.dispatch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerRepresentation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Dispatch Batch representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispatchBatchRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the Dispatch Batch", example = "123")
  private Long id;

  @JsonProperty(value = "dispatchBatchNumber")
  @ApiModelProperty(value = "Unique number of the Dispatch Batch", example = "DISP-001")
  private String dispatchBatchNumber;

  @JsonProperty(value = "dispatchBatchStatus")
  @ApiModelProperty(value = "Status of the Dispatch Batch", example = "DISPATCHED")
  private String dispatchBatchStatus;

  @JsonProperty(value = "dispatchCreatedAt")
  @ApiModelProperty(value = "Timestamp when the batch is created for dispatch", example = "2025-01-01T10:00:00")
  private String dispatchCreatedAt;

  @JsonProperty(value = "dispatchReadyAt")
  @ApiModelProperty(value = "Timestamp when the batch is ready for dispatch", example = "2025-01-01T10:00:00")
  private String dispatchReadyAt;

  @JsonProperty(value = "dispatchedAt")
  @ApiModelProperty(value = "Timestamp when the batch was dispatched", example = "2025-01-01T12:00:00")
  private String dispatchedAt;

  @JsonProperty(value = "invoiceNumber")
  @ApiModelProperty(value = "Invoice number for the dispatched batch", example = "INV-2025-001")
  private String invoiceNumber;

  @JsonProperty(value = "invoiceDateTime")
  @ApiModelProperty(value = "Timestamp of the invoice", example = "2025-01-01T12:30:00")
  private String invoiceDateTime;

  @JsonProperty(value = "purchaseOrderNumber")
  @ApiModelProperty(value = "Purchase order number for the dispatched batch", example = "PO-2025-001")
  private String purchaseOrderNumber;

  @JsonProperty(value = "purchaseOrderDateTime")
  @ApiModelProperty(value = "Timestamp of the purchase order", example = "2025-01-01T09:00:00")
  private String purchaseOrderDateTime;

  @JsonProperty(value = "dispatchProcessedItemInspections")
  @ApiModelProperty(value = "List of associated Dispatch Processed Item Inspections")
  private List<DispatchProcessedItemInspectionRepresentation> dispatchProcessedItemInspections;

  @JsonProperty(value = "processedItemDispatchBatch")
  @ApiModelProperty(value = "Associated Processed Item Dispatch Batch")
  private ProcessedItemDispatchBatchRepresentation processedItemDispatchBatch;

  @JsonProperty(value = "dispatchPackages")
  @ApiModelProperty(value = "List of packages in this dispatch batch")
  private List<DispatchPackageRepresentation> dispatchPackages;

  @JsonProperty("dispatchHeats")
  @ApiModelProperty(value = "List of dispatch heats associated with this batch")
  private List<DispatchHeatRepresentation> dispatchHeats;

  @JsonProperty(value = "packagingType")
  @ApiModelProperty(value = "Type of packaging used for dispatch", example = "BOX")
  private String packagingType;

  @JsonProperty(value = "packagingQuantity")
  @ApiModelProperty(value = "Total number of packages", example = "10")
  private Integer packagingQuantity;

  @JsonProperty(value = "perPackagingQuantity")
  @ApiModelProperty(value = "Quantity of items per package", example = "50")
  private Integer perPackagingQuantity;

  @JsonProperty(value = "useUniformPackaging")
  @ApiModelProperty(value = "Whether all packages have the same quantity", example = "false")
  private Boolean useUniformPackaging;

  @JsonProperty(value = "tenantId")
  @ApiModelProperty(value = "tenantId of the Dispatch Batch", example = "123")
  private Long tenantId;

  @JsonProperty(value = "buyerId")
  @ApiModelProperty(value = "buyerId of the Dispatch Batch", example = "123")
  private Long buyerId;

  @JsonProperty(value = "buyer")
  @ApiModelProperty(value = "buyer of the Dispatch Batch", example = "123")
  private BuyerRepresentation buyer;

  @JsonProperty(value = "billingEntityId")
  @ApiModelProperty(value = "buyerId of the Dispatch Batch", example = "123")
  private Long billingEntityId;

  @JsonProperty(value = "shippingEntityId")
  @ApiModelProperty(value = "shippingEntityId of the Dispatch Batch", example = "123")
  private Long shippingEntityId;
}
