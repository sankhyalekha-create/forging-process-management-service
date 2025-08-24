package com.jangid.forging_process_management_service.service.vendor;

import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.vendor.Vendor;
import com.jangid.forging_process_management_service.entities.vendor.VendorInventory;
import com.jangid.forging_process_management_service.exception.vendor.VendorInventoryNotFoundException;
import com.jangid.forging_process_management_service.repositories.vendor.VendorDispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorInventoryRepository;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.vendor.VendorRepository;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.vendor.VendorReceiveBatch;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchHeat;
import com.jangid.forging_process_management_service.entities.vendor.ProcessedItemVendorDispatchBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.CalculatedVendorInventoryRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.CalculatedVendorInventorySummary;

/**
 * Service to manage vendor inventory operations.
 * This service handles the separation between tenant inventory (Heat) and vendor inventory.
 */
@Slf4j
@Service
@Transactional
public class VendorInventoryService {

    @Autowired
    private VendorInventoryRepository vendorInventoryRepository;

    @Autowired
    private RawMaterialHeatService rawMaterialHeatService;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorDispatchBatchRepository vendorDispatchBatchRepository;

    /**
     * Transfers material from tenant Heat inventory to vendor inventory.
     * This is called during vendor dispatch batch creation.
     */
    public VendorInventory transferToVendorInventory(Vendor vendor, Heat heat, Double quantity, Integer pieces) {
        log.info("Transferring inventory to vendor {} from heat {}: quantity={}, pieces={}", 
                 vendor.getId(), heat.getId(), quantity, pieces);

        // Check if vendor inventory already exists for this heat
        Optional<VendorInventory> existingInventory = vendorInventoryRepository
                .findByVendorIdAndOriginalHeatIdAndDeletedFalse(vendor.getId(), heat.getId());

        VendorInventory vendorInventory;

        if (existingInventory.isPresent()) {
            // Update existing vendor inventory
            vendorInventory = existingInventory.get();
            
            if (vendorInventory.getIsInPieces()) {
                vendorInventory.setTotalDispatchedPieces(vendorInventory.getTotalDispatchedPieces() + pieces);
                vendorInventory.setAvailablePiecesCount(vendorInventory.getAvailablePiecesCount() + pieces);
            } else {
                vendorInventory.setTotalDispatchedQuantity(vendorInventory.getTotalDispatchedQuantity() + quantity);
                vendorInventory.setAvailableQuantity(vendorInventory.getAvailableQuantity() + quantity);
            }
            
            log.info("Updated existing vendor inventory {}: new total dispatched={}, new available={}", 
                     vendorInventory.getId(), 
                     vendorInventory.getTotalDispatchedQuantity(),
                     vendorInventory.getAvailableQuantity());
        } else {
            // Create new vendor inventory
            vendorInventory = VendorInventory.builder()
                    .vendor(vendor)
                    .originalHeat(heat)
                    .rawMaterialProduct(heat.getRawMaterialProduct())
                    .heatNumber(heat.getHeatNumber())
                    .isInPieces(heat.getIsInPieces())
                    .testCertificateNumber(heat.getTestCertificateNumber())
                    .createdAt(LocalDateTime.now())
                    .deleted(false)
                    .build();

            if (heat.getIsInPieces()) {
                vendorInventory.setTotalDispatchedPieces(pieces);
                vendorInventory.setAvailablePiecesCount(pieces);
            } else {
                vendorInventory.setTotalDispatchedQuantity(quantity);
                vendorInventory.setAvailableQuantity(quantity);
            }

            log.info("Created new vendor inventory for vendor {} from heat {}: dispatched={}, available={}",
                     vendor.getId(), heat.getId(),
                     vendorInventory.getTotalDispatchedQuantity(),
                     vendorInventory.getAvailableQuantity());
        }

        // Consume from original heat inventory (this removes it from tenant's available inventory)
        if (heat.getIsInPieces()) {
            if (!heat.consumeQuantity(pieces.doubleValue())) {
                throw new IllegalArgumentException("Insufficient pieces in heat " + heat.getId());
            }
        } else {
            if (!heat.consumeQuantity(quantity)) {
                throw new IllegalArgumentException("Insufficient quantity in heat " + heat.getId());
            }
        }

        // Save the updated heat
        rawMaterialHeatService.updateRawMaterialHeat(heat);

        // Save vendor inventory
        vendorInventory = vendorInventoryRepository.save(vendorInventory);

        log.info("Successfully transferred inventory to vendor inventory {}", vendorInventory.getId());
        return vendorInventory;
    }


