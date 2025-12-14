package com.jangid.forging_process_management_service.service.order;

import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.order.WorkType;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemProduct;
import com.jangid.forging_process_management_service.entities.product.Product;
import com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderItemWorkflowRepresentation;
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
   * Check inventory availability for a single workflow
   * This is the primary method to use when adding workflows to order items
   *
   * @param tenantId The tenant ID
   * @param itemId The item ID
   * @param workflow The workflow representation containing quantity and workType
   * @return Map containing hasShortage flag and list of shortages
   */
  @Transactional(readOnly = true)
  public Map<String, Object> checkInventoryForWorkflow(
    Long tenantId,
    Long itemId,
    OrderItemWorkflowRepresentation workflow
  ) {
    Map<String, Object> result = new HashMap<>();
    List<Map<String, Object>> shortages = new ArrayList<>();
    List<Map<String, Object>> availableInventory = new ArrayList<>();
    boolean hasShortage = false;

    log.info("Checking inventory availability for workflow - Item ID: {}, Quantity: {}, WorkType: {}", 
      itemId, workflow.getQuantity(), workflow.getWorkType());

    // Get the item with its product composition
    Item item = itemRepository.findByIdAndTenantIdAndDeletedFalse(itemId, tenantId).orElse(null);

    if (item == null) {
      log.warn("Item not found for ID: {} and tenant: {}", itemId, tenantId);
      result.put("hasShortage", false);
      result.put("shortages", shortages);
      result.put("availableInventory", availableInventory);
      result.put("error", "Item not found");
      return result;
    }

    if (item.getItemProducts() == null || item.getItemProducts().isEmpty()) {
      log.debug("Item {} has no product composition, skipping inventory check", item.getItemName());
      result.put("hasShortage", false);
      result.put("shortages", shortages);
      result.put("availableInventory", availableInventory);
      result.put("note", "No product composition defined");
      return result;
    }

    // Check each product requirement
    for (ItemProduct itemProduct : item.getItemProducts()) {
      Product product = itemProduct.getProduct();

      // Calculate required quantity based on workflow quantity
      Double required = calculateRequiredQuantity(item, workflow.getQuantity());

      // Get available quantity from heats
      Double available = getAvailableStock(tenantId, product);

      log.debug("Product: {}, UoM: {}, Required: {}, Available: {}", 
        product.getProductName(), product.getUnitOfMeasurement(), required, available);

      // Determine work type for better context
      String workType = workflow.getWorkType();
      boolean isJobWorkOnly = WorkType.JOB_WORK_ONLY.name().equals(workType);

      // Always track inventory info
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
      inventoryInfo.put("workflowQuantity", workflow.getQuantity());

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

    result.put("hasShortage", hasShortage);
    result.put("shortages", shortages);
    result.put("availableInventory", availableInventory);
    result.put("itemId", itemId);
    result.put("workflowQuantity", workflow.getQuantity());
    result.put("workType", workflow.getWorkType());
    result.put("checkedAt", LocalDateTime.now());

    log.info("Workflow inventory check complete. Has shortage: {}, Total shortages: {}", 
      hasShortage, shortages.size());

    return result;
  }

  /**
   * Check inventory availability for order items (iterates through all workflows)
   * Returns a simple map with shortage information
   *
   * @param tenantId The tenant ID
   * @param orderItems List of order items to check (each containing workflows)
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
    int totalWorkflowsChecked = 0;

    log.info("Checking inventory availability for {} order items for tenant {}", orderItems.size(), tenantId);

    for (OrderItemRepresentation orderItem : orderItems) {
      // With new structure, quantity/workType are at workflow level
      if (orderItem.getOrderItemWorkflows() == null || orderItem.getOrderItemWorkflows().isEmpty()) {
        log.debug("OrderItem {} has no workflows, skipping inventory check", orderItem.getItemId());
        continue;
      }

      // Check inventory for each workflow
      for (OrderItemWorkflowRepresentation workflow : orderItem.getOrderItemWorkflows()) {
        totalWorkflowsChecked++;
        
        Map<String, Object> workflowCheck = checkInventoryForWorkflow(
          tenantId, 
          orderItem.getItemId(), 
          workflow
        );

        // Merge results
        Boolean workflowHasShortage = (Boolean) workflowCheck.get("hasShortage");
        if (workflowHasShortage != null && workflowHasShortage) {
          hasShortage = true;
          @SuppressWarnings("unchecked")
          List<Map<String, Object>> workflowShortages = 
            (List<Map<String, Object>>) workflowCheck.get("shortages");
          if (workflowShortages != null) {
            shortages.addAll(workflowShortages);
          }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workflowAvailable = 
          (List<Map<String, Object>>) workflowCheck.get("availableInventory");
        if (workflowAvailable != null) {
          availableInventory.addAll(workflowAvailable);
        }
      }
    }

    result.put("hasShortage", hasShortage);
    result.put("shortages", shortages);
    result.put("availableInventory", availableInventory);
    result.put("totalItemsChecked", orderItems.size());
    result.put("totalWorkflowsChecked", totalWorkflowsChecked);
    result.put("checkedAt", LocalDateTime.now());

    log.info("Inventory check complete. Has shortage: {}, Total shortages: {}, Total workflows checked: {}", 
      hasShortage, shortages.size(), totalWorkflowsChecked);

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
