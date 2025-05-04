package com.jangid.forging_process_management_service.entitiesRepresentation.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Representation class for inward vs outward statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InwardOutwardStatisticsRepresentation {
    
    /**
     * Total inward quantity in KGS for materials purchased in KGS
     */
    private double totalInwardQuantityKgs;
    
    /**
     * Total outward quantity in KGS for materials purchased in KGS
     */
    private double totalOutwardQuantityKgs;
    
    /**
     * Total inward count in pieces for materials purchased in PIECES
     */
    private int totalInwardPieces;
    
    /**
     * Total outward count in pieces for materials purchased in PIECES
     */
    private int totalOutwardPieces;
    
    /**
     * Monthly breakdown of inward/outward statistics
     * Key is in format "YYYY-MM"
     * Value contains the statistics for that month
     */
    private Map<String, MonthlyInwardOutwardStatistics> monthlyBreakdown;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyInwardOutwardStatistics {
        /**
         * Inward quantity in KGS for materials purchased in KGS
         */
        private double inwardQuantityKgs;
        
        /**
         * Outward quantity in KGS for materials purchased in KGS
         */
        private double outwardQuantityKgs;
        
        /**
         * Inward count in pieces for materials purchased in PIECES
         */
        private int inwardPieces;
        
        /**
         * Outward count in pieces for materials purchased in PIECES
         */
        private int outwardPieces;
    }
} 