    /**
     * Calculate vendor inventory from dispatch and receive batches without pagination
     */
    public List<CalculatedVendorInventoryRepresentation> getCalculatedInventoryByVendor(Long vendorId, Long tenantId) {
        log.info("Calculating vendor inventory for vendor {} from dispatch/receive batches", vendorId);

        // Get all dispatch batches for this vendor that are not deleted
        List<VendorDispatchBatch> dispatchBatches = vendorDispatchBatchRepository
                .findByVendorIdAndTenantIdAndDeletedFalse(vendorId, tenantId);

        return dispatchBatches.stream()
                .map(this::convertToCalculatedInventoryRepresentation)
                .filter(rep -> hasRemainingInventory(rep)) // Only include batches with remaining inventory
                .collect(Collectors.toList());
    }

    /**
     * Calculate vendor inventory from dispatch and receive batches with pagination
     */
    public Page<CalculatedVendorInventoryRepresentation> getCalculatedInventoryByVendor(Long vendorId, Long tenantId, int page, int size) {
        log.info("Calculating vendor inventory for vendor {} from dispatch/receive batches (page {}, size {})", vendorId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VendorDispatchBatch> dispatchBatchPage = vendorDispatchBatchRepository
                .findByVendorIdAndTenantIdAndDeletedFalse(vendorId, tenantId, pageable);

        return dispatchBatchPage.map(this::convertToCalculatedInventoryRepresentation);
    }

    /**
     * Get calculated vendor inventory summary
     */
    public CalculatedVendorInventorySummary getCalculatedInventorySummary(Long vendorId, Long tenantId) {
        log.info("Calculating vendor inventory summary for vendor {}", vendorId);

        List<VendorDispatchBatch> dispatchBatches = vendorDispatchBatchRepository
                .findByVendorIdAndTenantIdAndDeletedFalse(vendorId, tenantId);

        CalculatedVendorInventorySummary.CalculatedVendorInventorySummaryBuilder summaryBuilder =
                CalculatedVendorInventorySummary.builder();

        // Initialize counters
        int totalDispatchBatches = dispatchBatches.size();
        int totalReceiveBatches = 0;
        int activeWorkflows = 0;
        int totalDispatchedPieces = 0;
        double totalDispatchedQuantity = 0.0;
        int totalReceivedPieces = 0;
        int totalRejectedPieces = 0;
        int totalTenantRejects = 0;
        int totalRemainingPieces = 0;
        double totalRemainingQuantity = 0.0;
        int batchesWithQualityCheckPending = 0;
        int batchesWithAllQualityChecksCompleted = 0;
        LocalDateTime lastDispatchAt = null;
        LocalDateTime lastReceivedAt = null;

        // Process each dispatch batch
        for (VendorDispatchBatch dispatchBatch : dispatchBatches) {
            // Filter out deleted processed item
            ProcessedItemVendorDispatchBatch processedItem = dispatchBatch.getProcessedItem();
            if (processedItem != null && Boolean.TRUE.equals(processedItem.getDeleted())) {
                processedItem = null; // Treat as if no processed item exists
            }

            // Filter out deleted receive batches
            List<VendorReceiveBatch> receiveBatches = dispatchBatch.getVendorReceiveBatches().stream()
                    .filter(batch -> !batch.isDeleted()) // Only include non-deleted receive batches
                    .collect(Collectors.toList());

            // Count receive batches
            totalReceiveBatches += receiveBatches.size();

            // Dispatch totals
            if (processedItem != null) {
                if (Boolean.TRUE.equals(processedItem.getIsInPieces())) {
                    totalDispatchedPieces += processedItem.getDispatchedPiecesCount() != null ?
                            processedItem.getDispatchedPiecesCount() : 0;
                } else {
                    totalDispatchedQuantity += processedItem.getDispatchedQuantity() != null ?
                            processedItem.getDispatchedQuantity() : 0.0;
                }
            }

            // Track last dispatch time
            if (dispatchBatch.getDispatchedAt() != null) {
                if (lastDispatchAt == null || dispatchBatch.getDispatchedAt().isAfter(lastDispatchAt)) {
                    lastDispatchAt = dispatchBatch.getDispatchedAt();
                }
            }

            // Process receive batches
            int batchReceivedPieces = 0;
            int batchRejectedPieces = 0;
            int batchTenantRejects = 0;
            boolean hasQualityCheckPending = false;
            boolean allQualityChecksCompleted = true;

            for (VendorReceiveBatch receiveBatch : receiveBatches) {
                batchReceivedPieces += receiveBatch.getReceivedPiecesCount() != null ?
                        receiveBatch.getReceivedPiecesCount() : 0;
                batchRejectedPieces += receiveBatch.getRejectedPiecesCount() != null ?
                        receiveBatch.getRejectedPiecesCount() : 0;
                batchTenantRejects += receiveBatch.getTenantRejectsCount() != null ?
                        receiveBatch.getTenantRejectsCount() : 0;

                // Quality check status
                if (Boolean.TRUE.equals(receiveBatch.getQualityCheckRequired()) &&
                    !Boolean.TRUE.equals(receiveBatch.getQualityCheckCompleted())) {
                    hasQualityCheckPending = true;
                    allQualityChecksCompleted = false;
                }

                // Track last received time
                if (receiveBatch.getReceivedAt() != null) {
                    if (lastReceivedAt == null || receiveBatch.getReceivedAt().isAfter(lastReceivedAt)) {
                        lastReceivedAt = receiveBatch.getReceivedAt();
                    }
                }
            }

            // Update totals
            totalReceivedPieces += batchReceivedPieces;
            totalRejectedPieces += batchRejectedPieces;
            totalTenantRejects += batchTenantRejects;

            // Calculate remaining inventory for this batch
            int batchDispatchedPieces = processedItem != null && processedItem.getDispatchedPiecesCount() != null ?
                    processedItem.getDispatchedPiecesCount() : 0;
            double batchDispatchedQuantity = processedItem != null && processedItem.getDispatchedQuantity() != null ?
                    processedItem.getDispatchedQuantity() : 0.0;

            // Fix: Calculate remaining pieces correctly (same as individual calculation)
            int remainingPieces = Math.max(0, batchDispatchedPieces - batchReceivedPieces);
            
            // For quantity calculation, we need to handle the unit conversion properly
            // Since receiving is always done in pieces, we need to calculate remaining quantity based on the dispatch type
            double remainingQuantity = 0.0;
            if (processedItem != null && processedItem.getIsInPieces() != null && processedItem.getIsInPieces()) {
                // If dispatched in pieces, remaining quantity should be 0 (we track pieces only)
                remainingQuantity = 0.0;
            } else {
                // If dispatched in quantity (KG), we need to calculate remaining quantity
                // Since we only receive in pieces, we can't directly calculate remaining KG
                // We should use the remaining pieces to estimate remaining quantity
                // For now, let's set remaining quantity to 0 if all pieces are received
                remainingQuantity = remainingPieces > 0 ? batchDispatchedQuantity : 0.0;
            }

            // Only include in summary if there's actually remaining inventory
            if (remainingPieces > 0 || remainingQuantity > 0) {
                activeWorkflows++;
                totalRemainingPieces += remainingPieces;
                totalRemainingQuantity += remainingQuantity;
            }

            // Quality check counters
            if (hasQualityCheckPending) {
                batchesWithQualityCheckPending++;
            }
            if (allQualityChecksCompleted && !receiveBatches.isEmpty()) {
                batchesWithAllQualityChecksCompleted++;
            }
        }

        return summaryBuilder
                .totalDispatchBatches(totalDispatchBatches)
                .totalReceiveBatches(totalReceiveBatches)
                .activeWorkflows(activeWorkflows)
                .totalDispatchedPieces(totalDispatchedPieces)
                .totalDispatchedQuantity(totalDispatchedQuantity)
                .totalReceivedPieces(totalReceivedPieces)
                .totalRejectedPieces(totalRejectedPieces)
                .totalTenantRejects(totalTenantRejects)
                .totalRemainingPieces(totalRemainingPieces)
                .totalRemainingQuantity(totalRemainingQuantity)
                .batchesWithQualityCheckPending(batchesWithQualityCheckPending)
                .batchesWithAllQualityChecksCompleted(batchesWithAllQualityChecksCompleted)
                .lastDispatchAt(lastDispatchAt)
                .lastReceivedAt(lastReceivedAt)
                .build();
    }

    /**
     * Convert VendorDispatchBatch to CalculatedVendorInventoryRepresentation
     */
    private CalculatedVendorInventoryRepresentation convertToCalculatedInventoryRepresentation(VendorDispatchBatch dispatchBatch) {
        // Filter out deleted processed item
        ProcessedItemVendorDispatchBatch processedItem = dispatchBatch.getProcessedItem();
        if (processedItem != null && Boolean.TRUE.equals(processedItem.getDeleted())) {
            processedItem = null; // Treat as if no processed item exists
        }

        // Filter out deleted receive batches
        List<VendorReceiveBatch> receiveBatches = dispatchBatch.getVendorReceiveBatches().stream()
                .filter(batch -> !batch.isDeleted()) // Only include non-deleted receive batches
                .collect(Collectors.toList());

        // Calculate receive totals
        int totalReceivedPieces = receiveBatches.stream()
                .mapToInt(rb -> rb.getReceivedPiecesCount() != null ? rb.getReceivedPiecesCount() : 0)
                .sum();
        int totalRejectedPieces = receiveBatches.stream()
                .mapToInt(rb -> rb.getRejectedPiecesCount() != null ? rb.getRejectedPiecesCount() : 0)
                .sum();
        int totalTenantRejects = receiveBatches.stream()
                .mapToInt(rb -> rb.getTenantRejectsCount() != null ? rb.getTenantRejectsCount() : 0)
                .sum();


        // Calculate remaining inventory
        int dispatchedPieces = processedItem != null && processedItem.getDispatchedPiecesCount() != null ?
                processedItem.getDispatchedPiecesCount() : 0;
        double dispatchedQuantity = processedItem != null && processedItem.getDispatchedQuantity() != null ?
                processedItem.getDispatchedQuantity() : 0.0;

        int remainingPieces = Math.max(0, dispatchedPieces - totalReceivedPieces);
        
        // For quantity calculation, handle unit conversion properly
        double remainingQuantity = 0.0;
        if (processedItem != null && processedItem.getIsInPieces() != null && processedItem.getIsInPieces()) {
            // If dispatched in pieces, remaining quantity should be 0 (we track pieces only)
            remainingQuantity = 0.0;
        } else {
            // If dispatched in quantity (KG), calculate remaining quantity properly
            // Since we only receive in pieces, we can't directly calculate remaining KG
            // We should use the remaining pieces to estimate remaining quantity
            remainingQuantity = remainingPieces > 0 ? dispatchedQuantity : 0.0;
        }

        // Quality check status
        boolean hasQualityCheckPending = receiveBatches.stream()
                .anyMatch(rb -> Boolean.TRUE.equals(rb.getQualityCheckRequired()) &&
                               !Boolean.TRUE.equals(rb.getQualityCheckCompleted()));
        boolean allQualityChecksCompleted = !receiveBatches.isEmpty() &&
                receiveBatches.stream()
                .allMatch(rb -> !Boolean.TRUE.equals(rb.getQualityCheckRequired()) ||
                               Boolean.TRUE.equals(rb.getQualityCheckCompleted()));

        // Convert dispatch heats
        List<CalculatedVendorInventoryRepresentation.VendorDispatchHeatRepresentation> dispatchHeats =
                processedItem != null && processedItem.getVendorDispatchHeats() != null ?
                processedItem.getVendorDispatchHeats().stream()
                        .map(this::convertDispatchHeatToRepresentation)
                        .collect(Collectors.toList()) : List.of();

        // Last received date
        LocalDateTime lastReceivedAt = receiveBatches.stream()
                .map(VendorReceiveBatch::getReceivedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return CalculatedVendorInventoryRepresentation.builder()
                .id(dispatchBatch.getId())
                .workflowIdentifier(processedItem != null ? processedItem.getWorkflowIdentifier() : null)
                .itemWorkflowId(processedItem != null ? processedItem.getItemWorkflowId() : null)
                .itemName(processedItem != null && processedItem.getItem() != null ?
                        processedItem.getItem().getItemName() : null)
                .vendorDispatchBatchNumber(dispatchBatch.getVendorDispatchBatchNumber())
                .dispatchedAt(dispatchBatch.getDispatchedAt())
                .totalDispatchedPieces(dispatchedPieces)
                .totalDispatchedQuantity(dispatchedQuantity)
                .isInPieces(processedItem != null ? processedItem.getIsInPieces() : true)
                .totalReceivedPieces(totalReceivedPieces)
                .totalRejectedPieces(totalRejectedPieces)
                .totalTenantRejects(totalTenantRejects)
                .remainingPiecesAtVendor(remainingPieces)
                .remainingQuantityAtVendor(remainingQuantity)
                .dispatchHeats(dispatchHeats)
                .totalReceiveBatches(receiveBatches.size())
                .lastReceivedAt(lastReceivedAt)
                .fullyReceived(dispatchedPieces > 0 && totalReceivedPieces >= dispatchedPieces)
                .hasQualityCheckPending(hasQualityCheckPending)
                .allQualityChecksCompleted(allQualityChecksCompleted)
                .build();
    }

    /**
     * Convert VendorDispatchHeat to representation
     */
    private CalculatedVendorInventoryRepresentation.VendorDispatchHeatRepresentation convertDispatchHeatToRepresentation(VendorDispatchHeat dispatchHeat) {
        return CalculatedVendorInventoryRepresentation.VendorDispatchHeatRepresentation.builder()
                .heatId(dispatchHeat.getHeat() != null ? dispatchHeat.getHeat().getId() : null)
                .heatNumber(dispatchHeat.getHeat() != null ? dispatchHeat.getHeat().getHeatNumber() : null)
                .consumptionType(dispatchHeat.getConsumptionType() != null ? dispatchHeat.getConsumptionType().toString() : null)
                .quantityUsed(dispatchHeat.getQuantityUsed())
                .piecesUsed(dispatchHeat.getPiecesUsed())
                .testCertificateNumber(dispatchHeat.getHeat() != null ? dispatchHeat.getHeat().getTestCertificateNumber() : null)
                .createdAt(dispatchHeat.getCreatedAt())
                .build();
    }

    /**
     * Check if calculated inventory representation has remaining inventory
     */
    private boolean hasRemainingInventory(CalculatedVendorInventoryRepresentation rep) {
        return (rep.getRemainingPiecesAtVendor() != null && rep.getRemainingPiecesAtVendor() > 0) ||
               (rep.getRemainingQuantityAtVendor() != null && rep.getRemainingQuantityAtVendor() > 0);
    }

    /**
     * Transfer material from tenant Heat inventory to VendorInventory
     * This is a separate operation from VendorDispatchBatch creation
     */
    @Transactional
    public VendorInventory transferMaterialToVendor(Long vendorId, Long heatId, Double quantity, Integer pieces) {
        log.info("Transferring material to vendor {} from heat {}: quantity={}, pieces={}", 
                 vendorId, heatId, quantity, pieces);
        
        // Validate inputs
        if ((quantity == null || quantity <= 0) && (pieces == null || pieces <= 0)) {
            throw new IllegalArgumentException("Either quantity or pieces must be provided and greater than 0");
        }
        
        if (quantity != null && pieces != null) {
            throw new IllegalArgumentException("Only one of quantity or pieces should be provided, not both");
        }
        
        // Get vendor and heat
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));
        
        Heat heat = rawMaterialHeatService.getRawMaterialHeatById(heatId);
        
        // Validate tenant access
        if (!Objects.equals(vendor.getTenant().getId(), heat.getRawMaterialProduct().getProduct().getTenant().getId())) {
            throw new IllegalArgumentException("Vendor and heat must belong to the same tenant");
        }
        
        // Check if vendor already has inventory for this heat
        List<VendorInventory> existingInventories = getInventoryByVendorAndHeatNumber(vendorId, heat.getHeatNumber());
        
        if (!existingInventories.isEmpty()) {
            // Update existing inventory
            VendorInventory existingInventory = existingInventories.get(0);
            
            if (quantity != null) {
                // Validate heat has sufficient quantity
                if (heat.getAvailableQuantity() < quantity) {
                    throw new IllegalArgumentException("Insufficient quantity in heat. Available: " + 
                                                     heat.getAvailableQuantity() + ", Required: " + quantity);
                }
                
                // Transfer quantity
                heat.setAvailableHeatQuantity(heat.getAvailableHeatQuantity() - quantity);
                existingInventory.setTotalDispatchedQuantity(existingInventory.getTotalDispatchedQuantity() + quantity);
                existingInventory.setAvailableQuantity(existingInventory.getAvailableQuantity() + quantity);
                
            } else {
                // Validate heat has sufficient pieces
                if (heat.getAvailablePiecesCount() < pieces) {
                    throw new IllegalArgumentException("Insufficient pieces in heat. Available: " + 
                                                     heat.getAvailablePiecesCount() + ", Required: " + pieces);
                }
                
                // Transfer pieces
                heat.setAvailablePiecesCount(heat.getAvailablePiecesCount() - pieces);
                existingInventory.setTotalDispatchedPieces(existingInventory.getTotalDispatchedPieces() + pieces);
                existingInventory.setAvailablePiecesCount(existingInventory.getAvailablePiecesCount() + pieces);
            }
            
            existingInventory.setUpdatedAt(LocalDateTime.now());
            VendorInventory saved = vendorInventoryRepository.save(existingInventory);
            
            log.info("Updated existing vendor inventory {} with additional material", existingInventory.getId());
            return saved;
            
        } else {
            // Create new vendor inventory
            return transferToVendorInventory(vendor, heat, quantity, pieces);
        }
    }
    
