package com.jangid.forging_process_management_service.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorInventoryReturnRequest {
    
    private Long vendorId;
    private LocalDateTime transactionDateTime;
    private String remarks;
    private List<VendorInventoryReturnItem> returnItems;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorInventoryReturnItem {
        private Long vendorInventoryId;
        private Double quantity;
        private Integer pieces;
    }
} 