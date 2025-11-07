package com.jangid.forging_process_management_service.assemblers.gst;

import com.jangid.forging_process_management_service.entities.gst.DeliveryChallan;
import com.jangid.forging_process_management_service.entities.gst.ChallanLineItem;
import com.jangid.forging_process_management_service.entities.gst.ChallanType;
import com.jangid.forging_process_management_service.entities.gst.ChallanStatus;
import com.jangid.forging_process_management_service.entities.gst.TransportationMode;
import com.jangid.forging_process_management_service.entities.order.Order;
import com.jangid.forging_process_management_service.entities.order.OrderItemWorkflow;
import com.jangid.forging_process_management_service.entitiesRepresentation.gst.DeliveryChallanRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.gst.DeliveryChallanRepresentation.DispatchBatchPackagingDetail;
import com.jangid.forging_process_management_service.entitiesRepresentation.gst.DeliveryChallanRepresentation.PackageDetail;
import com.jangid.forging_process_management_service.repositories.order.OrderItemWorkflowRepository;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.utils.HeatNumberUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryChallanAssembler {

    private final HeatNumberUtil heatNumberUtil;
    private final ItemWorkflowService itemWorkflowService;
    private final OrderItemWorkflowRepository orderItemWorkflowRepository;

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
                .challanDateTime(representation.getChallanDateTime())
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
            // Fetch Order details from first line item's itemWorkflowId
            String orderPoNumber = null;
            LocalDate orderDate = null;
            
            if (entity.hasLineItems()) {
                ChallanLineItem firstLineItem = entity.getLineItems().get(0);
                if (firstLineItem.getItemWorkflowId() != null) {
                    try {
                        Optional<OrderItemWorkflow> orderItemWorkflow = 
                            orderItemWorkflowRepository.findByItemWorkflowId(firstLineItem.getItemWorkflowId());
                        
                        if (orderItemWorkflow.isPresent()) {
                            Order order = orderItemWorkflow.get().getOrderItem().getOrder();
                            orderPoNumber = order.getPoNumber();
                            orderDate = order.getOrderDate();
                        }
                    } catch (Exception e) {
                        log.warn("Could not fetch Order details for challan {}: {}", entity.getId(), e.getMessage());
                    }
                }
            }

            return DeliveryChallanRepresentation.builder()
                .id(entity.getId())
                .challanNumber(entity.getChallanNumber())
                .challanDateTime(entity.getChallanDateTime())
                .challanType(entity.getChallanType() != null ? entity.getChallanType().name() : null)
                .otherChallanTypeDetails(entity.getOtherChallanTypeDetails())
                .workType(entity.getWorkType() != null ? entity.getWorkType().name() : null)
                // Multi-batch fields
                .dispatchBatchIds(entity.getDispatchBatchIds())
                .dispatchBatchNumbers(entity.getDispatchBatches().stream()
                    .map(db -> db.getDispatchBatchNumber())
                    .collect(Collectors.toList()))
                // Packaging details for each dispatch batch
                .packagingDetails(entity.getDispatchBatches().stream()
                    .map(db -> DispatchBatchPackagingDetail.builder()
                        .dispatchBatchNumber(db.getDispatchBatchNumber())
                        .packagingType(db.getPackagingType() != null ? db.getPackagingType().name() : null)
                        .packages(db.getDispatchPackages() != null ? db.getDispatchPackages().stream()
                            .map(pkg -> PackageDetail.builder()
                                .packageNumber(pkg.getPackageNumber())
                                .quantityInPackage(pkg.getQuantityInPackage())
                                .build())
                            .collect(Collectors.toList()) : null)
                            .build())
                        .collect(Collectors.toList()))
                // Order reference
                .orderId(entity.getOrderId())
                .orderPoNumber(orderPoNumber)
                .orderDate(orderDate)
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
                .consignorPhoneNumber(entity.getTenant().getPhoneNumber())
                .consignorEmail(entity.getTenant().getEmail())
                // Consignee details from entity helper methods
                .consigneeGstin(entity.getConsigneeGstin())
                .consigneeName(entity.getConsigneeName())
                .consigneeAddress(entity.getConsigneeAddress())
                .consigneeStateCode(entity.getConsigneeStateCode())
                .consigneeType(entity.getConsigneeType())
                // Transportation details extended
                .transporterName(entity.getTransporterName())
                .transporterId(entity.getTransporterId())
                .vehicleNumber(entity.getVehicleNumber())
                .transportationDistance(entity.getTransportationDistance())
                .amountInWords(entity.getAmountInWords())
                // Terms and conditions
                .termsAndConditions(entity.getTermsAndConditions())
                // Bank details
                .bankName(entity.getBankName())
                .accountNumber(entity.getAccountNumber())
                .ifscCode(entity.getIfscCode())
                // Financial summary
                .totalQuantity(entity.getTotalQuantity())
                .totalTaxableValue(entity.getTotalTaxableValue())
                .totalCgstAmount(entity.getTotalCgstAmount())
                .totalSgstAmount(entity.getTotalSgstAmount())
                .totalIgstAmount(entity.getTotalIgstAmount())
                .totalValue(entity.getTotalValue())
                // Line items - manually map to avoid assembler dependency
                .lineItems(entity.hasLineItems() ? entity.getLineItems().stream()
                    .map(item -> com.jangid.forging_process_management_service.entitiesRepresentation.gst.ChallanLineItemRepresentation.builder()
                        .id(item.getId())
                        .lineNumber(item.getLineNumber())
                        .itemName(item.getItemName())
                        .hsnCode(item.getHsnCode())
                        .workType(item.getWorkType())
                        .quantity(item.getQuantity())
                        .unitOfMeasurement(item.getUnitOfMeasurement())
                        .ratePerUnit(item.getRatePerUnit())
                        .taxableValue(item.getTaxableValue())
                        .cgstRate(item.getCgstRate())
                        .sgstRate(item.getSgstRate())
                        .igstRate(item.getIgstRate())
                        .cgstAmount(item.getCgstAmount())
                        .sgstAmount(item.getSgstAmount())
                        .igstAmount(item.getIgstAmount())
                        .totalValue(item.getTotalValue())
                        .remarks(item.getRemarks())
                        .itemWorkflowId(item.getItemWorkflowId())
                        .itemWorkflowName(item.getItemWorkflowId()!=null?itemWorkflowService.getItemWorkflowById(item.getItemWorkflowId()).getWorkflowIdentifier():null)
                        .processedItemDispatchBatchId(item.getProcessedItemDispatchBatchId())
                        .heatNumbers(heatNumberUtil.getHeatNumbersFromItemWorkflow(item.getItemWorkflowId()))
                        .build())
                    .collect(Collectors.toList()) : null)
                // Status
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                // Document reference
                .documentPath(entity.getDocumentPath())
                // Tenant information
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                // Audit fields
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