    /**
     * Return material from VendorInventory back to tenant Heat inventory
     * This is a separate operation from VendorDispatchBatch deletion
     */
    @Transactional
    public boolean returnMaterialFromVendor(Long vendorInventoryId, Double quantityToReturn, Integer piecesToReturn) {
        log.info("Returning material from vendor inventory {} to tenant: quantity={}, pieces={}", 
                 vendorInventoryId, quantityToReturn, piecesToReturn);
        
        // Validate inputs
        if ((quantityToReturn == null || quantityToReturn <= 0) && (piecesToReturn == null || piecesToReturn <= 0)) {
            throw new IllegalArgumentException("Either quantity or pieces must be provided and greater than 0");
        }
        
        if (quantityToReturn != null && piecesToReturn != null) {
            throw new IllegalArgumentException("Only one of quantity or pieces should be provided, not both");
        }
        
        VendorInventory vendorInventory = vendorInventoryRepository.findByIdAndDeletedFalse(vendorInventoryId)
                .orElseThrow(() -> new VendorInventoryNotFoundException(vendorInventoryId));
        
        Heat originalHeat = vendorInventory.getOriginalHeat();
        
        if (quantityToReturn != null) {
            // Validate vendor has sufficient quantity
            if (vendorInventory.getAvailableQuantity() < quantityToReturn) {
                throw new IllegalArgumentException("Insufficient quantity in vendor inventory. Available: " + 
                                                 vendorInventory.getAvailableQuantity() + ", Requested: " + quantityToReturn);
            }
            
            // Return quantity to heat
            originalHeat.setAvailableHeatQuantity(originalHeat.getAvailableHeatQuantity() + quantityToReturn);
            vendorInventory.setAvailableQuantity(vendorInventory.getAvailableQuantity() - quantityToReturn);
            
            log.info("Returned {} KG from vendor inventory {} to heat {}", 
                     quantityToReturn, vendorInventoryId, originalHeat.getId());
            
        } else {
            // Validate vendor has sufficient pieces
            if (vendorInventory.getAvailablePiecesCount() < piecesToReturn) {
                throw new IllegalArgumentException("Insufficient pieces in vendor inventory. Available: " + 
                                                 vendorInventory.getAvailablePiecesCount() + ", Requested: " + piecesToReturn);
            }
            
            // Return pieces to heat
            originalHeat.setAvailablePiecesCount(originalHeat.getAvailablePiecesCount() + piecesToReturn);
            vendorInventory.setAvailablePiecesCount(vendorInventory.getAvailablePiecesCount() - piecesToReturn);
            
            log.info("Returned {} pieces from vendor inventory {} to heat {}", 
                     piecesToReturn, vendorInventoryId, originalHeat.getId());
        }
        
        // If vendor inventory is empty, mark it as completed
        if (vendorInventory.getAvailableQuantity() <= 0 && vendorInventory.getAvailablePiecesCount() <= 0) {
            vendorInventory.setDeleted(true);
            vendorInventory.setDeletedAt(LocalDateTime.now());
            log.info("Vendor inventory {} is now empty and marked as completed", vendorInventoryId);
        }
        
        vendorInventory.setUpdatedAt(LocalDateTime.now());
        vendorInventoryRepository.save(vendorInventory);
        
        return true;
    }
    
