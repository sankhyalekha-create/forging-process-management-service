package com.jangid.forging_process_management_service.entitiesRepresentation.dispatch;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representation for dispatch processed item consumption from previous operations.
 * Supports consumption from any type of previous operation (not just inspection).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchProcessedItemConsumptionRepresentation {

    @JsonProperty(value = "id")
    @ApiModelProperty(value = "Unique identifier for the consumption record", example = "1")
    private Long id;

    @JsonProperty(value = "previousOperationEntityId")
    @ApiModelProperty(value = "ID of the entity from the previous operation", example = "123", required = true)
    private Long previousOperationEntityId;

    @JsonProperty(value = "previousOperationType")
    @ApiModelProperty(value = "Type of the previous operation", example = "QUALITY", required = true)
    private String previousOperationType;

    @JsonProperty(value = "consumedPiecesCount")
    @ApiModelProperty(value = "Number of pieces consumed from this previous operation", example = "50", required = true)
    private Integer consumedPiecesCount;

    @JsonProperty(value = "availablePiecesCount")
    @ApiModelProperty(value = "Number of pieces that were available at the time of dispatch", example = "100")
    private Integer availablePiecesCount;

    @JsonProperty(value = "batchIdentifier")
    @ApiModelProperty(value = "Human-readable identifier for the batch", example = "InspectionBatch-123")
    private String batchIdentifier;

    @JsonProperty(value = "entityContext")
    @ApiModelProperty(value = "Additional context information about the consumed entity", example = "Quality inspection completed on 2025-01-01")
    private String entityContext;

    @JsonProperty(value = "dispatchBatch")
    @ApiModelProperty(value = "Associated dispatch batch (usually not included to avoid circular references)")
    private DispatchBatchRepresentation dispatchBatch;
}