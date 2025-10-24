package com.jangid.forging_process_management_service.assemblers.gst;

import com.jangid.forging_process_management_service.entities.gst.InvoiceLineItem;
import com.jangid.forging_process_management_service.entitiesRepresentation.gst.InvoiceLineItemRepresentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceLineItemAssembler {

  /**
   * Convert InvoiceLineItemRepresentation to InvoiceLineItem entity
   * Note: Invoice and Tenant relationships should be set by the service layer
   */
  public InvoiceLineItem assemble(InvoiceLineItemRepresentation representation) {
    if (representation == null) {
      return null;
    }

    try {
      return InvoiceLineItem.builder()
        .id(representation.getId())
        .lineNumber(representation.getLineNumber())
        .itemName(representation.getItemName())
        .workType(representation.getWorkType())
        .hsnCode(representation.getHsnCode())
        .quantity(representation.getQuantity())
        .unitOfMeasurement(representation.getUnitOfMeasurement())
        .unitPrice(representation.getUnitPrice())
        .taxableValue(representation.getTaxableValue())
        .cgstRate(representation.getCgstRate())
        .cgstAmount(representation.getCgstAmount())
        .sgstRate(representation.getSgstRate())
        .sgstAmount(representation.getSgstAmount())
        .igstRate(representation.getIgstRate())
        .igstAmount(representation.getIgstAmount())
        .discountPercentage(representation.getDiscountPercentage())
        .discountAmount(representation.getDiscountAmount())
        .lineTotal(representation.getLineTotal())
        .itemWorkflowId(representation.getItemWorkflowId())
        .processedItemDispatchBatchId(representation.getProcessedItemDispatchBatchId())
        .build();
    } catch (Exception e) {
      log.error("Error assembling InvoiceLineItem from representation", e);
      throw new RuntimeException("Failed to assemble InvoiceLineItem: " + e.getMessage(), e);
    }
  }

  /**
   * Convert InvoiceLineItem entity to InvoiceLineItemRepresentation
   */
  public InvoiceLineItemRepresentation disassemble(InvoiceLineItem entity) {
    if (entity == null) {
      return null;
    }

    try {
      return InvoiceLineItemRepresentation.builder()
        .id(entity.getId())
        .invoiceId(entity.getInvoice() != null ? entity.getInvoice().getId() : null)
        .lineNumber(entity.getLineNumber())
        .itemName(entity.getItemName())
        .workType(entity.getWorkType())
        .hsnCode(entity.getHsnCode())
        .quantity(entity.getQuantity())
        .unitOfMeasurement(entity.getUnitOfMeasurement())
        .unitPrice(entity.getUnitPrice())
        .taxableValue(entity.getTaxableValue())
        .cgstRate(entity.getCgstRate())
        .cgstAmount(entity.getCgstAmount())
        .sgstRate(entity.getSgstRate())
        .sgstAmount(entity.getSgstAmount())
        .igstRate(entity.getIgstRate())
        .igstAmount(entity.getIgstAmount())
        .discountPercentage(entity.getDiscountPercentage())
        .discountAmount(entity.getDiscountAmount())
        .lineTotal(entity.getLineTotal())
        .itemWorkflowId(entity.getItemWorkflowId())
        .processedItemDispatchBatchId(entity.getProcessedItemDispatchBatchId())
        .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
    } catch (Exception e) {
      log.error("Error disassembling InvoiceLineItem to representation", e);
      throw new RuntimeException("Failed to disassemble InvoiceLineItem: " + e.getMessage(), e);
    }
  }

  /**
   * Convert list of InvoiceLineItem entities to list of representations
   */
  public List<InvoiceLineItemRepresentation> disassemble(List<InvoiceLineItem> entities) {
    if (entities == null) {
      return null;
    }

    return entities.stream()
      .map(this::disassemble)
      .collect(Collectors.toList());
  }

  /**
   * Update existing InvoiceLineItem entity with data from representation
   */
  public InvoiceLineItem updateEntity(InvoiceLineItem existingEntity, InvoiceLineItemRepresentation representation) {
    if (existingEntity == null || representation == null) {
      return existingEntity;
    }

    try {
      // Update all editable fields
      if (representation.getLineNumber() != null) {
        existingEntity.setLineNumber(representation.getLineNumber());
      }
      if (representation.getItemName() != null) {
        existingEntity.setItemName(representation.getItemName());
      }
      if (representation.getWorkType() != null) {
        existingEntity.setWorkType(representation.getWorkType());
      }
      if (representation.getHsnCode() != null) {
        existingEntity.setHsnCode(representation.getHsnCode());
      }
      if (representation.getQuantity() != null) {
        existingEntity.setQuantity(representation.getQuantity());
      }
      if (representation.getUnitOfMeasurement() != null) {
        existingEntity.setUnitOfMeasurement(representation.getUnitOfMeasurement());
      }
      if (representation.getUnitPrice() != null) {
        existingEntity.setUnitPrice(representation.getUnitPrice());
      }
      if (representation.getDiscountPercentage() != null) {
        existingEntity.setDiscountPercentage(representation.getDiscountPercentage());
      }

      // Recalculate all amounts
      existingEntity.calculateAllAmounts();

      return existingEntity;
    } catch (Exception e) {
      log.error("Error updating InvoiceLineItem entity from representation", e);
      throw new RuntimeException("Failed to update InvoiceLineItem: " + e.getMessage(), e);
    }
  }

  /**
   * Create a new InvoiceLineItem with calculated amounts
   * Helper method for service layer to create line items with automatic calculations
   */
  public InvoiceLineItem createWithCalculations(
      Integer lineNumber,
      String itemName,
      String workType,
      String hsnCode,
      BigDecimal quantity,
      BigDecimal unitPrice,
      boolean isInterState,
      BigDecimal gstRate,
      BigDecimal discountPercentage,
      Long itemWorkflowId,
      Long processedItemDispatchBatchId) {

    InvoiceLineItem lineItem = InvoiceLineItem.builder()
      .lineNumber(lineNumber)
      .itemName(itemName)
      .workType(workType)
      .hsnCode(hsnCode)
      .quantity(quantity)
      .unitPrice(unitPrice)
      .discountPercentage(discountPercentage != null ? discountPercentage : BigDecimal.ZERO)
      .itemWorkflowId(itemWorkflowId)
      .processedItemDispatchBatchId(processedItemDispatchBatchId)
      .build();

    // Set GST rates based on inter-state or intra-state
    if (isInterState) {
      lineItem.setIgstRate(gstRate);
    } else {
      lineItem.setCgstRate(gstRate.divide(BigDecimal.valueOf(2), 2, BigDecimal.ROUND_HALF_UP));
      lineItem.setSgstRate(gstRate.divide(BigDecimal.valueOf(2), 2, BigDecimal.ROUND_HALF_UP));
    }

    // Calculate all amounts
    lineItem.calculateAllAmounts();

    return lineItem;
  }
}

