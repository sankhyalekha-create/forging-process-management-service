package com.jangid.forging_process_management_service.entitiesRepresentation.vendor;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Vendor receive batch representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorReceiveBatchRepresentation {
    
    @JsonProperty(value = "id")
    @ApiModelProperty(value = "Id of the vendor receive batch", example = "123")
    private Long id;

    @JsonProperty("vendor")
    @ApiModelProperty(value = "Vendor representation")
    private VendorRepresentation vendor;

    @JsonProperty("vendorDispatchBatch")
    @ApiModelProperty(value = "Associated vendor dispatch batch representation")
    private VendorDispatchBatchRepresentation vendorDispatchBatch;

    @JsonProperty("vendorReceiveBatchNumber")
    @ApiModelProperty(value = "Vendor receive batch number")
    private String vendorReceiveBatchNumber;
    
    @JsonProperty("originalVendorReceiveBatchNumber")
    @ApiModelProperty(value = "Original vendor receive batch number")
    private String originalVendorReceiveBatchNumber;

    @JsonProperty("vendorReceiveBatchStatus")
    @ApiModelProperty(value = "Status of the vendor receive batch")
    private String vendorReceiveBatchStatus;

    @JsonProperty("receivedAt")
    @ApiModelProperty(value = "When receive was completed")
    private String receivedAt;

    @JsonProperty("isInPieces")
    @ApiModelProperty(value = "Whether the item is measured in pieces or quantity")
    private Boolean isInPieces;

    @JsonProperty("receivedPiecesCount")
    @ApiModelProperty(value = "Actually received pieces count (for count-based measurements)")
    private Integer receivedPiecesCount;

    @JsonProperty("rejectedPiecesCount")
    @ApiModelProperty(value = "Rejected pieces count (for count-based measurements)")
    private Integer rejectedPiecesCount;

    @JsonProperty("tenantRejectsCount")
    @ApiModelProperty(value = "Tenant rejects count - items already rejected by tenant but dispatched to vendor")
    private Integer tenantRejectsCount;

    @JsonProperty("piecesEligibleForNextOperation")
    @ApiModelProperty(value = "Pieces eligible for next operation in workflow")
    private Integer piecesEligibleForNextOperation;

    @JsonProperty("qualityCheckRequired")
    @ApiModelProperty(value = "Whether quality check is required")
    private Boolean qualityCheckRequired;

    @JsonProperty("qualityCheckCompleted")
    @ApiModelProperty(value = "Whether quality check is completed")
    private Boolean qualityCheckCompleted;

    @JsonProperty("remarks")
    @ApiModelProperty(value = "Additional remarks")
    private String remarks;

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
} 