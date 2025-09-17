package com.jangid.forging_process_management_service.service.vendor;

import com.jangid.forging_process_management_service.dto.vendor.VendorInventoryTransferRequest;
import com.jangid.forging_process_management_service.dto.vendor.VendorInventoryReturnRequest;
import com.jangid.forging_process_management_service.entities.vendor.VendorInventoryTransaction;
import com.jangid.forging_process_management_service.entities.vendor.VendorInventoryTransactionItem;
import com.jangid.forging_process_management_service.entities.vendor.Vendor;
import com.jangid.forging_process_management_service.entities.vendor.VendorInventory;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorInventoryTransactionSummary;
import com.jangid.forging_process_management_service.repositories.vendor.VendorInventoryTransactionRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorDispatchHeatRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorRepository;
import com.jangid.forging_process_management_service.repositories.inventory.HeatRepository;
import com.jangid.forging_process_management_service.repositories.TenantRepository;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorInventoryTransactionService {

    private final VendorInventoryTransactionRepository vendorInventoryTransactionRepository;
    private final VendorDispatchHeatRepository vendorDispatchHeatRepository;
    private final VendorInventoryService vendorInventoryService;
    private final VendorRepository vendorRepository;
    private final HeatRepository heatRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public VendorInventoryTransaction batchTransferMaterialToVendor(Long tenantId, VendorInventoryTransferRequest request) {
        log.info("Starting batch transfer of {} items to vendor {}", 
                request.getHeatTransferItems().size(), request.getVendorId());

        // Validate vendor and tenant
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));
        
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + request.getVendorId()));

        // Create transaction
        VendorInventoryTransaction transaction = new VendorInventoryTransaction();
        transaction.setTenant(tenant);
        transaction.setVendor(vendor);
        transaction.setTransactionType(VendorInventoryTransaction.VendorInventoryTransactionType.TRANSFER_TO_VENDOR);
        transaction.setTransactionDateTime(request.getTransactionDateTime() != null ? 
                request.getTransactionDateTime() : LocalDateTime.now());
        transaction.setRemarks(request.getRemarks());

        List<VendorInventoryTransactionItem> transactionItems = new ArrayList<>();
        double totalQuantity = 0.0;
        int totalPieces = 0;

        // Process each heat transfer
        for (VendorInventoryTransferRequest.HeatTransferItem item : request.getHeatTransferItems()) {
            try {
                // Validate heat exists
                Heat heat = heatRepository.findById(item.getHeatId())
                        .orElseThrow(() -> new ResourceNotFoundException("Heat not found with id: " + item.getHeatId()));

                // Transfer material using existing service method
                vendorInventoryService.transferMaterialToVendor(
                        request.getVendorId(), 
                        item.getHeatId(), 
                        item.getQuantity(), 
                        item.getPieces()
                );

                // Create transaction item for audit trail
                VendorInventoryTransactionItem transactionItem = new VendorInventoryTransactionItem();
                transactionItem.setVendorInventoryTransaction(transaction);
                transactionItem.setHeat(heat);
                transactionItem.setQuantityTransferred(item.getQuantity());
                transactionItem.setPiecesTransferred(item.getPieces());
                
                // Override test certificate number if provided
                if (item.getTestCertificateNumber() != null && !item.getTestCertificateNumber().trim().isEmpty()) {
                    transactionItem.setTestCertificateNumber(item.getTestCertificateNumber());
                }

                transactionItems.add(transactionItem);

                // Accumulate totals
                if (item.getQuantity() != null) {
                    totalQuantity += item.getQuantity();
                }
                if (item.getPieces() != null) {
                    totalPieces += item.getPieces();
                }

                log.debug("Successfully transferred heat {} to vendor {}", item.getHeatId(), request.getVendorId());

            } catch (Exception e) {
                log.error("Failed to transfer heat {} to vendor {}: {}", 
                        item.getHeatId(), request.getVendorId(), e.getMessage());
                throw new RuntimeException("Failed to transfer heat " + item.getHeatId() + ": " + e.getMessage(), e);
            }
        }

        transaction.setTotalQuantityTransferred(totalQuantity > 0 ? totalQuantity : null);
        transaction.setTotalPiecesTransferred(totalPieces > 0 ? totalPieces : null);
        transaction.setTransactionItems(transactionItems);

        // Save transaction
        VendorInventoryTransaction savedTransaction = vendorInventoryTransactionRepository.save(transaction);
        
        log.info("Successfully completed batch transfer of {} items to vendor {}. Transaction ID: {}", 
                request.getHeatTransferItems().size(), request.getVendorId(), savedTransaction.getId());

        return savedTransaction;
    }

    @Transactional
    public VendorInventoryTransaction batchReturnMaterialFromVendor(Long tenantId, VendorInventoryReturnRequest request) {
        log.info("Starting batch return of {} items from vendor {}", 
                request.getReturnItems().size(), request.getVendorId());

        // Validate vendor and tenant
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));
        
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + request.getVendorId()));

        // Create transaction
        VendorInventoryTransaction transaction = new VendorInventoryTransaction();
        transaction.setTenant(tenant);
        transaction.setVendor(vendor);
        transaction.setTransactionType(VendorInventoryTransaction.VendorInventoryTransactionType.RETURN_FROM_VENDOR);
        transaction.setTransactionDateTime(request.getTransactionDateTime() != null ? 
                request.getTransactionDateTime() : LocalDateTime.now());
        transaction.setRemarks(request.getRemarks());

        List<VendorInventoryTransactionItem> transactionItems = new ArrayList<>();
        double totalQuantity = 0.0;
        int totalPieces = 0;

        // Process each return item
        for (VendorInventoryReturnRequest.VendorInventoryReturnItem item : request.getReturnItems()) {
            try {
                // Get vendor inventory first to access heat information before returning
                VendorInventory vendorInventory = vendorInventoryService.getVendorInventoryById(item.getVendorInventoryId());
                
                Heat originalHeat = vendorInventory.getOriginalHeat();
                
                // Return material using existing service method
                boolean success = vendorInventoryService.returnMaterialFromVendor(
                        item.getVendorInventoryId(), 
                        item.getQuantity(), 
                        item.getPieces()
                );

                if (!success) {
                    throw new RuntimeException("Failed to return material from vendor inventory " + item.getVendorInventoryId());
                }

                // Create transaction item for audit trail
                VendorInventoryTransactionItem transactionItem = new VendorInventoryTransactionItem();
                transactionItem.setVendorInventoryTransaction(transaction);
                transactionItem.setHeat(originalHeat);
                transactionItem.setQuantityTransferred(item.getQuantity());
                transactionItem.setPiecesTransferred(item.getPieces());
                transactionItem.setHeatNumber(originalHeat.getHeatNumber());
                transactionItem.setTestCertificateNumber(originalHeat.getTestCertificateNumber());
                transactionItem.setLocation(originalHeat.getLocation());
                transactionItem.setIsInPieces(originalHeat.getIsInPieces());
                
                transactionItems.add(transactionItem);
                
                // Accumulate totals
                if (item.getQuantity() != null) {
                    totalQuantity += item.getQuantity();
                }
                if (item.getPieces() != null) {
                    totalPieces += item.getPieces();
                }

                log.debug("Successfully returned material from vendor inventory {} to tenant heat {}", 
                         item.getVendorInventoryId(), originalHeat.getId());

            } catch (Exception e) {
                log.error("Failed to return material from vendor inventory {}: {}", 
                        item.getVendorInventoryId(), e.getMessage());
                throw new RuntimeException("Failed to return material from vendor inventory " + item.getVendorInventoryId() + ": " + e.getMessage(), e);
            }
        }

        transaction.setTotalQuantityTransferred(totalQuantity > 0 ? totalQuantity : null);
        transaction.setTotalPiecesTransferred(totalPieces > 0 ? totalPieces : null);
        transaction.setTransactionItems(transactionItems);

        // Save transaction
        VendorInventoryTransaction savedTransaction = vendorInventoryTransactionRepository.save(transaction);
        
        log.info("Successfully completed batch return of {} items from vendor {}. Transaction ID: {}", 
                request.getReturnItems().size(), request.getVendorId(), savedTransaction.getId());

        return savedTransaction;
    }

    public Page<VendorInventoryTransaction> getVendorInventoryTransactions(Long tenantId, Long vendorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (vendorId != null) {
            return vendorInventoryTransactionRepository.findByTenantIdAndVendorId(tenantId, vendorId, pageable);
        } else {
            return vendorInventoryTransactionRepository.findByTenantId(tenantId, pageable);
        }
    }

    public VendorInventoryTransaction getVendorInventoryTransactionById(Long transactionId) {
        return vendorInventoryTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor inventory transaction not found with id: " + transactionId));
    }

    /**
     * Get vendor inventory transaction summary for a specific vendor
     */
    public VendorInventoryTransactionSummary getVendorInventoryTransactionSummary(Long tenantId, Long vendorId) {
        log.info("Calculating vendor inventory transaction summary for vendor {}", vendorId);

        List<VendorInventoryTransaction> transactions = vendorInventoryTransactionRepository
                .findAllByTenantIdAndVendorId(tenantId, vendorId);

        VendorInventoryTransactionSummary.VendorInventoryTransactionSummaryBuilder summaryBuilder =
                VendorInventoryTransactionSummary.builder();

        // Initialize counters
        int totalTransactions = transactions.size();
        int totalTransferTransactions = 0;
        int totalReturnTransactions = 0;
        double totalTransferredQuantity = 0.0;
        int totalTransferredPieces = 0;
        double totalReturnedQuantity = 0.0;
        int totalReturnedPieces = 0;
        LocalDateTime lastTransferAt = null;
        LocalDateTime lastReturnAt = null;
        LocalDateTime firstTransactionAt = null;
        LocalDateTime lastTransactionAt = null;

        // Process each transaction
        for (VendorInventoryTransaction transaction : transactions) {
            LocalDateTime transactionDateTime = transaction.getTransactionDateTime();
            
            // Update first and last transaction times
            if (firstTransactionAt == null || transactionDateTime.isBefore(firstTransactionAt)) {
                firstTransactionAt = transactionDateTime;
            }
            if (lastTransactionAt == null || transactionDateTime.isAfter(lastTransactionAt)) {
                lastTransactionAt = transactionDateTime;
            }

            if (transaction.getTransactionType() == VendorInventoryTransaction.VendorInventoryTransactionType.TRANSFER_TO_VENDOR) {
                totalTransferTransactions++;
                if (lastTransferAt == null || transactionDateTime.isAfter(lastTransferAt)) {
                    lastTransferAt = transactionDateTime;
                }
                
                // Add transferred amounts
                if (transaction.getTotalQuantityTransferred() != null) {
                    totalTransferredQuantity += transaction.getTotalQuantityTransferred();
                }
                if (transaction.getTotalPiecesTransferred() != null) {
                    totalTransferredPieces += transaction.getTotalPiecesTransferred();
                }
                
            } else if (transaction.getTransactionType() == VendorInventoryTransaction.VendorInventoryTransactionType.RETURN_FROM_VENDOR) {
                totalReturnTransactions++;
                if (lastReturnAt == null || transactionDateTime.isAfter(lastReturnAt)) {
                    lastReturnAt = transactionDateTime;
                }
                
                // Add returned amounts
                if (transaction.getTotalQuantityTransferred() != null) {
                    totalReturnedQuantity += transaction.getTotalQuantityTransferred();
                }
                if (transaction.getTotalPiecesTransferred() != null) {
                    totalReturnedPieces += transaction.getTotalPiecesTransferred();
                }
            }
        }

        // Get total consumed by dispatch batches
        Double totalConsumedByDispatchQuantity = vendorDispatchHeatRepository.sumQuantityConsumedByVendor(vendorId, tenantId);
        Long totalConsumedByDispatchPiecesLong = vendorDispatchHeatRepository.sumPiecesConsumedByVendor(vendorId, tenantId);
        
        // Convert null values to 0
        if (totalConsumedByDispatchQuantity == null) totalConsumedByDispatchQuantity = 0.0;
        if (totalConsumedByDispatchPiecesLong == null) totalConsumedByDispatchPiecesLong = 0L;
        
        int totalConsumedByDispatchPieces = totalConsumedByDispatchPiecesLong.intValue();

        // Calculate net remaining (Transferred - Returned - Consumed_by_Dispatches)
        double netRemainingQuantity = Math.max(0, totalTransferredQuantity - totalReturnedQuantity - totalConsumedByDispatchQuantity);
        int netRemainingPieces = Math.max(0, totalTransferredPieces - totalReturnedPieces - totalConsumedByDispatchPieces);

        log.info("Vendor {} inventory summary: Transferred={}KG/{}pcs, Returned={}KG/{}pcs, ConsumedByDispatch={}KG/{}pcs, NetRemaining={}KG/{}pcs", 
                vendorId, totalTransferredQuantity, totalTransferredPieces, 
                totalReturnedQuantity, totalReturnedPieces,
                totalConsumedByDispatchQuantity, totalConsumedByDispatchPieces,
                netRemainingQuantity, netRemainingPieces);

        return summaryBuilder
                .totalTransactions(totalTransactions)
                .totalTransferTransactions(totalTransferTransactions)
                .totalReturnTransactions(totalReturnTransactions)
                .totalTransferredQuantity(totalTransferredQuantity)
                .totalTransferredPieces(totalTransferredPieces)
                .totalReturnedQuantity(totalReturnedQuantity)
                .totalReturnedPieces(totalReturnedPieces)
                .totalConsumedByDispatchQuantity(totalConsumedByDispatchQuantity)
                .totalConsumedByDispatchPieces(totalConsumedByDispatchPieces)
                .netRemainingQuantity(netRemainingQuantity)
                .netRemainingPieces(netRemainingPieces)
                .lastTransferAt(lastTransferAt)
                .lastReturnAt(lastReturnAt)
                .firstTransactionAt(firstTransactionAt)
                .lastTransactionAt(lastTransactionAt)
                .build();
    }
} 