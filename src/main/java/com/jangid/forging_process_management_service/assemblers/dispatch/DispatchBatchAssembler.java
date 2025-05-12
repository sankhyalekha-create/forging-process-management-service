package com.jangid.forging_process_management_service.assemblers.dispatch;

import com.jangid.forging_process_management_service.assemblers.buyer.BuyerAssembler;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.service.dispatch.ProcessedItemDispatchBatchService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DispatchBatchAssembler {

  @Autowired
  private ProcessedItemDispatchBatchService processedItemDispatchBatchService;

  @Autowired
  private DispatchProcessedItemInspectionAssembler dispatchProcessedItemInspectionAssembler;
  @Autowired
  private ProcessedItemDispatchBatchAssembler processedItemDispatchBatchAssembler;
  @Autowired
  private BuyerAssembler buyerAssembler;
  @Autowired
  private DispatchPackageAssembler dispatchPackageAssembler;

  /**
   * Converts DispatchBatch to DispatchBatchRepresentation.
   */
  public DispatchBatchRepresentation dissemble(DispatchBatch dispatchBatch) {
    return DispatchBatchRepresentation.builder()
        .id(dispatchBatch.getId())
        .dispatchBatchNumber(dispatchBatch.getDispatchBatchNumber())
        .dispatchProcessedItemInspections(dispatchBatch.getDispatchProcessedItemInspections() != null
                                        ? dispatchBatch.getDispatchProcessedItemInspections().stream()
                                            .map(dispatchProcessedItemInspectionAssembler::dissemble)
                                            .collect(Collectors.toList())
                                        : new ArrayList<>())
        .processedItemDispatchBatch(dispatchBatch.getProcessedItemDispatchBatch() != null
                                    ? processedItemDispatchBatchAssembler.dissemble(dispatchBatch.getProcessedItemDispatchBatch())
                                    : null)
        .dispatchPackages(dispatchBatch.getDispatchPackages() != null
                         ? dispatchBatch.getDispatchPackages().stream()
                             .map(dispatchPackageAssembler::dissemble)
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
        .build();
  }

  /**
   * Converts DispatchBatchRepresentation to DispatchBatch.
   */
  public DispatchBatch assemble(DispatchBatchRepresentation dispatchBatchRepresentation) {
    if (dispatchBatchRepresentation != null) {
      ProcessedItemDispatchBatch processedItemDispatchBatch = null;
      if (dispatchBatchRepresentation.getProcessedItemDispatchBatch() != null
          && dispatchBatchRepresentation.getProcessedItemDispatchBatch().getId() != null) {
        processedItemDispatchBatch = processedItemDispatchBatchService.getProcessedItemDispatchBatchById(
            dispatchBatchRepresentation.getProcessedItemDispatchBatch().getId());
      }
      DispatchBatch dispatchBatch = DispatchBatch.builder()
          .id(dispatchBatchRepresentation.getId())
          .dispatchBatchNumber(dispatchBatchRepresentation.getDispatchBatchNumber())
          .dispatchProcessedItemInspections(dispatchBatchRepresentation.getDispatchProcessedItemInspections() != null
                                          ? dispatchBatchRepresentation.getDispatchProcessedItemInspections().stream()
                                              .map(dispatchProcessedItemInspectionAssembler::assemble)
                                              .collect(Collectors.toList())
                                          : new ArrayList<>())
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
                        ? DispatchBatch.PackagingType.valueOf(dispatchBatchRepresentation.getPackagingType())
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
    dispatchBatch.getDispatchProcessedItemInspections().forEach(dispatchProcessedItemInspection -> dispatchProcessedItemInspection.setCreatedAt(LocalDateTime.now()));
    
    if (dispatchBatch.getDispatchPackages() != null) {
      dispatchBatch.getDispatchPackages().forEach(dispatchPackage -> dispatchPackage.setCreatedAt(LocalDateTime.now()));
    }
    
    return dispatchBatch;
  }
}

