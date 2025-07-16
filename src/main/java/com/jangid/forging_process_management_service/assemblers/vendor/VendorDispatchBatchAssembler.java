package com.jangid.forging_process_management_service.assemblers.vendor;

import com.jangid.forging_process_management_service.entities.PackagingType;
import com.jangid.forging_process_management_service.entities.forging.ItemWeightType;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.vendor.VendorProcessType;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorReceiveBatchRepresentation;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
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

        return VendorDispatchBatchRepresentation.builder()
                .id(batch.getId())
                .vendor(batch.getVendor() != null ? vendorAssembler.dissemble(batch.getVendor()) : null)
                .vendorReceiveBatches(vendorReceiveBatches)
                .processedItem(includeProcessedItem && batch.getProcessedItem() != null ? 
                    processedItemVendorDispatchBatchAssembler.dissemble(batch.getProcessedItem(), false) : null)
                .vendorDispatchBatchNumber(batch.getVendorDispatchBatchNumber())
                .originalVendorDispatchBatchNumber(batch.getOriginalVendorDispatchBatchNumber())
                .vendorDispatchBatchStatus(batch.getVendorDispatchBatchStatus() != null ? batch.getVendorDispatchBatchStatus().toString() : null)
                .dispatchedAt(batch.getDispatchedAt().toString())
                .remarks(batch.getRemarks())
                .processes(processes)
                .billingEntityId(batch.getBillingEntity() != null ? batch.getBillingEntity().getId() : null)
                .shippingEntityId(batch.getShippingEntity() != null ? batch.getShippingEntity().getId() : null)
                .packagingType(batch.getPackagingType() != null ? batch.getPackagingType().toString() : null)
                .itemWeightType(batch.getItemWeightType() != null ? batch.getItemWeightType().name() : null)
                .packagingQuantity(batch.getPackagingQuantity())
                .perPackagingQuantity(batch.getPerPackagingQuantity())
                .useUniformPackaging(batch.getUseUniformPackaging())
                .build();
    }
} 