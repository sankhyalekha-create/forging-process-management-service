package com.jangid.forging_process_management_service.entitiesRepresentation.vendor;

import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "Representation of processed item vendor dispatch batch for API responses")
public class ProcessedItemVendorDispatchBatchRepresentation {

    @JsonProperty("id")
    @ApiModelProperty(value = "Unique identifier of the processed item vendor dispatch batch", example = "123")
    private Long id;

    @JsonProperty("vendorDispatchBatchId")
    @ApiModelProperty(value = "ID of the parent vendor dispatch batch", example = "456")
    private Long vendorDispatchBatchId;

    @JsonProperty("vendorDispatchBatch")
    @ApiModelProperty(value = "The associated vendor dispatch batch (full representation, optional to avoid circular references)")
    private VendorDispatchBatchRepresentation vendorDispatchBatch;

    @JsonProperty("item")
    @ApiModelProperty(value = "Item being processed by the vendor")
    private ItemRepresentation item;

    @JsonProperty("itemStatus")
    @ApiModelProperty(value = "Current status of the item in vendor processing", example = "VENDOR_DISPATCH_IN_PROGRESS")
    private String itemStatus;

    // Workflow Integration Fields
    @JsonProperty("workflowIdentifier")
    @ApiModelProperty(value = "Workflow identifier for batch-level workflow tracking")
    private String workflowIdentifier;

    @JsonProperty("itemWorkflowId")
    @ApiModelProperty(value = "ID of the associated item workflow", example = "789")
    private Long itemWorkflowId;

    @JsonProperty("previousOperationProcessedItemId")
    @ApiModelProperty(value = "ID of the processed item from the previous operation", example = "101")
    private Long previousOperationProcessedItemId;

    // Heat Tracking - Track which heats are consumed during vendor dispatch
    @JsonProperty("vendorDispatchHeats")
    @ApiModelProperty(value = "List of heat consumption records for this processed item vendor dispatch")
    private List<VendorDispatchHeatRepresentation> vendorDispatchHeats;

    // Simple Dispatch Tracking - Just what was sent to vendor
    @JsonProperty("isInPieces")
    @ApiModelProperty(value = "Whether measurement is in pieces (true) or weight/volume (false)", example = "true")
    private Boolean isInPieces;

    @JsonProperty("dispatchedPiecesCount")
    @ApiModelProperty(value = "Number of pieces dispatched to vendor", example = "100")
    private Integer dispatchedPiecesCount;

    @JsonProperty("dispatchedQuantity")
    @ApiModelProperty(value = "Quantity dispatched to vendor (for non-piece items)", example = "50.5")
    private Double dispatchedQuantity;

    // Expected quantities
    @JsonProperty("totalExpectedPiecesCount")
    @ApiModelProperty(value = "Total expected pieces count", example = "100")
    private Integer totalExpectedPiecesCount;

    // Total Received Tracking
    @JsonProperty("totalReceivedPiecesCount")
    @ApiModelProperty(value = "Total pieces received across all vendor receive batches", example = "95")
    private Integer totalReceivedPiecesCount;

    // Total Rejected Tracking
    @JsonProperty("totalRejectedPiecesCount")
    @ApiModelProperty(value = "Total pieces rejected across all vendor receive batches", example = "5")
    private Integer totalRejectedPiecesCount;

    // Total Tenant Rejects Tracking
    @JsonProperty("totalTenantRejectsCount")
    @ApiModelProperty(value = "Total tenant rejects across all vendor receive batches", example = "3")
    private Integer totalTenantRejectsCount;

    // Total Pieces Eligible for Next Operation
    @JsonProperty("totalPiecesEligibleForNextOperation")
    @ApiModelProperty(value = "Total pieces eligible for next operation across all vendor receive batches", example = "90")
    private Integer totalPiecesEligibleForNextOperation;

    @JsonProperty("fullyReceived")
    @ApiModelProperty(value = "Whether all dispatched quantities have been received", example = "false")
    private Boolean fullyReceived;

    // Audit fields
    @JsonProperty("createdAt")
    @ApiModelProperty(value = "Creation timestamp", example = "2024-01-15T10:00:00")
    private String createdAt;

    @JsonProperty("updatedAt")
    @ApiModelProperty(value = "Last update timestamp", example = "2024-01-20T14:30:00")
    private String updatedAt;

    @JsonProperty("deletedAt")
    @ApiModelProperty(value = "Deletion timestamp (if soft deleted)", example = "2024-01-25T10:00:00")
    private String deletedAt;

    @JsonProperty("deleted")
    @ApiModelProperty(value = "Whether the record is soft deleted", example = "false")
    private Boolean deleted;

    // Computed fields for convenience

    @JsonProperty("dispatchedQuantityValue")
    @ApiModelProperty(value = "Dispatched quantity value based on unit of measurement", example = "100.0")
    public Double getDispatchedQuantityValue() {
        if (isInPieces != null && isInPieces) {
            return dispatchedPiecesCount != null ? (double) dispatchedPiecesCount : 0.0;
        } else {
            return dispatchedQuantity != null ? dispatchedQuantity : 0.0;
        }
    }
} 