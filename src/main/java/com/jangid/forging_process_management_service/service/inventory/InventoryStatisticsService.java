package com.jangid.forging_process_management_service.service.inventory;

import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.InwardOutwardStatisticsRepresentation;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.inventory.HeatRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class InventoryStatisticsService {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private HeatRepository heatRepository;

    @Autowired
    private DispatchBatchRepository dispatchBatchRepository;

    /**
     * Get inward vs outward statistics for a date range
     *
     * @param tenantId Tenant ID
     * @param fromMonth Starting month (1-12)
     * @param fromYear Starting year
     * @param toMonth Ending month (1-12)
     * @param toYear Ending year
     * @return Statistics representation
     */
    public InwardOutwardStatisticsRepresentation getInwardOutwardStatistics(
            Long tenantId, int fromMonth, int fromYear, int toMonth, int toYear) {
        
        log.info("Fetching inward-outward statistics for tenant={}, from={}-{}, to={}-{}", 
                tenantId, fromYear, fromMonth, toYear, toMonth);
        
        // Create start and end date time boundaries
        LocalDateTime startDateTime = LocalDateTime.of(fromYear, fromMonth, 1, 0, 0, 0);
        
        // Set end date to the last day of the month
        LocalDateTime endDateTime = LocalDateTime.of(toYear, toMonth, 1, 23, 59, 59)
            .plusMonths(1).minusDays(1);
        
        // Validate tenant exists
        tenantService.isTenantExists(tenantId);
        
        // Initialize result structure
        Map<String, InwardOutwardStatisticsRepresentation.MonthlyInwardOutwardStatistics> monthlyBreakdown = new HashMap<>();
        double totalInwardQuantityKgs = 0;
        double totalOutwardQuantityKgs = 0;
        int totalInwardPieces = 0;
        int totalOutwardPieces = 0;
        
        // Get all heats (inward) in the date range
        List<Heat> heats = heatRepository.findHeatsByDateRange(tenantId, startDateTime, endDateTime);
        
        // Process inward data
        for (Heat heat : heats) {
            // Get month key (YYYY-MM) from raw material receiving date
            String monthKey = getMonthKey(heat.getRawMaterialProduct().getRawMaterial().getRawMaterialReceivingDate());
            
            // Initialize monthly statistics if not exists
            if (!monthlyBreakdown.containsKey(monthKey)) {
                monthlyBreakdown.put(monthKey, 
                    InwardOutwardStatisticsRepresentation.MonthlyInwardOutwardStatistics.builder()
                        .inwardQuantityKgs(0)
                        .outwardQuantityKgs(0)
                        .inwardPieces(0)
                        .outwardPieces(0)
                        .build());
            }
            
            // Get current statistics for the month
            InwardOutwardStatisticsRepresentation.MonthlyInwardOutwardStatistics monthStats = 
                monthlyBreakdown.get(monthKey);
            
            // Process based on heat type
            if (heat.getIsInPieces()) {
                // Heat in PIECES
                int piecesCount = heat.getPiecesCount() != null ? heat.getPiecesCount() : 0;
                monthStats.setInwardPieces(monthStats.getInwardPieces() + piecesCount);
                totalInwardPieces += piecesCount;
            } else {
                // Heat in KGS
                double quantity = heat.getHeatQuantity() != null ? heat.getHeatQuantity() : 0;
                monthStats.setInwardQuantityKgs(monthStats.getInwardQuantityKgs() + quantity);
                totalInwardQuantityKgs += quantity;
            }
        }
        
        // Get all dispatched batches (outward) with DISPATCHED status in the date range
        List<DispatchBatch> dispatchBatches = 
            dispatchBatchRepository.findDispatchedBatchesByDateRange(tenantId, startDateTime, endDateTime);
        
        // Process outward data
        for (DispatchBatch dispatchBatch : dispatchBatches) {
            // Skip if no processedItemDispatchBatch or if no items
            if (dispatchBatch.getProcessedItemDispatchBatch() == null || 
                dispatchBatch.getDispatchProcessedItemInspections() == null || 
                dispatchBatch.getDispatchProcessedItemInspections().isEmpty()) {
                continue;
            }
            
            // Get month key (YYYY-MM)
            String monthKey = getMonthKey(dispatchBatch.getDispatchedAt());
            
            // Initialize monthly statistics if not exists
            if (!monthlyBreakdown.containsKey(monthKey)) {
                monthlyBreakdown.put(monthKey, 
                    InwardOutwardStatisticsRepresentation.MonthlyInwardOutwardStatistics.builder()
                        .inwardQuantityKgs(0)
                        .outwardQuantityKgs(0)
                        .inwardPieces(0)
                        .outwardPieces(0)
                        .build());
            }
            
            // Get current statistics for the month
            InwardOutwardStatisticsRepresentation.MonthlyInwardOutwardStatistics monthStats = 
                monthlyBreakdown.get(monthKey);
            
            // Get the total dispatch count from processedItemDispatchBatch
            Integer totalDispatchCount = dispatchBatch.getProcessedItemDispatchBatch().getTotalDispatchPiecesCount();
            if (totalDispatchCount == null || totalDispatchCount == 0) {
                continue;
            }
            
            // Check if this dispatch was from a heat measured in pieces
            boolean isFromPiecesHeat = false;
            
            // Check if the dispatch is from pieces heat based on itemCount=1
            if (dispatchBatch.getProcessedItemDispatchBatch() != null && 
                dispatchBatch.getProcessedItemDispatchBatch().getProcessedItem() != null && 
                dispatchBatch.getProcessedItemDispatchBatch().getProcessedItem().getItem() != null) {
                
                // If itemCount=1, it means the dispatch is associated with pieces heat
                Integer itemCount = dispatchBatch.getProcessedItemDispatchBatch().getProcessedItem().getItem().getItemCount();
                isFromPiecesHeat = (itemCount != null && itemCount == 1);
            }
            
            if (isFromPiecesHeat) {
                // Count pieces for PIECES heat
                monthStats.setOutwardPieces(monthStats.getOutwardPieces() + totalDispatchCount);
                totalOutwardPieces += totalDispatchCount;
            } else {
                // Calculate weight for KGS heat
                // Get the processed item ID from the dispatch batch
                Long processedItemId = dispatchBatch.getProcessedItemDispatchBatch().getProcessedItem().getId();
                double itemWeight = getFinishedItemWeight(processedItemId);
                double dispatchedWeight = totalDispatchCount * itemWeight;
                
                monthStats.setOutwardQuantityKgs(monthStats.getOutwardQuantityKgs() + dispatchedWeight);
                totalOutwardQuantityKgs += dispatchedWeight;
            }
        }
        
        // Build and return the response
        return InwardOutwardStatisticsRepresentation.builder()
            .totalInwardQuantityKgs(totalInwardQuantityKgs)
            .totalOutwardQuantityKgs(totalOutwardQuantityKgs)
            .totalInwardPieces(totalInwardPieces)
            .totalOutwardPieces(totalOutwardPieces)
            .monthlyBreakdown(monthlyBreakdown)
            .build();
    }
    
    /**
     * Helper method to get month key in YYYY-MM format
     */
    private String getMonthKey(LocalDateTime dateTime) {
        return String.format("%d-%02d", dateTime.getYear(), dateTime.getMonthValue());
    }
    
    /**
     * Determine if a processed item came from a heat measured in pieces
     * 
     * @param processedItemId The processed item ID
     * @return true if from pieces heat, false if from KGS heat
     */
    private boolean isProcessedItemFromPiecesHeat(Long processedItemId) {
        // Using a direct query that checks if the processed item's source heat used pieces
        // This is a simplified implementation - adapt as needed for your data model
        return heatRepository.isProcessedItemFromPiecesHeat(processedItemId);
    }
    
    /**
     * Get the finished weight of an item
     * 
     * @param processedItemId The processed item ID
     * @return The finished weight in KGS
     */
    private double getFinishedItemWeight(Long processedItemId) {
        return heatRepository.getProcessedItemFinishedWeight(processedItemId);
    }
} 