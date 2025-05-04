package com.jangid.forging_process_management_service.entitiesRepresentation.machining;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Representation for monthly machining batch statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyMachiningStatisticsRepresentation {
    
    /**
     * Total finished pieces across all machining batches in the period
     */
    private int totalFinished;
    
    /**
     * Total reworked pieces across all machining batches in the period
     */
    private int totalRework;
    
    /**
     * Total rejected pieces across all machining batches in the period
     */
    private int totalRejected;
    
    /**
     * Monthly breakdown of statistics
     * Key is in format "YYYY-MM"
     * Value contains the statistics for that month
     */
    private Map<String, MonthlyStatistics> monthlyBreakdown;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStatistics {
        private int finished;
        private int rework;
        private int rejected;
    }
} 