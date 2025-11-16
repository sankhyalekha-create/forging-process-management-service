package com.jangid.forging_process_management_service.entitiesRepresentation.vendor;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entities.vendor.VendorProcessType;
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
@ApiModel(description = "Vendor dispatch batch representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorDispatchBatchRepresentation {
    
    @JsonProperty(value = "id")
    @ApiModelProperty(value = "Id of the vendor dispatch batch", example = "123")
    private Long id;

    @JsonProperty("vendor")
    @ApiModelProperty(value = "vendor")
    private VendorRepresentation vendor;

    @JsonProperty("vendorReceiveBatches")
    @ApiModelProperty(value = "List of associated vendor receive batches")
    private List<VendorReceiveBatchRepresentation> vendorReceiveBatches;

    @JsonProperty("vendorDispatchBatchNumber")
    @ApiModelProperty(value = "Vendor dispatch batch number")
    private String vendorDispatchBatchNumber;
    
    @JsonProperty("originalVendorDispatchBatchNumber")
    @ApiModelProperty(value = "Original vendor dispatch batch number")
    private String originalVendorDispatchBatchNumber;

    @JsonProperty("vendorDispatchBatchStatus")
    @ApiModelProperty(value = "Status of the vendor dispatch batch")
    private String vendorDispatchBatchStatus;

    @JsonProperty("dispatchedAt")
    @ApiModelProperty(value = "When dispatch was completed")
    private String dispatchedAt;
    
    @JsonProperty("remarks")
    @ApiModelProperty(value = "Additional remarks")
    private String remarks;

    @JsonProperty("processes")
    @ApiModelProperty(value = "List of processes to be performed by vendor")
    private List<VendorProcessType> processes;

    @JsonProperty("billingEntityId")
    @ApiModelProperty(value = "Id of the billing entity", example = "123")
    private Long billingEntityId;

    @JsonProperty("shippingEntityId")
    @ApiModelProperty(value = "Id of the shipping entity", example = "123")
    private Long shippingEntityId;

    @JsonProperty("packagingType")
    @ApiModelProperty(value = "Type of packaging")
    private String packagingType;

    @JsonProperty("packagingQuantity")
    @ApiModelProperty(value = "Number of packages")
    private Integer packagingQuantity;

    @JsonProperty("perPackagingQuantity")
    @ApiModelProperty(value = "Quantity per package")
    private Integer perPackagingQuantity;

    @JsonProperty("useUniformPackaging")
    @ApiModelProperty(value = "Whether to use uniform packaging")
    private Boolean useUniformPackaging;

    @JsonProperty("remainingPieces")
    @ApiModelProperty(value = "Remaining pieces not part of any full package (only when useUniformPackaging=false)")
    private Integer remainingPieces;

    @JsonProperty("itemWeightType")
    @ApiModelProperty(value = "Type of weight to use for calculations", example = "ITEM_WEIGHT, ITEM_SLUG_WEIGHT, ITEM_FORGED_WEIGHT, ITEM_FINISHED_WEIGHT")
    private String itemWeightType;

    @JsonProperty("processedItem")
    @ApiModelProperty(value = "The associated processed item vendor dispatch batch (full representation)")
    private ProcessedItemVendorDispatchBatchRepresentation processedItem;

    @JsonProperty("orderPoNumber")
    @ApiModelProperty(value = "Order PO number derived from itemWorkflowId")
    private String orderPoNumber;

    @JsonProperty("orderDate")
    @ApiModelProperty(value = "Order date")
    private String orderDate;

    @JsonProperty("orderId")
    @ApiModelProperty(value = "Order ID")
    private Long orderId;
} 