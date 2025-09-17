package com.jangid.forging_process_management_service.entitiesRepresentation.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorInventoryTransactionSummary {

    // Basic Counts
    private Integer totalTransactions;
    private Integer totalTransferTransactions;
    private Integer totalReturnTransactions;
    
    // Total Transferred (from all TRANSFER_TO_VENDOR transactions)
    private Double totalTransferredQuantity;
    private Integer totalTransferredPieces;
    
    // Total Returned (from all RETURN_FROM_VENDOR transactions)
    private Double totalReturnedQuantity;
    private Integer totalReturnedPieces;
    
    // Total Consumed by Dispatch Batches (from VendorDispatchHeat)
    private Double totalConsumedByDispatchQuantity;
    private Integer totalConsumedByDispatchPieces;
    
    // Net Remaining (Transferred - Returned - Consumed_by_Dispatches)
    private Double netRemainingQuantity;
    private Integer netRemainingPieces;
    
    // Time Information
    private LocalDateTime lastTransferAt;
    private LocalDateTime lastReturnAt;
    private LocalDateTime firstTransactionAt;
    private LocalDateTime lastTransactionAt;
}
