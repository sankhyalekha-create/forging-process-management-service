package com.jangid.forging_process_management_service.service.vendor;

import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.vendor.Vendor;
import com.jangid.forging_process_management_service.entities.vendor.VendorInventory;
import com.jangid.forging_process_management_service.exception.vendor.VendorInventoryNotFoundException;
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
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.vendor.VendorRepository;

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
     * Gets vendor inventory for a specific vendor and product
     */
    @Transactional(readOnly = true)
    public List<VendorInventory> getAvailableInventoryByVendorAndProduct(Long vendorId, Long productId) {
        return vendorInventoryRepository.findAvailableInventoryByVendorIdAndProductId(vendorId, productId);
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