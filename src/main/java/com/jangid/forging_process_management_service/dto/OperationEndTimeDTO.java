package com.jangid.forging_process_management_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationEndTimeDTO {
    
    /**
     * The end time of the operation (format: yyyy-MM-dd HH:mm:ss)
     * This will be used for UI validation when creating the next workflow step
     */
    private String endTime;
    
    /**
     * The operation type that was queried
     */
    private String operationType;
    
    /**
     * The processed item ID that was queried
     */
    private Long processedItemId;
    
    /**
     * Additional context information about where the end time was retrieved from
     * e.g., "forge.endAt", "lastForgeShift.endDateTime", "heatTreatmentBatch.endAt", etc.
     */
    private String source;
} 