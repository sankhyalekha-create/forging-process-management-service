package com.jangid.forging_process_management_service.entitiesRepresentation.vendor;

import com.jangid.forging_process_management_service.entities.vendor.VendorInventoryTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorInventoryTransactionRepresentation {
    
    private Long id;
    private Long vendorId;
    private String vendorName;
    private VendorInventoryTransaction.VendorInventoryTransactionType transactionType;
    private LocalDateTime transactionDateTime;
    private String remarks;
    private Double totalQuantityTransferred;
    private Integer totalPiecesTransferred;
    private List<VendorInventoryTransactionItemRepresentation> transactionItems;
    private LocalDateTime createdAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorInventoryTransactionItemRepresentation {
        private Long id;
        private Long heatId;
        private String heatNumber;
        private Double quantityTransferred;
        private Integer piecesTransferred;
        private String testCertificateNumber;
        private String location;
        private Boolean isInPieces;
        private LocalDateTime createdAt;
    }
} 