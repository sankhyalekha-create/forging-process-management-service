package com.jangid.forging_process_management_service.assemblers.vendor;

import com.jangid.forging_process_management_service.entities.PackagingType;
import com.jangid.forging_process_management_service.entities.vendor.VendorReceiveBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorReceiveBatchRepresentation;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class VendorReceiveBatchAssembler {

    @Autowired
    private VendorAssembler vendorAssembler;
    
    @Autowired
    @Lazy
    private VendorDispatchBatchAssembler vendorDispatchBatchAssembler;

    public VendorReceiveBatch createAssemble(VendorReceiveBatchRepresentation representation) {
        VendorReceiveBatch batch = assemble(representation);
        batch.setCreatedAt(LocalDateTime.now());
        return batch;
    }

    public VendorReceiveBatch assemble(VendorReceiveBatchRepresentation representation) {
        VendorReceiveBatch.VendorReceiveBatchStatus status = null;
        if (representation.getVendorReceiveBatchStatus() != null) {
            status = VendorReceiveBatch.VendorReceiveBatchStatus.valueOf(representation.getVendorReceiveBatchStatus());
        }

        PackagingType packagingType = null;
        if (representation.getPackagingType() != null) {
            packagingType = PackagingType.valueOf(representation.getPackagingType());
        }

        VendorReceiveBatch.VendorReceiveBatchBuilder builder = VendorReceiveBatch.builder()
                .id(representation.getId())
                .vendorReceiveBatchNumber(representation.getVendorReceiveBatchNumber())
                .originalVendorReceiveBatchNumber(representation.getOriginalVendorReceiveBatchNumber())
                .vendorReceiveBatchStatus(status)
                .receivedAt(ConvertorUtils.convertStringToLocalDateTime(representation.getReceivedAt()))
                .isInPieces(representation.getIsInPieces())
                .receivedPiecesCount(representation.getReceivedPiecesCount())
                .rejectedPiecesCount(representation.getRejectedPiecesCount())
                .tenantRejectsCount(representation.getTenantRejectsCount())
                .piecesEligibleForNextOperation(representation.getPiecesEligibleForNextOperation())
                .qualityCheckRequired(representation.getQualityCheckRequired())
                .qualityCheckCompleted(representation.getQualityCheckCompleted())
                .remarks(representation.getRemarks())
                .packagingType(packagingType)
                .packagingQuantity(representation.getPackagingQuantity())
                .perPackagingQuantity(representation.getPerPackagingQuantity())
                .useUniformPackaging(representation.getUseUniformPackaging())
                .remainingPieces(representation.getRemainingPieces());

        // Add quality completion fields if available
        if (representation.getQualityCheckCompletedAt() != null) {
            builder.qualityCheckCompletedAt(ConvertorUtils.convertStringToLocalDateTime(representation.getQualityCheckCompletedAt()));
        }
        if (representation.getFinalVendorRejectsCount() != null) {
            builder.finalVendorRejectsCount(representation.getFinalVendorRejectsCount());
        }
        if (representation.getFinalTenantRejectsCount() != null) {
            builder.finalTenantRejectsCount(representation.getFinalTenantRejectsCount());
        }
        if (representation.getQualityCheckRemarks() != null) {
            builder.qualityCheckRemarks(representation.getQualityCheckRemarks());
        }
        if (representation.getIsLocked() != null) {
            builder.isLocked(representation.getIsLocked());
        }

        return builder.build();
    }

    public VendorReceiveBatchRepresentation dissemble(VendorReceiveBatch batch) {
        return dissemble(batch, true);
    }

    public VendorReceiveBatchRepresentation dissemble(VendorReceiveBatch batch, boolean includeVendorDispatchBatch) {
        return VendorReceiveBatchRepresentation.builder()
                .id(batch.getId())
                .vendor(batch.getVendor() != null ? vendorAssembler.dissemble(batch.getVendor()) : null)
                .vendorDispatchBatch(includeVendorDispatchBatch && batch.getVendorDispatchBatch() != null ? 
                    vendorDispatchBatchAssembler.dissemble(batch.getVendorDispatchBatch()) : null)
                .vendorReceiveBatchNumber(batch.getVendorReceiveBatchNumber())
                .originalVendorReceiveBatchNumber(batch.getOriginalVendorReceiveBatchNumber())
                .vendorReceiveBatchStatus(batch.getVendorReceiveBatchStatus() != null ? batch.getVendorReceiveBatchStatus().toString() : null)
                .receivedAt(batch.getReceivedAt() != null ? batch.getReceivedAt().toString() : null)
                .isInPieces(batch.getIsInPieces())
                .receivedPiecesCount(batch.getReceivedPiecesCount())
                .rejectedPiecesCount(batch.getRejectedPiecesCount())
                .tenantRejectsCount(batch.getTenantRejectsCount())
                .piecesEligibleForNextOperation(batch.getPiecesEligibleForNextOperation())
                .qualityCheckRequired(batch.getQualityCheckRequired())
                .qualityCheckCompleted(batch.getQualityCheckCompleted())
                .remarks(batch.getRemarks())
                .qualityCheckCompletedAt(batch.getQualityCheckCompletedAt() != null ? batch.getQualityCheckCompletedAt().toString() : null)
                .finalVendorRejectsCount(batch.getFinalVendorRejectsCount())
                .finalTenantRejectsCount(batch.getFinalTenantRejectsCount())
                .qualityCheckRemarks(batch.getQualityCheckRemarks())
                .isLocked(batch.getIsLocked())
                .totalFinalRejectsCount(batch.getTotalFinalRejectsCount())
                .billingEntityId(batch.getBillingEntity() != null ? batch.getBillingEntity().getId() : null)
                .shippingEntityId(batch.getShippingEntity() != null ? batch.getShippingEntity().getId() : null)
                .packagingType(batch.getPackagingType() != null ? batch.getPackagingType().toString() : null)
                .packagingQuantity(batch.getPackagingQuantity())
                .perPackagingQuantity(batch.getPerPackagingQuantity())
                .useUniformPackaging(batch.getUseUniformPackaging())
                .remainingPieces(batch.getRemainingPieces())
                .build();
    }
} 