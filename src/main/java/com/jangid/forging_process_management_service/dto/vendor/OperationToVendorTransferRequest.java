package com.jangid.forging_process_management_service.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for transferring materials from previous operation output to vendor inventory
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationToVendorTransferRequest {

    private Long vendorId;
    private Long itemWorkflowId;
    private String previousOperationType; // FORGING, MACHINING, etc.
    private String remarks;
    private LocalDateTime transferDateTime;
    private List<OperationOutputTransferItem> transferItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationOutputTransferItem {
        private Long previousOperationProcessedItemId; // ID of the processed item from previous operation
        private Integer piecesToTransfer;
        private String notes;
        
        // For cases where we need to specify heat information (if not already tracked in operation)
        private Long heatId; // Optional, for traceability
        private String heatNumber; // Optional, for traceability
        private String testCertificateNumber; // Optional override
    }
} 