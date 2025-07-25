package com.jangid.forging_process_management_service.entitiesRepresentation.vendor;

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
public class CalculatedVendorInventoryListRepresentation {

    private List<CalculatedVendorInventoryRepresentation> calculatedInventories;
    
    // Summary Information
    private Integer totalDispatchBatches;
    private Integer totalRemainingPieces;
    private Double totalRemainingQuantity;
    private Integer totalActiveWorkflows;
    
    public CalculatedVendorInventoryListRepresentation(List<CalculatedVendorInventoryRepresentation> calculatedInventories) {
        this.calculatedInventories = calculatedInventories;
        calculateSummaryInfo();
    }
    
    private void calculateSummaryInfo() {
        if (calculatedInventories == null || calculatedInventories.isEmpty()) {
            this.totalDispatchBatches = 0;
            this.totalRemainingPieces = 0;
            this.totalRemainingQuantity = 0.0;
            this.totalActiveWorkflows = 0;
            return;
        }
        
        this.totalDispatchBatches = calculatedInventories.size();
        this.totalRemainingPieces = calculatedInventories.stream()
                .mapToInt(inv -> inv.getRemainingPiecesAtVendor() != null ? inv.getRemainingPiecesAtVendor() : 0)
                .sum();
        this.totalRemainingQuantity = calculatedInventories.stream()
                .mapToDouble(inv -> inv.getRemainingQuantityAtVendor() != null ? inv.getRemainingQuantityAtVendor() : 0.0)
                .sum();
        this.totalActiveWorkflows = (int) calculatedInventories.stream()
                .filter(inv -> (inv.getRemainingPiecesAtVendor() != null && inv.getRemainingPiecesAtVendor() > 0) ||
                              (inv.getRemainingQuantityAtVendor() != null && inv.getRemainingQuantityAtVendor() > 0))
                .count();
    }
} 