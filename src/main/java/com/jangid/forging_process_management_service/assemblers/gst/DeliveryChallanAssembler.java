package com.jangid.forging_process_management_service.assemblers.gst;

import com.jangid.forging_process_management_service.entities.gst.DeliveryChallan;
import com.jangid.forging_process_management_service.entities.gst.ChallanType;
import com.jangid.forging_process_management_service.entities.gst.ChallanStatus;
import com.jangid.forging_process_management_service.entities.gst.TransportationMode;
import com.jangid.forging_process_management_service.entitiesRepresentation.gst.DeliveryChallanRepresentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryChallanAssembler {

    /**
     * Convert DeliveryChallanRepresentation to DeliveryChallan entity
     * Note: Relationships (dispatchBatch, consignee entities, tenant)
     * must be set separately by the service layer
     */
    public DeliveryChallan assemble(DeliveryChallanRepresentation representation) {
        if (representation == null) {
            return null;
        }

        try {
            return DeliveryChallan.builder()
                .id(representation.getId())
                .challanNumber(representation.getChallanNumber())
                .challanDate(representation.getChallanDate())
                .challanType(parseChallanType(representation.getChallanType()))
                .transportationReason(representation.getTransportationReason())
                .transportationMode(parseTransportationMode(representation.getTransportationMode()))
                .expectedDeliveryDate(representation.getExpectedDeliveryDate())
                .actualDeliveryDate(representation.getActualDeliveryDate())
                .totalQuantity(representation.getTotalQuantity())
                .totalValue(representation.getTotalValue())
                .status(parseChallanStatus(representation.getStatus()))
                .documentPath(representation.getDocumentPath())
                .build();
        } catch (Exception e) {
            log.error("Error assembling DeliveryChallan from representation", e);
            throw new RuntimeException("Failed to assemble DeliveryChallan: " + e.getMessage(), e);
        }
    }

    /**
     * Convert DeliveryChallan entity to DeliveryChallanRepresentation
     * Uses entity's helper methods to populate consignor/consignee details
     */
    public DeliveryChallanRepresentation disassemble(DeliveryChallan entity) {
        if (entity == null) {
            return null;
        }

        try {
            return DeliveryChallanRepresentation.builder()
                .id(entity.getId())
                .challanNumber(entity.getChallanNumber())
                .challanDate(entity.getChallanDate())
                .challanType(entity.getChallanType() != null ? entity.getChallanType().name() : null)
                .dispatchBatchId(entity.getDispatchBatch() != null ? entity.getDispatchBatch().getId() : null)
                .convertedToInvoiceId(entity.getConvertedToInvoice() != null ? entity.getConvertedToInvoice().getId() : null)
                .transportationReason(entity.getTransportationReason())
                .transportationMode(entity.getTransportationMode() != null ? entity.getTransportationMode().name() : null)
                .expectedDeliveryDate(entity.getExpectedDeliveryDate())
                .actualDeliveryDate(entity.getActualDeliveryDate())
                // Consignor details from entity helper methods
                .consigneeBuyerEntityId(entity.getConsigneeBuyerEntity() != null ? entity.getConsigneeBuyerEntity().getId() : null)
                .consigneeVendorEntityId(entity.getConsigneeVendorEntity() != null ? entity.getConsigneeVendorEntity().getId() : null)
                .consignorGstin(entity.getConsignorGstin())
                .consignorName(entity.getConsignorName())
                .consignorAddress(entity.getConsignorAddress())
                .consignorStateCode(entity.getConsignorStateCodeFromTenant())
                // Consignee details from entity helper methods
                .consigneeGstin(entity.getConsigneeGstin())
                .consigneeName(entity.getConsigneeName())
                .consigneeAddress(entity.getConsigneeAddress())
                .consigneeStateCode(entity.getConsigneeStateCode())
                .consigneeType(entity.getConsigneeType())
                .totalQuantity(entity.getTotalQuantity())
                .totalValue(entity.getTotalValue())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .documentPath(entity.getDocumentPath())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
        } catch (Exception e) {
            log.error("Error disassembling DeliveryChallan to representation", e);
            throw new RuntimeException("Failed to disassemble DeliveryChallan: " + e.getMessage(), e);
        }
    }

    /**
     * Convert list of DeliveryChallan entities to list of representations
     */
    public List<DeliveryChallanRepresentation> disassemble(List<DeliveryChallan> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
            .map(this::disassemble)
            .collect(Collectors.toList());
    }

    /**
     * Update existing DeliveryChallan entity with data from representation
     */
    public DeliveryChallan updateEntity(DeliveryChallan existingEntity, DeliveryChallanRepresentation representation) {
        if (existingEntity == null || representation == null) {
            return existingEntity;
        }

        try {
            // Update only non-null fields from representation
            if (representation.getChallanType() != null) {
                existingEntity.setChallanType(parseChallanType(representation.getChallanType()));
            }
            if (representation.getTransportationReason() != null) {
                existingEntity.setTransportationReason(representation.getTransportationReason());
            }
            if (representation.getTransportationMode() != null) {
                existingEntity.setTransportationMode(parseTransportationMode(representation.getTransportationMode()));
            }
            if (representation.getExpectedDeliveryDate() != null) {
                existingEntity.setExpectedDeliveryDate(representation.getExpectedDeliveryDate());
            }
            if (representation.getActualDeliveryDate() != null) {
                existingEntity.setActualDeliveryDate(representation.getActualDeliveryDate());
            }
            if (representation.getTotalQuantity() != null) {
                existingEntity.setTotalQuantity(representation.getTotalQuantity());
            }
            if (representation.getTotalValue() != null) {
                existingEntity.setTotalValue(representation.getTotalValue());
            }
            if (representation.getStatus() != null) {
                existingEntity.setStatus(parseChallanStatus(representation.getStatus()));
            }

            return existingEntity;
        } catch (Exception e) {
            log.error("Error updating DeliveryChallan entity from representation", e);
            throw new RuntimeException("Failed to update DeliveryChallan: " + e.getMessage(), e);
        }
    }

    // Private helper methods
    private ChallanType parseChallanType(String challanType) {
        if (challanType == null) {
            return null;
        }
        try {
            return ChallanType.valueOf(challanType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid challan type: {}, defaulting to OTHER", challanType);
            return ChallanType.OTHER;
        }
    }

    private ChallanStatus parseChallanStatus(String status) {
        if (status == null) {
            return ChallanStatus.DRAFT;
        }
        try {
            return ChallanStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid challan status: {}, defaulting to DRAFT", status);
            return ChallanStatus.DRAFT;
        }
    }

    private TransportationMode parseTransportationMode(String mode) {
        if (mode == null) {
            return TransportationMode.ROAD;
        }
        try {
            return TransportationMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid transportation mode: {}, defaulting to ROAD", mode);
            return TransportationMode.ROAD;
        }
    }
}
