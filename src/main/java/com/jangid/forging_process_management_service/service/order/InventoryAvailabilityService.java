package com.jangid.forging_process_management_service.service.order;

import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.order.WorkType;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemProduct;
import com.jangid.forging_process_management_service.entities.product.Product;
import com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderItemRepresentation;
import com.jangid.forging_process_management_service.repositories.product.ItemRepository;
import com.jangid.forging_process_management_service.repositories.inventory.HeatRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class InventoryAvailabilityService {

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private HeatRepository heatRepository;

  /**
   * Check inventory availability for order items
   * Returns a simple map with shortage information
   *
   * @param tenantId The tenant ID
   * @param orderItems List of order items to check
   * @return Map containing hasShortage flag and list of shortages
   */
  @Transactional(readOnly = true)
  public Map<String, Object> checkInventoryForOrderItems(
    Long tenantId,
    List<OrderItemRepresentation> orderItems
  ) {
    Map<String, Object> result = new HashMap<>();
    List<Map<String, Object>> shortages = new ArrayList<>();
    List<Map<String, Object>> availableInventory = new ArrayList<>();
    boolean hasShortage = false;

    log.info("Checking inventory availability for {} order items for tenant {}", orderItems.size(), tenantId);

    for (OrderItemRepresentation orderItem : orderItems) {
      // Note: We check inventory for both WITH_MATERIAL and JOB_WORK_ONLY
      // For JOB_WORK_ONLY, customer provides material but we still need to receive it in our inventory
      // The difference is only in costing, not in material requirement
      
      // Get the item with its product composition
      Item item = itemRepository.findByIdAndTenantIdAndDeletedFalse(
        orderItem.getItemId(), tenantId
      ).orElse(null);

      if (item == null) {
        log.warn("Item not found for ID: {} and tenant: {}", orderItem.getItemId(), tenantId);
        continue;
      }

      if (item.getItemProducts() == null || item.getItemProducts().isEmpty()) {
        log.debug("Item {} has no product composition, skipping inventory check", item.getItemName());
        continue;
      }

      // Check each product requirement
      for (ItemProduct itemProduct : item.getItemProducts()) {
        Product product = itemProduct.getProduct();

        // Calculate required quantity based on order quantity
        // For simplicity: assume 1 unit of product is needed per item
        // In real production, this might be itemWeight or other ratios
        Double required = calculateRequiredQuantity(item, orderItem.getQuantity());

        // Get available quantity from heats (uses appropriate method based on unit of measurement)
        Double available = getAvailableStock(tenantId, product);

        log.debug("Product: {}, UoM: {}, Required: {}, Available: {}", 
          product.getProductName(), product.getUnitOfMeasurement(), required, available);

        // Determine work type for better context
        String workType = orderItem.getWorkType();
        boolean isJobWorkOnly = WorkType.JOB_WORK_ONLY.name().equals(workType);

        // Always track inventory info (for both shortage and available cases)
        Map<String, Object> inventoryInfo = new HashMap<>();
        inventoryInfo.put("productId", product.getId());
        inventoryInfo.put("productName", product.getProductName());
        inventoryInfo.put("productCode", product.getProductCode());
        inventoryInfo.put("itemName", item.getItemName());
        inventoryInfo.put("itemCode", item.getItemCode());
        inventoryInfo.put("requiredQuantity", required);
        inventoryInfo.put("availableQuantity", available);
        inventoryInfo.put("unit", product.getUnitOfMeasurement().name());
        inventoryInfo.put("workType", workType);
        inventoryInfo.put("isJobWorkOnly", isJobWorkOnly);

        // Check for shortage
        if (available < required) {
          hasShortage = true;
          double shortage = required - available;
          
          inventoryInfo.put("shortageQuantity", shortage);
          inventoryInfo.put("status", "SHORTAGE");
          shortages.add(inventoryInfo);

          log.warn("Inventory shortage detected - Product: {}, Required: {}, Available: {}, Shortage: {}, WorkType: {}",
            product.getProductName(), required, available, shortage, workType);
        } else {
          inventoryInfo.put("status", "AVAILABLE");
          inventoryInfo.put("surplusQuantity", available - required);
          availableInventory.add(inventoryInfo);
          
          log.debug("Inventory sufficient - Product: {}, Required: {}, Available: {}, Surplus: {}",
            product.getProductName(), required, available, (available - required));
        }
      }
    }

    result.put("hasShortage", hasShortage);
    result.put("shortages", shortages);
    result.put("availableInventory", availableInventory);
    result.put("totalItemsChecked", orderItems.size());
    result.put("checkedAt", LocalDateTime.now());

    log.info("Inventory check complete. Has shortage: {}, Total shortages: {}", 
      hasShortage, shortages.size());

    return result;
  }

  /**
   * Calculate required product quantity based on item and order quantity
   * Simplified: Uses item weight for KGS products, item count for PIECES
   *
   * @param item The item being ordered
   * @param orderQuantity The order quantity
   * @return Required quantity of the product
   */
  private Double calculateRequiredQuantity(Item item, Integer orderQuantity) {
    // Use item weight if available, otherwise default to order quantity
    if (item.getItemWeight() != null && item.getItemWeight() > 0) {
      return item.getItemWeight() * orderQuantity;
    } else if (item.getItemCount() != null && item.getItemCount() > 0) {
      return (double) (item.getItemCount() * orderQuantity);
    }
    // Fallback: 1:1 ratio
    return (double) orderQuantity;
  }

  /**
   * Get available stock from heats for a given product
   * Uses appropriate HeatRepository query based on unit of measurement:
   * - For KGS: Uses findHeatsHavingQuantitiesByProductIdAndTenantId (checks availableHeatQuantity)
   * - For PIECES: Uses findHeatsHavingPiecesByProductIdAndTenantId (checks availablePiecesCount)
   *
   * @param tenantId The tenant ID
   * @param product The product entity (contains UoM information)
   * @return Total available quantity across all heats
   */
  private Double getAvailableStock(Long tenantId, Product product) {
    List<Heat> availableHeats;
    
    // Use appropriate repository method based on unit of measurement
    if (product.getUnitOfMeasurement() == UnitOfMeasurement.PIECES) {
      // For PIECES: Check availablePiecesCount > 0
      availableHeats = heatRepository
        .findHeatsHavingPiecesByProductIdAndTenantId(product.getId(), tenantId);
      
      log.debug("Product {} (PIECES): Checking heats with available pieces", product.getProductName());
    } else {
      // For KGS and other weight-based measurements: Check availableHeatQuantity > 0
      availableHeats = heatRepository
        .findHeatsHavingQuantitiesByProductIdAndTenantId(product.getId(), tenantId);
      
      log.debug("Product {} ({}): Checking heats with available quantity", 
        product.getProductName(), product.getUnitOfMeasurement());
    }

    // Sum available quantities from all heats using Heat's getAvailableQuantity() method
    // This method automatically returns the correct value based on isInPieces flag
    double totalAvailable = availableHeats.stream()
      .mapToDouble(Heat::getAvailableQuantity)
      .sum();

    log.debug("Product {} ({}): Found {} heats, total available: {}", 
      product.getProductName(), product.getUnitOfMeasurement(), 
      availableHeats.size(), totalAvailable);

    return totalAvailable;
  }
}
