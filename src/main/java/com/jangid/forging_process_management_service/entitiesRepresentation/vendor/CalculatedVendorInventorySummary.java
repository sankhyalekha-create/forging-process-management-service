package com.jangid.forging_process_management_service.entitiesRepresentation.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculatedVendorInventorySummary {

    // Basic Counts
    private Integer totalDispatchBatches;
    private Integer totalReceiveBatches;
    private Integer activeWorkflows; // Workflows with remaining inventory
    
    // Dispatch Totals
    private Integer totalDispatchedPieces;
    private Double totalDispatchedQuantity;
    
    // Receive Totals
    private Integer totalReceivedPieces;
    private Integer totalRejectedPieces;
    private Integer totalTenantRejects;
    
    // Calculated Remaining Inventory
    private Integer totalRemainingPieces;
    private Double totalRemainingQuantity;
    
    // Quality Check Status
    private Integer batchesWithQualityCheckPending;
    private Integer batchesWithAllQualityChecksCompleted;
    
    // Time Information
    private LocalDateTime lastDispatchAt;
    private LocalDateTime lastReceivedAt;
} 