package com.jangid.forging_process_management_service.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorInventoryTransferRequest {
    
    private Long vendorId;
    private LocalDateTime transactionDateTime;
    private String remarks;
    private List<HeatTransferItem> heatTransferItems;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatTransferItem {
        private Long heatId;
        private Double quantity;
        private Integer pieces;
        private String testCertificateNumber; // Optional override
    }
} 