package com.jangid.forging_process_management_service.assemblers.dispatch;

import com.jangid.forging_process_management_service.assemblers.buyer.BuyerAssembler;
import com.jangid.forging_process_management_service.entities.PackagingType;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.service.dispatch.ProcessedItemDispatchBatchService;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DispatchBatchAssembler {

  @Autowired
  private ProcessedItemDispatchBatchService processedItemDispatchBatchService;

  @Autowired
  private ProcessedItemDispatchBatchAssembler processedItemDispatchBatchAssembler;
  @Autowired
  private BuyerAssembler buyerAssembler;
  @Autowired
  private DispatchPackageAssembler dispatchPackageAssembler;
  @Autowired
  private DispatchProcessedItemConsumptionAssembler dispatchProcessedItemConsumptionAssembler;
  @Autowired
  @Lazy
  private ItemWorkflowService itemWorkflowService;


  /**
   * Converts DispatchBatch to DispatchBatchRepresentation.
   */
  @Transactional(readOnly = true)
  public DispatchBatchRepresentation dissemble(DispatchBatch dispatchBatch) {
    // Extract order association information if available via ItemWorkflow
    Long orderId = null;
    String orderPoNumber = null;
    String orderDate = null;
    boolean isOrderBased = false;

    try {
      // Get itemWorkflowId from ProcessedItemDispatchBatch
      if (dispatchBatch.getProcessedItemDispatchBatch() != null && 
          dispatchBatch.getProcessedItemDispatchBatch().getItemWorkflowId() != null) {
        Long itemWorkflowId = dispatchBatch.getProcessedItemDispatchBatch().getItemWorkflowId();
        
        // Fetch ItemWorkflow to get order information
        ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
        
        if (itemWorkflow != null && itemWorkflow.getOrderItemWorkflows() != null && 
            !itemWorkflow.getOrderItemWorkflows().isEmpty()) {
          // Get the first orderItemWorkflow to extract order info
          var orderItemWorkflow = itemWorkflow.getOrderItemWorkflows().stream()
              .findFirst()
              .orElse(null);

          if (orderItemWorkflow.getOrderItem() != null) {
            var order = orderItemWorkflow.getOrderItem().getOrder();
            if (order != null) {
              orderId = order.getId();
              orderPoNumber = order.getPoNumber();
              orderDate = order.getOrderDate() != null ? order.getOrderDate().toString() : null;
              isOrderBased = true;
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Failed to extract order information for dispatch batch ID: {}. Error: {}",
               dispatchBatch.getId(), e.getMessage());
      throw e;
    }

    return DispatchBatchRepresentation.builder()
        .id(dispatchBatch.getId())
        .dispatchBatchNumber(dispatchBatch.getDispatchBatchNumber())
        .processedItemDispatchBatch(dispatchBatch.getProcessedItemDispatchBatch() != null
                                    ? processedItemDispatchBatchAssembler.dissemble(dispatchBatch.getProcessedItemDispatchBatch())
                                    : null)
        .dispatchPackages(dispatchBatch.getDispatchPackages() != null
                         ? dispatchBatch.getDispatchPackages().stream()
                             .map(dispatchPackageAssembler::dissemble)
                             .collect(Collectors.toList())
                         : new ArrayList<>())
        .dispatchProcessedItemConsumptions(dispatchBatch.getDispatchProcessedItemConsumptions() != null
                         ? dispatchBatch.getDispatchProcessedItemConsumptions().stream()
                             .map(dispatchProcessedItemConsumptionAssembler::dissemble)
                             .collect(Collectors.toList())
                         : new ArrayList<>())
        .dispatchBatchStatus(dispatchBatch.getDispatchBatchStatus() != null
                             ? dispatchBatch.getDispatchBatchStatus().name()
                             : null)
        .dispatchCreatedAt(dispatchBatch.getDispatchCreatedAt() != null ? dispatchBatch.getDispatchCreatedAt().toString() : null)
        .dispatchReadyAt(dispatchBatch.getDispatchReadyAt() != null ? dispatchBatch.getDispatchReadyAt().toString() : null)
        .dispatchedAt(dispatchBatch.getDispatchedAt() != null ? dispatchBatch.getDispatchedAt().toString() : null)
        .invoiceNumber(dispatchBatch.getInvoiceNumber())
        .invoiceDateTime(dispatchBatch.getInvoiceDateTime() != null ? dispatchBatch.getInvoiceDateTime().toString() : null)
        .purchaseOrderNumber(dispatchBatch.getPurchaseOrderNumber())
        .purchaseOrderDateTime(dispatchBatch.getPurchaseOrderDateTime() != null ? dispatchBatch.getPurchaseOrderDateTime().toString() : null)
        .packagingType(dispatchBatch.getPackagingType() != null ? dispatchBatch.getPackagingType().name() : null)
        .packagingQuantity(dispatchBatch.getPackagingQuantity())
        .perPackagingQuantity(dispatchBatch.getPerPackagingQuantity())
        .useUniformPackaging(dispatchBatch.getUseUniformPackaging())
        .buyer(buyerAssembler.dissemble(dispatchBatch.getBuyer()))
        .buyerId(dispatchBatch.getBuyer() != null ? dispatchBatch.getBuyer().getId():null)
        .billingEntityId(dispatchBatch.getBillingEntity() != null ? dispatchBatch.getBillingEntity().getId() : null)
        .shippingEntityId(dispatchBatch.getShippingEntity() != null ? dispatchBatch.getShippingEntity().getId() : null)
        .tenantId(dispatchBatch.getTenant().getId())
        .orderId(orderId)
        .orderPoNumber(orderPoNumber)
        .orderDate(orderDate)
        .isOrderBased(isOrderBased)
        .build();
  }

  /**
   * Converts DispatchBatchRepresentation to DispatchBatch.
   */
  public DispatchBatch assemble(DispatchBatchRepresentation dispatchBatchRepresentation) {
    if (dispatchBatchRepresentation != null) {
      ProcessedItemDispatchBatch processedItemDispatchBatch = null;
      if (dispatchBatchRepresentation.getProcessedItemDispatchBatch() != null) {
        if (dispatchBatchRepresentation.getProcessedItemDispatchBatch().getId() != null) {
          processedItemDispatchBatch = processedItemDispatchBatchService.getProcessedItemDispatchBatchById(
              dispatchBatchRepresentation.getProcessedItemDispatchBatch().getId());
        } else {
          processedItemDispatchBatch = processedItemDispatchBatchAssembler.assemble(dispatchBatchRepresentation.getProcessedItemDispatchBatch());
        }

      }

      DispatchBatch dispatchBatch = DispatchBatch.builder()
          .id(dispatchBatchRepresentation.getId())
          .dispatchBatchNumber(dispatchBatchRepresentation.getDispatchBatchNumber())
          .processedItemDispatchBatch(processedItemDispatchBatch)
          .dispatchBatchStatus(dispatchBatchRepresentation.getDispatchBatchStatus() != null
                               ? DispatchBatch.DispatchBatchStatus.valueOf(dispatchBatchRepresentation.getDispatchBatchStatus())
                               : null)
          .dispatchCreatedAt(dispatchBatchRepresentation.getDispatchCreatedAt() != null
                           ? LocalDateTime.parse(dispatchBatchRepresentation.getDispatchCreatedAt())
                           : null)
          .dispatchReadyAt(dispatchBatchRepresentation.getDispatchReadyAt() != null
                           ? LocalDateTime.parse(dispatchBatchRepresentation.getDispatchReadyAt())
                           : null)
          .dispatchedAt(dispatchBatchRepresentation.getDispatchedAt() != null
                        ? LocalDateTime.parse(dispatchBatchRepresentation.getDispatchedAt())
                        : null)
          .invoiceNumber(dispatchBatchRepresentation.getInvoiceNumber())
          .invoiceDateTime(dispatchBatchRepresentation.getInvoiceDateTime() != null
                          ? LocalDateTime.parse(dispatchBatchRepresentation.getInvoiceDateTime())
                          : null)
          .purchaseOrderNumber(dispatchBatchRepresentation.getPurchaseOrderNumber())
          .purchaseOrderDateTime(dispatchBatchRepresentation.getPurchaseOrderDateTime() != null
                                ? LocalDateTime.parse(dispatchBatchRepresentation.getPurchaseOrderDateTime())
                                : null)
          .packagingType(dispatchBatchRepresentation.getPackagingType() != null
                        ? PackagingType.valueOf(dispatchBatchRepresentation.getPackagingType())
                        : null)
          .packagingQuantity(dispatchBatchRepresentation.getPackagingQuantity())
          .perPackagingQuantity(dispatchBatchRepresentation.getPerPackagingQuantity())
          .useUniformPackaging(dispatchBatchRepresentation.getUseUniformPackaging())
          .build();
          
      if (dispatchBatchRepresentation.getDispatchPackages() != null && !dispatchBatchRepresentation.getDispatchPackages().isEmpty()) {
        dispatchBatch.setDispatchPackages(
            dispatchBatchRepresentation.getDispatchPackages().stream()
                .map(packageRep -> dispatchPackageAssembler.assemble(packageRep, dispatchBatch))
                .collect(Collectors.toList())
        );
      }

      // NOTE: DispatchProcessedItemConsumption entities are handled separately in DispatchBatchService
      // to avoid duplicate creation. The assembler should not create them during initial assembly.
      // They will be created in handleMultipleParentOperationsConsumption() method.
          
      return dispatchBatch;
    }
    return null;
  }

  /**
   * Creates a new DispatchBatch from a representation with default settings.
   */
  public DispatchBatch createAssemble(DispatchBatchRepresentation dispatchBatchRepresentation) {
    DispatchBatch dispatchBatch = assemble(dispatchBatchRepresentation);
    dispatchBatch.setCreatedAt(LocalDateTime.now());
    dispatchBatch.getProcessedItemDispatchBatch().setDispatchBatch(dispatchBatch);
    dispatchBatch.getProcessedItemDispatchBatch().setCreatedAt(LocalDateTime.now());

    if (dispatchBatch.getDispatchPackages() != null) {
      dispatchBatch.getDispatchPackages().forEach(dispatchPackage -> dispatchPackage.setCreatedAt(LocalDateTime.now()));
    }

    // NOTE: DispatchProcessedItemConsumption entities are handled in DispatchBatchService
    // No need to set createdAt here as they're created separately
    
    return dispatchBatch;
  }
}

