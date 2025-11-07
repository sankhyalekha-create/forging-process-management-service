package com.jangid.forging_process_management_service.assemblers.gst;

import com.jangid.forging_process_management_service.assemblers.tenant.TenantAssembler;
import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.InvoiceLineItem;
import com.jangid.forging_process_management_service.entities.gst.InvoiceType;
import com.jangid.forging_process_management_service.entities.gst.InvoiceStatus;
import com.jangid.forging_process_management_service.entities.order.OrderItemWorkflow;
import com.jangid.forging_process_management_service.entities.order.Order;
import com.jangid.forging_process_management_service.entitiesRepresentation.gst.InvoiceRepresentation;
import com.jangid.forging_process_management_service.repositories.order.OrderItemWorkflowRepository;

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
public class InvoiceAssembler {

    private final InvoiceLineItemAssembler invoiceLineItemAssembler;
    private final TenantAssembler tenantAssembler;
    private final OrderItemWorkflowRepository orderItemWorkflowRepository;

    /**
     * Convert InvoiceRepresentation to Invoice entity
     */
    public Invoice assemble(InvoiceRepresentation representation) {
        if (representation == null) {
            return null;
        }

        try {
            return Invoice.builder()
                .id(representation.getId())
                .invoiceNumber(representation.getInvoiceNumber())
                .invoiceDate(representation.getInvoiceDate())
                .invoiceType(parseInvoiceType(representation.getInvoiceType()))
                .placeOfSupply(representation.getPlaceOfSupply())
                .isInterState(representation.getIsInterState())
                .totalTaxableValue(representation.getTotalTaxableValue())
                .totalCgstAmount(representation.getTotalCgstAmount())
                .totalSgstAmount(representation.getTotalSgstAmount())
                .totalIgstAmount(representation.getTotalIgstAmount())
                .totalInvoiceValue(representation.getTotalInvoiceValue())
                .paymentTerms(representation.getPaymentTerms())
                .dueDate(representation.getDueDate())
                .status(parseInvoiceStatus(representation.getStatus()))
                .irn(representation.getIrn())
                .ackNo(representation.getAckNo())
                .ackDate(representation.getAckDate())
                .qrCodeData(representation.getQrCodeData())
                .documentPath(representation.getDocumentPath())
                .build();
        } catch (Exception e) {
            log.error("Error assembling Invoice from representation", e);
            throw new RuntimeException("Failed to assemble Invoice: " + e.getMessage(), e);
        }
    }

    /**
     * Convert Invoice entity to InvoiceRepresentation
     * Uses entity's helper methods to populate supplier/recipient details
     */
    public InvoiceRepresentation disassemble(Invoice entity) {
        if (entity == null) {
            return null;
        }

        try {
            // Fetch Order details from first line item's itemWorkflowId
            String orderPoNumber = null;
            LocalDate orderDate = null;
            
            if (entity.hasLineItems()) {
                InvoiceLineItem firstLineItem = entity.getLineItems().get(0);
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
                        log.warn("Could not fetch Order details for invoice {}: {}", entity.getId(), e.getMessage());
                    }
                }
            }

