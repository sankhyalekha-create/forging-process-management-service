package com.jangid.forging_process_management_service.repositories.vendor;

import com.jangid.forging_process_management_service.entities.vendor.VendorInventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorInventoryRepository extends CrudRepository<VendorInventory, Long> {

    Optional<VendorInventory> findByIdAndDeletedFalse(long id);

    /**
     * Find vendor inventory by vendor and original heat
     */
    Optional<VendorInventory> findByVendorIdAndOriginalHeatIdAndDeletedFalse(Long vendorId, Long originalHeatId);

    /**
     * Check if a heat (originalHeat) is transferred to any vendor inventory
     * 
     * @param originalHeatId The original heat ID to check
     * @return true if heat is transferred to vendor inventory, false otherwise
     */
    boolean existsByOriginalHeatIdAndDeletedFalse(Long originalHeatId);

    /**
     * Count how many times a heat is transferred to vendor inventory
     * 
     * @param originalHeatId The original heat ID to check
     * @return count of vendor inventory records
     */
    @Query("SELECT COUNT(vi) FROM vendor_inventory vi WHERE vi.originalHeat.id = :originalHeatId AND vi.deleted = false")
    Long countByOriginalHeatIdAndDeletedFalse(@Param("originalHeatId") Long originalHeatId);

    /**
     * Find all vendor inventory for a specific vendor
     */
    List<VendorInventory> findByVendorIdAndDeletedFalseOrderByCreatedAtDesc(Long vendorId);

    /**
     * Find vendor inventory with available quantity/pieces for a specific vendor
     */
    @Query("""
        SELECT vi
        FROM vendor_inventory vi
        WHERE vi.vendor.id = :vendorId
          AND vi.deleted = false
          AND (
            (vi.isInPieces = false AND vi.availableQuantity > 0) OR
            (vi.isInPieces = true AND vi.availablePiecesCount > 0)
          )
        ORDER BY vi.updatedAt DESC
    """)
    List<VendorInventory> findAvailableInventoryByVendorId(@Param("vendorId") Long vendorId);

    /**
     * Find vendor inventory with available quantity/pieces for a specific vendor (paginated)
     */
    @Query("""
        SELECT vi
        FROM vendor_inventory vi
        WHERE vi.vendor.id = :vendorId
          AND vi.deleted = false
          AND (
            (vi.isInPieces = false AND vi.availableQuantity > 0) OR
            (vi.isInPieces = true AND vi.availablePiecesCount > 0)
          )
        ORDER BY vi.updatedAt DESC
    """)
    Page<VendorInventory> findAvailableInventoryByVendorId(@Param("vendorId") Long vendorId, Pageable pageable);

    /**
     * Find vendor inventory by product for a specific vendor
     */
    @Query("""
        SELECT vi
        FROM vendor_inventory vi
        JOIN vi.rawMaterialProduct rmp
        JOIN rmp.product p
        WHERE vi.vendor.id = :vendorId
          AND p.id = :productId
          AND vi.deleted = false
          AND (
            (vi.isInPieces = false AND vi.availableQuantity > 0) OR
            (vi.isInPieces = true AND vi.availablePiecesCount > 0)
          )
        ORDER BY vi.createdAt ASC
    """)
    List<VendorInventory> findAvailableInventoryByVendorIdAndProductId(@Param("vendorId") Long vendorId, 
                                                                       @Param("productId") Long productId);

    /**
     * Find vendor inventory by heat number for a specific vendor
     */
    @Query("""
        SELECT vi
        FROM vendor_inventory vi
        WHERE vi.vendor.id = :vendorId
          AND vi.heatNumber = :heatNumber
          AND vi.deleted = false
        ORDER BY vi.createdAt DESC
    """)
    List<VendorInventory> findByVendorIdAndHeatNumberAndDeletedFalse(@Param("vendorId") Long vendorId, 
                                                                     @Param("heatNumber") String heatNumber);

    /**
     * Get paginated vendor inventory for a specific vendor
     */
    @Query("""
        SELECT vi
        FROM vendor_inventory vi
        WHERE vi.vendor.id = :vendorId
          AND vi.deleted = false
        ORDER BY vi.createdAt DESC
    """)
    Page<VendorInventory> findByVendorIdAndDeletedFalse(@Param("vendorId") Long vendorId, Pageable pageable);
} 