package com.jangid.forging_process_management_service.assemblers.vendor;

import com.jangid.forging_process_management_service.entities.PackagingType;
import com.jangid.forging_process_management_service.entities.forging.ItemWeightType;
import com.jangid.forging_process_management_service.entities.order.OrderItemWorkflow;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.vendor.VendorProcessType;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorReceiveBatchRepresentation;
import com.jangid.forging_process_management_service.repositories.order.OrderItemWorkflowRepository;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class VendorDispatchBatchAssembler {

    @Autowired
    @Lazy
    private ProcessedItemVendorDispatchBatchAssembler processedItemVendorDispatchBatchAssembler;
    @Autowired
    private VendorAssembler vendorAssembler;
    @Autowired
    @Lazy
    private VendorReceiveBatchAssembler vendorReceiveBatchAssembler;
    @Autowired
    private OrderItemWorkflowRepository orderItemWorkflowRepository;

    public VendorDispatchBatch createAssemble(VendorDispatchBatchRepresentation representation) {
        VendorDispatchBatch batch = assemble(representation);
        batch.setCreatedAt(LocalDateTime.now());
        return batch;
    }

    public VendorDispatchBatch assemble(VendorDispatchBatchRepresentation representation) {
        VendorDispatchBatch.VendorDispatchBatchStatus status = null;
        if (representation.getVendorDispatchBatchStatus() != null) {
            status = VendorDispatchBatch.VendorDispatchBatchStatus.valueOf(representation.getVendorDispatchBatchStatus());
        }

        PackagingType packagingType = null;
        if (representation.getPackagingType() != null) {
            packagingType = PackagingType.valueOf(representation.getPackagingType());
        }

        ItemWeightType itemWeightType = null;
        if (representation.getItemWeightType() != null) {
            itemWeightType = ItemWeightType.fromString(representation.getItemWeightType());
        }

        return VendorDispatchBatch.builder()
                .id(representation.getId())
                .vendorDispatchBatchNumber(representation.getVendorDispatchBatchNumber())
                .originalVendorDispatchBatchNumber(representation.getOriginalVendorDispatchBatchNumber())
                .vendorDispatchBatchStatus(status)
                .dispatchedAt(ConvertorUtils.convertStringToLocalDateTime(representation.getDispatchedAt()))
                .remarks(representation.getRemarks())
                .packagingType(packagingType)
                .itemWeightType(itemWeightType)
                .packagingQuantity(representation.getPackagingQuantity())
                .perPackagingQuantity(representation.getPerPackagingQuantity())
                .useUniformPackaging(representation.getUseUniformPackaging())
                .remainingPieces(representation.getRemainingPieces())
                .processes(representation.getProcesses())
                .build();
    }

    public VendorDispatchBatchRepresentation dissemble(VendorDispatchBatch batch) {
        return dissemble(batch, false);
    }

    public VendorDispatchBatchRepresentation dissemble(VendorDispatchBatch batch, boolean includeProcessedItem) {
        List<VendorReceiveBatchRepresentation> vendorReceiveBatches = null;
        if (batch.getVendorReceiveBatches() != null) {
            vendorReceiveBatches = batch.getVendorReceiveBatches().stream()
                    .filter(vendorReceiveBatch -> !vendorReceiveBatch.isDeleted()) // Only include non-deleted batches
                    .map(vendorReceiveBatch -> vendorReceiveBatchAssembler.dissemble(vendorReceiveBatch, false)) // Don't include vendor dispatch batch to prevent circular dependency
                    .collect(Collectors.toList());
        }

        List<VendorProcessType> processes = batch.getProcesses();

        // Fetch order details if itemWorkflowId is available
        String orderPoNumber = null;
        String orderDate = null;
        Long orderId = null;
        
        if (batch.getProcessedItem() != null && batch.getProcessedItem().getItemWorkflowId() != null) {
            // Use repository directly to avoid transaction rollback issues
            Optional<OrderItemWorkflow> orderItemWorkflowOpt = 
                orderItemWorkflowRepository.findByItemWorkflowId(batch.getProcessedItem().getItemWorkflowId());
            
            if (orderItemWorkflowOpt.isPresent()) {
                OrderItemWorkflow orderItemWorkflow = orderItemWorkflowOpt.get();
                if (orderItemWorkflow.getOrderItem() != null && orderItemWorkflow.getOrderItem().getOrder() != null) {
                    orderPoNumber = orderItemWorkflow.getOrderItem().getOrder().getPoNumber();
                    orderDate = orderItemWorkflow.getOrderItem().getOrder().getOrderDate() != null 
                        ? orderItemWorkflow.getOrderItem().getOrder().getOrderDate().toString() 
                        : null;
                    orderId = orderItemWorkflow.getOrderItem().getOrder().getId();
                    log.debug("Found order PO No.: {} for itemWorkflowId: {}", orderPoNumber, batch.getProcessedItem().getItemWorkflowId());
                } else {
                    orderPoNumber = "Non-Order";
                }
            } else {
                // No order found for this itemWorkflowId
                log.debug("No order found for itemWorkflowId: {}. Setting as Non-Order batch.", 
                         batch.getProcessedItem().getItemWorkflowId());
                orderPoNumber = "Non-Order";
            }
        } else {
            // No itemWorkflowId means it's a non-order batch
            orderPoNumber = "Non-Order";
        }

        return VendorDispatchBatchRepresentation.builder()
                .id(batch.getId())
                .vendor(batch.getVendor() != null ? vendorAssembler.dissemble(batch.getVendor()) : null)
                .vendorReceiveBatches(vendorReceiveBatches)
                .processedItem(includeProcessedItem && batch.getProcessedItem() != null ? 
                    processedItemVendorDispatchBatchAssembler.dissemble(batch.getProcessedItem(), false) : null)
                .vendorDispatchBatchNumber(batch.getVendorDispatchBatchNumber())
                .originalVendorDispatchBatchNumber(batch.getOriginalVendorDispatchBatchNumber())
                .vendorDispatchBatchStatus(batch.getVendorDispatchBatchStatus() != null ? batch.getVendorDispatchBatchStatus().toString() : null)
                .dispatchedAt(batch.getDispatchedAt() != null ? batch.getDispatchedAt().toString() : null)
                .remarks(batch.getRemarks())
                .processes(processes)
                .billingEntityId(batch.getBillingEntity() != null ? batch.getBillingEntity().getId() : null)
                .shippingEntityId(batch.getShippingEntity() != null ? batch.getShippingEntity().getId() : null)
                .packagingType(batch.getPackagingType() != null ? batch.getPackagingType().toString() : null)
                .itemWeightType(batch.getItemWeightType() != null ? batch.getItemWeightType().name() : null)
                .packagingQuantity(batch.getPackagingQuantity())
                .perPackagingQuantity(batch.getPerPackagingQuantity())
                .useUniformPackaging(batch.getUseUniformPackaging())
                .remainingPieces(batch.getRemainingPieces())
                .orderPoNumber(orderPoNumber)
                .orderDate(orderDate)
                .orderId(orderId)
                .build();
    }
} 