            return InvoiceRepresentation.builder()
                .id(entity.getId())
                .invoiceNumber(entity.getInvoiceNumber())
                .invoiceDate(entity.getInvoiceDate())
                .invoiceType(entity.getInvoiceType() != null ? entity.getInvoiceType().name() : null)
                // Multi-batch fields
                .dispatchBatchIds(entity.getDispatchBatchIds())
                .dispatchBatchNumbers(entity.getDispatchBatches().stream()
                    .map(db -> db.getDispatchBatchNumber())
                    .collect(Collectors.toList()))
                // Packaging details for each dispatch batch
                .packagingDetails(entity.getDispatchBatches().stream()
                    .map(db -> InvoiceRepresentation.DispatchBatchPackagingDetail.builder()
                        .dispatchBatchNumber(db.getDispatchBatchNumber())
                        .packagingType(db.getPackagingType() != null ? db.getPackagingType().name() : null)
                        .packages(db.getDispatchPackages() != null ? db.getDispatchPackages().stream()
                            .map(pkg -> InvoiceRepresentation.PackageDetail.builder()
                                .packageNumber(pkg.getPackageNumber())
                                .quantityInPackage(pkg.getQuantityInPackage())
                                .build())
                            .collect(Collectors.toList()) : null)
                            .build())
                        .collect(Collectors.toList()))
                .originalInvoiceId(entity.getOriginalInvoice() != null ? entity.getOriginalInvoice().getId() : null)
                // Order reference
                .orderId(entity.getOrderId())
                .orderPoNumber(orderPoNumber)
                .orderDate(orderDate)
                .customerPoNumber(entity.getCustomerPoNumber())
                .customerPoDate(entity.getCustomerPoDate())
                // Supplier details from entity helper methods
                .recipientBuyerEntityId(entity.getRecipientBuyerEntity() != null ? entity.getRecipientBuyerEntity().getId() : null)
                .recipientVendorEntityId(entity.getRecipientVendorEntity() != null ? entity.getRecipientVendorEntity().getId() : null)
                .supplierGstin(entity.getSupplierGstin())
                .supplierName(entity.getSupplierName())
                .supplierAddress(entity.getSupplierAddress())
                .supplierStateCode(entity.getSupplierStateCode())
                // Recipient details from entity helper methods
                .recipientGstin(entity.getRecipientGstin())
                .recipientName(entity.getRecipientName())
                .recipientAddress(entity.getRecipientAddress())
                .recipientStateCode(entity.getRecipientStateCode())
                .recipientType(entity.getRecipientType())
                .placeOfSupply(entity.getPlaceOfSupply())
                .isInterState(entity.getIsInterState())
                // Line items
                .lineItems(entity.hasLineItems() ? invoiceLineItemAssembler.disassemble(entity.getLineItems()) : null)
                // Transportation details
                .transportationMode(entity.getTransportationMode() != null ? entity.getTransportationMode().name() : null)
                .transportationDistance(entity.getTransportationDistance())
                .transporterName(entity.getTransporterName())
                .transporterId(entity.getTransporterId())
                .vehicleNumber(entity.getVehicleNumber())
                .dispatchDate(entity.getDispatchDate())
                // Financial details
                .totalTaxableValue(entity.getTotalTaxableValue())
                .totalCgstAmount(entity.getTotalCgstAmount())
                .totalSgstAmount(entity.getTotalSgstAmount())
                .totalIgstAmount(entity.getTotalIgstAmount())
                .totalInvoiceValue(entity.getTotalInvoiceValue())
                .paymentTerms(entity.getPaymentTerms())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .approvedBy(entity.getApprovedBy())
                .termsAndConditions(entity.getTermsAndConditions())
                .bankName(entity.getBankName())
                .accountNumber(entity.getAccountNumber())
                .ifscCode(entity.getIfscCode())
                .amountInWords(entity.getAmountInWords())
                .irn(entity.getIrn())
                .ackNo(entity.getAckNo())
                .ackDate(entity.getAckDate())
                .qrCodeData(entity.getQrCodeData())
                .totalPaidAmount(entity.getTotalPaidAmount())
                .totalTdsAmountDeducted(entity.getTotalTdsAmountDeducted())
                .paymentCount(entity.getPaymentCount())
                .isManualInvoice(entity.isManualInvoice())
                .documentPath(entity.getDocumentPath())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .tenant(entity.getTenant() != null ? tenantAssembler.dissemble(entity.getTenant()) : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
        } catch (Exception e) {
            log.error("Error disassembling Invoice to representation", e);
            throw new RuntimeException("Failed to disassemble Invoice: " + e.getMessage(), e);
        }
    }

    /**
     * Convert list of Invoice entities to list of representations
     */
    public List<InvoiceRepresentation> disassemble(List<Invoice> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
            .map(this::disassemble)
            .collect(Collectors.toList());
    }

    /**
     * Update existing Invoice entity with data from representation
     */
    public Invoice updateEntity(Invoice existingEntity, InvoiceRepresentation representation) {
        if (existingEntity == null || representation == null) {
            return existingEntity;
        }

        try {
            // Update only non-null fields from representation
            if (representation.getInvoiceType() != null) {
                existingEntity.setInvoiceType(parseInvoiceType(representation.getInvoiceType()));
            }
            if (representation.getPlaceOfSupply() != null) {
                existingEntity.setPlaceOfSupply(representation.getPlaceOfSupply());
            }
            if (representation.getIsInterState() != null) {
                existingEntity.setIsInterState(representation.getIsInterState());
            }
            if (representation.getTotalTaxableValue() != null) {
                existingEntity.setTotalTaxableValue(representation.getTotalTaxableValue());
            }
            if (representation.getTotalCgstAmount() != null) {
                existingEntity.setTotalCgstAmount(representation.getTotalCgstAmount());
            }
            if (representation.getTotalSgstAmount() != null) {
                existingEntity.setTotalSgstAmount(representation.getTotalSgstAmount());
            }
            if (representation.getTotalIgstAmount() != null) {
                existingEntity.setTotalIgstAmount(representation.getTotalIgstAmount());
            }
            if (representation.getTotalInvoiceValue() != null) {
                existingEntity.setTotalInvoiceValue(representation.getTotalInvoiceValue());
            }
            if (representation.getPaymentTerms() != null) {
                existingEntity.setPaymentTerms(representation.getPaymentTerms());
            }
            if (representation.getDueDate() != null) {
                existingEntity.setDueDate(representation.getDueDate());
            }
            if (representation.getStatus() != null) {
                existingEntity.setStatus(parseInvoiceStatus(representation.getStatus()));
            }
            // E-Invoice fields
            if (representation.getIrn() != null) {
                existingEntity.setIrn(representation.getIrn());
            }
            if (representation.getAckNo() != null) {
                existingEntity.setAckNo(representation.getAckNo());
            }
            if (representation.getAckDate() != null) {
                existingEntity.setAckDate(representation.getAckDate());
            }
            if (representation.getQrCodeData() != null) {
                existingEntity.setQrCodeData(representation.getQrCodeData());
            }

            // Recalculate totals after updates
            existingEntity.calculateTotals();

            return existingEntity;
        } catch (Exception e) {
            log.error("Error updating Invoice entity from representation", e);
            throw new RuntimeException("Failed to update Invoice: " + e.getMessage(), e);
        }
    }

    // Private helper methods
    private InvoiceType parseInvoiceType(String invoiceType) {
        if (invoiceType == null) {
            return InvoiceType.TAX_INVOICE;
        }
        try {
            return InvoiceType.valueOf(invoiceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid invoice type: {}, defaulting to TAX_INVOICE", invoiceType);
            return InvoiceType.TAX_INVOICE;
        }
    }

    private InvoiceStatus parseInvoiceStatus(String status) {
        if (status == null) {
            return InvoiceStatus.DRAFT;
        }
        try {
            return InvoiceStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid invoice status: {}, defaulting to DRAFT", status);
            return InvoiceStatus.DRAFT;
        }
    }
}