    /**
     * Consume material from VendorInventory for VendorDispatchBatch
     * This replaces the direct Heat consumption in VendorDispatchBatch creation
     */
    @Transactional
    public boolean consumeFromVendorInventory(Long vendorId, String heatNumber, 
                                            Double quantityToConsume, Integer piecesToConsume) {
        log.info("Consuming from vendor {} inventory for heat {}: quantity={}, pieces={}", 
                 vendorId, heatNumber, quantityToConsume, piecesToConsume);
        
        // Validate inputs
        if ((quantityToConsume == null || quantityToConsume <= 0) && (piecesToConsume == null || piecesToConsume <= 0)) {
            throw new IllegalArgumentException("Either quantity or pieces must be provided and greater than 0");
        }
        
        if (quantityToConsume != null && piecesToConsume != null) {
            throw new IllegalArgumentException("Only one of quantity or pieces should be provided, not both");
        }
        
        // Find vendor inventory for this heat
        List<VendorInventory> vendorInventories = getInventoryByVendorAndHeatNumber(vendorId, heatNumber);
        
        if (vendorInventories.isEmpty()) {
            throw new IllegalArgumentException("No vendor inventory found for vendor " + vendorId + " and heat " + heatNumber);
        }
        
        VendorInventory vendorInventory = vendorInventories.get(0); // Get the first one
        
        if (quantityToConsume != null) {
            // Validate vendor has sufficient quantity
            if (vendorInventory.getAvailableQuantity() < quantityToConsume) {
                throw new IllegalArgumentException("Insufficient quantity in vendor inventory for heat " + heatNumber + 
                                                 ". Available: " + vendorInventory.getAvailableQuantity() + 
                                                 ", Required: " + quantityToConsume);
            }
            
            // Consume quantity from vendor inventory
            vendorInventory.setAvailableQuantity(vendorInventory.getAvailableQuantity() - quantityToConsume);
            
            log.info("Consumed {} KG from vendor {} inventory for heat {}", 
                     quantityToConsume, vendorId, heatNumber);
            
        } else {
            // Validate vendor has sufficient pieces
            if (vendorInventory.getAvailablePiecesCount() < piecesToConsume) {
                throw new IllegalArgumentException("Insufficient pieces in vendor inventory for heat " + heatNumber + 
                                                 ". Available: " + vendorInventory.getAvailablePiecesCount() + 
                                                 ", Required: " + piecesToConsume);
            }
            
            // Consume pieces from vendor inventory
            vendorInventory.setAvailablePiecesCount(vendorInventory.getAvailablePiecesCount() - piecesToConsume);
            
            log.info("Consumed {} pieces from vendor {} inventory for heat {}", 
                     piecesToConsume, vendorId, heatNumber);
        }
        
        vendorInventory.setUpdatedAt(LocalDateTime.now());
        vendorInventoryRepository.save(vendorInventory);
        
        return true;
    }

