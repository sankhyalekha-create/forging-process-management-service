package com.jangid.forging_process_management_service.entitiesRepresentation.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculatedVendorInventoryRepresentation {

    private Long id; // Use dispatch batch ID as reference
    private String workflowIdentifier;
    private Long itemWorkflowId;
    private String itemName;
    private String vendorDispatchBatchNumber;
    
    // Dispatch Information
    private LocalDateTime dispatchedAt;
    private Integer totalDispatchedPieces;
    private Double totalDispatchedQuantity;
    private Boolean isInPieces;
    
    // Receive Information (calculated from all receive batches)
    private Integer totalReceivedPieces;
    private Integer totalRejectedPieces;
    private Integer totalTenantRejects;
    
    // Calculated Inventory (Dispatched - Received)
    private Integer remainingPiecesAtVendor;
    private Double remainingQuantityAtVendor;
    
    // Heat Information from dispatch batch
    private List<VendorDispatchHeatRepresentation> dispatchHeats;
    
    // Receive Batch Summary
    private Integer totalReceiveBatches;
    private LocalDateTime lastReceivedAt;
    private Boolean fullyReceived;
    
    // Quality Check Summary
    private Boolean hasQualityCheckPending;
    private Boolean allQualityChecksCompleted;
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorDispatchHeatRepresentation {
        private Long heatId;
        private String heatNumber;
        private String consumptionType; // QUANTITY or PIECES
        private Double quantityUsed;
        private Integer piecesUsed;
        private String testCertificateNumber;
        private LocalDateTime createdAt;
    }
} 