package com.jangid.forging_process_management_service.assemblers.gst;

import com.jangid.forging_process_management_service.entities.gst.EWayBill;
import com.jangid.forging_process_management_service.entities.gst.EWayBillStatus;
import com.jangid.forging_process_management_service.entities.gst.TransportationMode;
import com.jangid.forging_process_management_service.entitiesRepresentation.gst.EWayBillRepresentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EWayBillAssembler {

    /**
     * Convert EWayBillRepresentation to EWayBill entity
     */
    public EWayBill assemble(EWayBillRepresentation representation) {
        if (representation == null) {
            return null;
        }

        try {
            return EWayBill.builder()
                .id(representation.getId())
                .ewayBillNumber(representation.getEwayBillNumber())
                .ewayBillDate(representation.getEwayBillDate())
                .documentNumber(representation.getDocumentNumber())
                .documentDate(representation.getDocumentDate())
                .documentType(representation.getDocumentType())
                .supplierGstin(representation.getSupplierGstin())
                .supplierName(representation.getSupplierName())
                .supplierStateCode(representation.getSupplierStateCode())
                .supplierPincode(representation.getSupplierPincode())
                .recipientGstin(representation.getRecipientGstin())
                .recipientName(representation.getRecipientName())
                .recipientStateCode(representation.getRecipientStateCode())
                .recipientPincode(representation.getRecipientPincode())
                .hsnCode(representation.getHsnCode())
                .goodsDescription(representation.getGoodsDescription())
                .totalQuantity(representation.getTotalQuantity())
                .totalValue(representation.getTotalValue())
                .transportationMode(parseTransportationMode(representation.getTransportationMode()))
                .transportationDistance(representation.getTransportationDistance())
                .vehicleNumber(representation.getVehicleNumber())
                .transporterName(representation.getTransporterName())
                .validFrom(representation.getValidFrom())
                .validUntil(representation.getValidUntil())
                .status(parseEWayBillStatus(representation.getStatus()))
                .build();
        } catch (Exception e) {
            log.error("Error assembling EWayBill from representation", e);
            throw new RuntimeException("Failed to assemble EWayBill: " + e.getMessage(), e);
        }
    }

    /**
     * Convert EWayBill entity to EWayBillRepresentation
     */
    public EWayBillRepresentation disassemble(EWayBill entity) {
        if (entity == null) {
            return null;
        }

        try {
            return EWayBillRepresentation.builder()
                .id(entity.getId())
                .ewayBillNumber(entity.getEwayBillNumber())
                .ewayBillDate(entity.getEwayBillDate())
                .dispatchBatchId(entity.getDispatchBatch() != null ? entity.getDispatchBatch().getId() : null)
                .invoiceId(entity.getInvoice() != null ? entity.getInvoice().getId() : null)
                .deliveryChallanId(entity.getDeliveryChallan() != null ? entity.getDeliveryChallan().getId() : null)
                .documentNumber(entity.getDocumentNumber())
                .documentDate(entity.getDocumentDate())
                .documentType(entity.getDocumentType())
                .supplierGstin(entity.getSupplierGstin())
                .supplierName(entity.getSupplierName())
                .supplierStateCode(entity.getSupplierStateCode())
                .supplierPincode(entity.getSupplierPincode())
                .recipientGstin(entity.getRecipientGstin())
                .recipientName(entity.getRecipientName())
                .recipientStateCode(entity.getRecipientStateCode())
                .recipientPincode(entity.getRecipientPincode())
                .hsnCode(entity.getHsnCode())
                .goodsDescription(entity.getGoodsDescription())
                .totalQuantity(entity.getTotalQuantity())
                .totalValue(entity.getTotalValue())
                .transportationMode(entity.getTransportationMode() != null ? entity.getTransportationMode().name() : null)
                .transportationDistance(entity.getTransportationDistance())
                .vehicleNumber(entity.getVehicleNumber())
                .transporterName(entity.getTransporterName())
                .validFrom(entity.getValidFrom())
                .validUntil(entity.getValidUntil())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
        } catch (Exception e) {
            log.error("Error disassembling EWayBill to representation", e);
            throw new RuntimeException("Failed to disassemble EWayBill: " + e.getMessage(), e);
        }
    }

    /**
     * Convert list of EWayBill entities to list of representations
     */
    public List<EWayBillRepresentation> disassemble(List<EWayBill> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
            .map(this::disassemble)
            .collect(Collectors.toList());
    }

    /**
     * Update existing EWayBill entity with data from representation
     */
    public EWayBill updateEntity(EWayBill existingEntity, EWayBillRepresentation representation) {
        if (existingEntity == null || representation == null) {
            return existingEntity;
        }

        try {
            // Update only non-null fields from representation
            if (representation.getEwayBillNumber() != null) {
                existingEntity.setEwayBillNumber(representation.getEwayBillNumber());
            }
            if (representation.getDocumentNumber() != null) {
                existingEntity.setDocumentNumber(representation.getDocumentNumber());
            }
            if (representation.getDocumentDate() != null) {
                existingEntity.setDocumentDate(representation.getDocumentDate());
            }
            if (representation.getDocumentType() != null) {
                existingEntity.setDocumentType(representation.getDocumentType());
            }
            if (representation.getSupplierGstin() != null) {
                existingEntity.setSupplierGstin(representation.getSupplierGstin());
            }
            if (representation.getSupplierName() != null) {
                existingEntity.setSupplierName(representation.getSupplierName());
            }
            if (representation.getSupplierStateCode() != null) {
                existingEntity.setSupplierStateCode(representation.getSupplierStateCode());
            }
            if (representation.getSupplierPincode() != null) {
                existingEntity.setSupplierPincode(representation.getSupplierPincode());
            }
            if (representation.getRecipientGstin() != null) {
                existingEntity.setRecipientGstin(representation.getRecipientGstin());
            }
            if (representation.getRecipientName() != null) {
                existingEntity.setRecipientName(representation.getRecipientName());
            }
            if (representation.getRecipientStateCode() != null) {
                existingEntity.setRecipientStateCode(representation.getRecipientStateCode());
            }
            if (representation.getRecipientPincode() != null) {
                existingEntity.setRecipientPincode(representation.getRecipientPincode());
            }
            if (representation.getHsnCode() != null) {
                existingEntity.setHsnCode(representation.getHsnCode());
            }
            if (representation.getGoodsDescription() != null) {
                existingEntity.setGoodsDescription(representation.getGoodsDescription());
            }
            if (representation.getTotalQuantity() != null) {
                existingEntity.setTotalQuantity(representation.getTotalQuantity());
            }
            if (representation.getTotalValue() != null) {
                existingEntity.setTotalValue(representation.getTotalValue());
            }
            if (representation.getTransportationMode() != null) {
                existingEntity.setTransportationMode(parseTransportationMode(representation.getTransportationMode()));
            }
            if (representation.getTransportationDistance() != null) {
                existingEntity.setTransportationDistance(representation.getTransportationDistance());
            }
            if (representation.getVehicleNumber() != null) {
                existingEntity.setVehicleNumber(representation.getVehicleNumber());
            }
            if (representation.getTransporterName() != null) {
                existingEntity.setTransporterName(representation.getTransporterName());
            }
            if (representation.getValidFrom() != null) {
                existingEntity.setValidFrom(representation.getValidFrom());
            }
            if (representation.getValidUntil() != null) {
                existingEntity.setValidUntil(representation.getValidUntil());
            }
            if (representation.getStatus() != null) {
                existingEntity.setStatus(parseEWayBillStatus(representation.getStatus()));
            }

            return existingEntity;
        } catch (Exception e) {
            log.error("Error updating EWayBill entity from representation", e);
            throw new RuntimeException("Failed to update EWayBill: " + e.getMessage(), e);
        }
    }

    // Private helper methods
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

    private EWayBillStatus parseEWayBillStatus(String status) {
        if (status == null) {
            return EWayBillStatus.ACTIVE;
        }
        try {
            return EWayBillStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid E-Way Bill status: {}, defaulting to ACTIVE", status);
            return EWayBillStatus.ACTIVE;
        }
    }
}
