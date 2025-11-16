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

import jakarta.validation.constraints.Min;


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

    @JsonProperty("useUniformPackaging")
    @ApiModelProperty(value = "Whether to use uniform packaging")
    private Boolean useUniformPackaging;

    @JsonProperty("remainingPieces")
    @ApiModelProperty(value = "Remaining pieces not part of any full package (only when useUniformPackaging=false)")
    private Integer remainingPieces;

    // Quality Check Completion Fields (Required when qualityCheckCompleted=true)
    @JsonProperty("qualityCheckCompletedAt")
    @ApiModelProperty(value = "When quality check was completed (automatically set)")
    private String qualityCheckCompletedAt;

    @JsonProperty("finalVendorRejectsCount")
    @ApiModelProperty(value = "Final vendor rejects count after quality check (required when qualityCheckCompleted=true)", example = "5")
    @Min(value = 0, message = "Final vendor rejects count must be non-negative")
    private Integer finalVendorRejectsCount;

    @JsonProperty("finalTenantRejectsCount")
    @ApiModelProperty(value = "Final tenant rejects count after quality check (required when qualityCheckCompleted=true)", example = "3")
    @Min(value = 0, message = "Final tenant rejects count must be non-negative")
    private Integer finalTenantRejectsCount;

    @JsonProperty("qualityCheckRemarks")
    @ApiModelProperty(value = "Quality check completion remarks", example = "Surface defects found in some pieces")
    private String qualityCheckRemarks;

    @JsonProperty("isLocked")
    @ApiModelProperty(value = "Whether the batch is locked from further modifications (automatically set)")
    private Boolean isLocked;

    @JsonProperty("totalFinalRejectsCount")
    @ApiModelProperty(value = "Total final rejects count (vendor + tenant, automatically calculated)")
    private Integer totalFinalRejectsCount;
} 