    /**
     * Gets all available vendor inventory for a specific vendor
     */
    @Transactional(readOnly = true)
    public List<VendorInventory> getAvailableInventoryByVendor(Long vendorId) {
        return vendorInventoryRepository.findAvailableInventoryByVendorId(vendorId);
    }

    /**
     * Gets paginated available vendor inventory for a specific vendor
     */
    @Transactional(readOnly = true)
    public Page<VendorInventory> getAvailableInventoryByVendor(Long vendorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return vendorInventoryRepository.findAvailableInventoryByVendorId(vendorId, pageable);
    }


    /**
     * Gets vendor inventory by heat number for a specific vendor
     */
    @Transactional(readOnly = true)
    public List<VendorInventory> getInventoryByVendorAndHeatNumber(Long vendorId, String heatNumber) {
        return vendorInventoryRepository.findByVendorIdAndHeatNumberAndDeletedFalse(vendorId, heatNumber);
    }

    /**
     * Gets paginated vendor inventory for a specific vendor
     */
    @Transactional(readOnly = true)
    public Page<VendorInventory> getVendorInventory(Long vendorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vendorInventoryRepository.findByVendorIdAndDeletedFalse(vendorId, pageable);
    }

    /**
     * Gets vendor inventory by ID
     */
    @Transactional(readOnly = true)
    public VendorInventory getVendorInventoryById(Long vendorInventoryId) {
        return vendorInventoryRepository.findByIdAndDeletedFalse(vendorInventoryId)
                .orElseThrow(() -> new VendorInventoryNotFoundException("Vendor inventory not found with id: " + vendorInventoryId));
    }

    /**
     * Gets all vendor inventory for a specific vendor (including those without available quantity)
     */
    @Transactional(readOnly = true)
    public List<VendorInventory> getAllInventoryByVendor(Long vendorId) {
        return vendorInventoryRepository.findByVendorIdAndDeletedFalseOrderByCreatedAtDesc(vendorId);
    }
} 