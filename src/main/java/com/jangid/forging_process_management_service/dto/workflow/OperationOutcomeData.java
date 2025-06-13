package com.jangid.forging_process_management_service.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lightweight operation outcome data for workflow flow control
 * Detailed operation data remains in existing entities (ProcessedItem, ProcessedItemHeatTreatmentBatch, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationOutcomeData {

    private String operationEntityType; // "FORGE", "HEAT_TREATMENT_BATCH", etc.
    
    // For FORGING operation - single object
    private ForgingOutcome forgingData;
    
    // For other operations - array of objects
    private List<BatchOutcome> batchData;
    
    // Basic completion tracking
    private LocalDateTime operationLastUpdatedAt;

    /**
     * Single forging operation outcome data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForgingOutcome {
        private Long id;
        private Integer initialPiecesCount;
        private Integer piecesAvailableForNext;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
        private Boolean deleted;
    }
    
    /**
     * Batch operation outcome data (for heat treatment, machining, quality, dispatch)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchOutcome {
        private Long id;
        private Integer initialPiecesCount;
        private Integer piecesAvailableForNext;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
        private Boolean deleted;
    }
    
    /**
     * Creates outcome data for forging operation
     * Detailed data is stored in ProcessedItem
     */
    public static OperationOutcomeData forForgingOperation(ForgingOutcome forgingData, LocalDateTime lastUpdatedAt) {
        
        return OperationOutcomeData.builder()
                .operationEntityType("FORGE")
                .forgingData(forgingData)
                .operationLastUpdatedAt(lastUpdatedAt)
                .build();
    }
    
    /**
     * Creates outcome data for heat treatment operation
     * Detailed data is stored in ProcessedItemHeatTreatmentBatch
     */
    public static OperationOutcomeData forHeatTreatmentOperation(List<BatchOutcome> batchData, LocalDateTime lastUpdatedAt) {
        
        return OperationOutcomeData.builder()
                .operationEntityType("HEAT_TREATMENT_BATCH")
                .batchData(batchData)
                .operationLastUpdatedAt(lastUpdatedAt)
                .build();
    }
    
    /**
     * Creates outcome data for machining operation
     * Detailed data is stored in ProcessedItemMachiningBatch
     */
    public static OperationOutcomeData forMachiningOperation(List<BatchOutcome> batchData, LocalDateTime lastUpdatedAt) {
        
        return OperationOutcomeData.builder()
                .operationEntityType("MACHINING_BATCH")
                .batchData(batchData)
                .operationLastUpdatedAt(lastUpdatedAt)
                .build();
    }
    
    /**
     * Creates outcome data for quality inspection operation
     * Detailed data is stored in ProcessedItemInspectionBatch
     */
    public static OperationOutcomeData forQualityOperation(List<BatchOutcome> batchData, LocalDateTime lastUpdatedAt) {
        
        return OperationOutcomeData.builder()
                .operationEntityType("INSPECTION_BATCH")
                .batchData(batchData)
                .operationLastUpdatedAt(lastUpdatedAt)
                .build();
    }
    
    /**
     * Creates outcome data for dispatch operation
     * Detailed data is stored in ProcessedItemDispatchBatch
     */
    public static OperationOutcomeData forDispatchOperation(List<BatchOutcome> batchData, LocalDateTime lastUpdatedAt) {
        
        return OperationOutcomeData.builder()
                .operationEntityType("DISPATCH_BATCH")
                .batchData(batchData)
                .operationLastUpdatedAt(lastUpdatedAt)
                .build();
    }
} 