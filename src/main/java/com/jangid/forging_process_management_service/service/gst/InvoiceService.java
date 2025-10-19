package com.jangid.forging_process_management_service.service.gst;

import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.InvoiceStatus;
import com.jangid.forging_process_management_service.entities.gst.InvoiceType;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.settings.TenantInvoiceSettings;
import com.jangid.forging_process_management_service.dto.gst.InvoiceGenerationRequest;
import com.jangid.forging_process_management_service.repositories.gst.InvoiceRepository;
import com.jangid.forging_process_management_service.service.dispatch.DispatchBatchService;
import com.jangid.forging_process_management_service.service.settings.TenantSettingsService;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

  private final InvoiceRepository invoiceRepository;
  private final DispatchBatchService dispatchBatchService;
  private final TenantSettingsService tenantSettingsService;
  private final TenantService tenantService;

  /**
   * Generate invoice from single dispatch batch
   */
  public Invoice generateInvoiceFromDispatchBatch(Long tenantId, Long dispatchBatchId) {
    log.info("Generating invoice for tenant: {} from dispatch batch: {}", tenantId, dispatchBatchId);

    tenantService.validateTenantExists(tenantId);

    // Check if invoice already exists for this dispatch batch
    Optional<Invoice> existingInvoice = invoiceRepository.findByDispatchBatchIdAndDeletedFalse(dispatchBatchId);
    if (existingInvoice.isPresent()) {
      log.warn("Invoice already exists for dispatch batch: {}", dispatchBatchId);
      return existingInvoice.get();
    }

    // Get dispatch batch details
    DispatchBatch dispatchBatch = dispatchBatchService.getDispatchBatchById(dispatchBatchId);

    if (dispatchBatch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH) {
      throw new IllegalStateException("Dispatch batch must be in READY_TO_DISPATCH status to generate invoice");
    }

    // Generate invoice
    Invoice invoice = createInvoiceFromDispatchBatch(tenantId, dispatchBatch);
    Invoice savedInvoice = invoiceRepository.save(invoice);

    // Update dispatch batch status to DISPATCH_APPROVED
    dispatchBatchService.updateStatusToDispatchApproved(dispatchBatchId);

    log.info("Successfully generated invoice: {} for dispatch batch: {}",
             savedInvoice.getInvoiceNumber(), dispatchBatchId);

    return savedInvoice;
  }

  /**
   * Generate invoice with detailed parameters from UI
   */
  public Invoice generateInvoiceWithDetails(Long tenantId, InvoiceGenerationRequest request) {
    log.info("Generating invoice with details for tenant: {} from {} dispatch batches",
             tenantId, request.getDispatchBatchIds().size());

    tenantService.validateTenantExists(tenantId);

    if (request.getDispatchBatchIds().isEmpty()) {
      throw new IllegalArgumentException("At least one dispatch batch ID is required");
    }

    // Validate all dispatch batches
    List<DispatchBatch> dispatchBatches = request.getDispatchBatchIds().stream()
        .map(dispatchBatchService::getDispatchBatchById)
        .toList();

    // Validate dispatch batches
    validateDispatchBatchesForInvoicing(dispatchBatches, tenantId);

    // Create invoice with custom parameters
    Invoice invoice = createInvoiceWithCustomParameters(tenantId, dispatchBatches, request);
    Invoice savedInvoice = invoiceRepository.save(invoice);

    // Update all dispatch batch statuses to DISPATCH_APPROVED
    dispatchBatchService.updateMultipleBatchesToDispatchApproved(request.getDispatchBatchIds());

    log.info("Successfully generated invoice: {} for {} dispatch batches",
             savedInvoice.getInvoiceNumber(), request.getDispatchBatchIds().size());

    return savedInvoice;
  }

  /**
   * Generate invoice from multiple dispatch batches
   */
  public Invoice generateInvoiceFromMultipleDispatchBatches(Long tenantId, List<Long> dispatchBatchIds) {
    log.info("Generating invoice for tenant: {} from {} dispatch batches", tenantId, dispatchBatchIds.size());

    tenantService.validateTenantExists(tenantId);

    if (dispatchBatchIds.isEmpty()) {
      throw new IllegalArgumentException("At least one dispatch batch ID is required");
    }

    // Validate all dispatch batches
    List<DispatchBatch> dispatchBatches = dispatchBatchIds.stream()
        .map(dispatchBatchService::getDispatchBatchById)
        .toList();

    // Check all batches are READY_TO_DISPATCH
    for (DispatchBatch batch : dispatchBatches) {
      if (batch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH) {
        throw new IllegalStateException("All dispatch batches must be in READY_TO_DISPATCH status. " +
                                        "Batch " + batch.getDispatchBatchNumber() + " is in " + batch.getDispatchBatchStatus());
      }
    }

    // Check if any batch already has an invoice
    for (Long batchId : dispatchBatchIds) {
      Optional<Invoice> existingInvoice = invoiceRepository.findByDispatchBatchIdAndDeletedFalse(batchId);
      if (existingInvoice.isPresent()) {
        throw new IllegalStateException("Dispatch batch " + batchId + " already has an invoice: " +
                                        existingInvoice.get().getInvoiceNumber());
      }
    }

    // Use first batch as primary for invoice details
    DispatchBatch primaryBatch = dispatchBatches.get(0);

    // Generate invoice with combined amounts
    Invoice invoice = createInvoiceFromMultipleDispatchBatches(tenantId, dispatchBatches, primaryBatch);
    Invoice savedInvoice = invoiceRepository.save(invoice);

    // Update all dispatch batches to DISPATCH_APPROVED
    dispatchBatchService.updateMultipleBatchesToDispatchApproved(dispatchBatchIds);

    log.info("Successfully generated invoice: {} for {} dispatch batches",
             savedInvoice.getInvoiceNumber(), dispatchBatchIds.size());

    return savedInvoice;
  }

  /**
   * Create invoice from single dispatch batch
   */
  private Invoice createInvoiceFromDispatchBatch(Long tenantId, DispatchBatch dispatchBatch) {
    TenantInvoiceSettings invoiceSettings = tenantSettingsService.getInvoiceSettings(tenantId);

    Invoice invoice = Invoice.builder()
        .dispatchBatch(dispatchBatch)
        .invoiceNumber(generateInvoiceNumber(tenantId, InvoiceType.REGULAR))
        .invoiceDate(LocalDateTime.now())
        .dueDate(LocalDateTime.now().toLocalDate().plusDays(30))
        .status(InvoiceStatus.DRAFT)
        .invoiceType(InvoiceType.REGULAR)
        .tenant(dispatchBatch.getTenant())
        .recipientBuyerEntity(dispatchBatch.getBillingEntity())
        .placeOfSupply(dispatchBatch.getBillingEntity().getAddress())
        .paymentTerms("Net 30 days")
        .build();

    // Calculate amounts
    calculateInvoiceAmounts(invoice, List.of(dispatchBatch), invoiceSettings);

    return invoice;
  }

  /**
   * Create invoice from multiple dispatch batches
   */
  private Invoice createInvoiceFromMultipleDispatchBatches(Long tenantId, List<DispatchBatch> dispatchBatches,
                                                           DispatchBatch primaryBatch) {
    TenantInvoiceSettings invoiceSettings = tenantSettingsService.getInvoiceSettings(tenantId);

    // Validate all batches have same billing entity
    Long billingEntityId = primaryBatch.getBillingEntity().getId();
    for (DispatchBatch batch : dispatchBatches) {
      if (!batch.getBillingEntity().getId().equals(billingEntityId)) {
        throw new IllegalArgumentException("All dispatch batches must have the same billing entity for combined invoice");
      }
    }

    Invoice invoice = Invoice.builder()
        .dispatchBatch(primaryBatch) // Use primary batch for reference
        .invoiceNumber(generateInvoiceNumber(tenantId, InvoiceType.REGULAR))
        .invoiceDate(LocalDateTime.now())
        .dueDate(LocalDateTime.now().toLocalDate().plusDays(30))
        .status(InvoiceStatus.DRAFT)
        .invoiceType(InvoiceType.REGULAR)
        .tenant(primaryBatch.getTenant())
        .recipientBuyerEntity(primaryBatch.getBillingEntity())
        .placeOfSupply(primaryBatch.getBillingEntity().getAddress())
        .paymentTerms("Net 30 days")
        .build();

    // Calculate combined amounts
    calculateInvoiceAmounts(invoice, dispatchBatches, invoiceSettings);

    return invoice;
  }

  /**
   * Calculate invoice amounts based on dispatch batch data
   */
  private void calculateInvoiceAmounts(Invoice invoice, List<DispatchBatch> dispatchBatches,
                                       TenantInvoiceSettings settings) {

    // Calculate total pieces and amount
    int totalPieces = 0;
    for (DispatchBatch batch : dispatchBatches) {
      if (batch.getProcessedItemDispatchBatch() != null) {
        totalPieces += batch.getProcessedItemDispatchBatch().getTotalDispatchPiecesCount();
      }
    }

    BigDecimal unitRate = BigDecimal.valueOf(100.00); // Default rate - should be configurable
    BigDecimal totalTaxableValue = unitRate.multiply(BigDecimal.valueOf(totalPieces));

    invoice.setTotalTaxableValue(totalTaxableValue);

    // Calculate tax amounts based on inter-state check
    invoice.updateInterStateFlag();

    if (invoice.isInterState()) {
      // Inter-state: Use IGST
      BigDecimal igstRate = settings.getMaterialIgstRate() != null ? settings.getMaterialIgstRate() : BigDecimal.valueOf(18);
      BigDecimal igstAmount = totalTaxableValue.multiply(igstRate).divide(BigDecimal.valueOf(100));

      invoice.setTotalIgstAmount(igstAmount);
      invoice.setTotalCgstAmount(BigDecimal.ZERO);
      invoice.setTotalSgstAmount(BigDecimal.ZERO);
    } else {
      // Intra-state: Use CGST + SGST
      BigDecimal cgstRate = settings.getMaterialCgstRate() != null ? settings.getMaterialCgstRate() : BigDecimal.valueOf(9);
      BigDecimal sgstRate = settings.getMaterialSgstRate() != null ? settings.getMaterialSgstRate() : BigDecimal.valueOf(9);

      BigDecimal cgstAmount = totalTaxableValue.multiply(cgstRate).divide(BigDecimal.valueOf(100));
      BigDecimal sgstAmount = totalTaxableValue.multiply(sgstRate).divide(BigDecimal.valueOf(100));

      invoice.setTotalCgstAmount(cgstAmount);
      invoice.setTotalSgstAmount(sgstAmount);
      invoice.setTotalIgstAmount(BigDecimal.ZERO);
    }

    // Calculate final total
    invoice.calculateTotals();
  }

  /**
   * Generate unique invoice number
   */
  private String generateInvoiceNumber(Long tenantId, InvoiceType invoiceType) {
    TenantInvoiceSettings settings = tenantSettingsService.getInvoiceSettings(tenantId);

    String prefix;
    Integer currentSequence;
    String seriesFormat;
    String sequenceType;

    if (invoiceType == InvoiceType.JOB_WORK) {
      prefix = settings.getJobWorkInvoicePrefix();
      currentSequence = settings.getJobWorkCurrentSequence();
      seriesFormat = settings.getJobWorkSeriesFormat();
      sequenceType = "JOB_WORK_INVOICE";
    } else {
      prefix = settings.getMaterialInvoicePrefix();
      currentSequence = settings.getMaterialCurrentSequence();
      seriesFormat = settings.getMaterialSeriesFormat();
      sequenceType = "MATERIAL_INVOICE";
    }

    String series = seriesFormat != null ? seriesFormat : "{25-26}";
    String invoiceNumber = prefix + series + String.format("%05d", currentSequence);

    // Update sequence
    tenantSettingsService.incrementInvoiceSequence(tenantId, sequenceType);

    return invoiceNumber;
  }

  /**
   * Get all invoices for a tenant with pagination
   */
  @Transactional(readOnly = true)
  public Page<Invoice> getInvoicesByTenant(Long tenantId, Pageable pageable) {
    tenantService.validateTenantExists(tenantId);
    return invoiceRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
  }

  /**
   * Get invoices by status
   */
  @Transactional(readOnly = true)
  public Page<Invoice> getInvoicesByStatus(Long tenantId, InvoiceStatus status, Pageable pageable) {
    tenantService.validateTenantExists(tenantId);
    return invoiceRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, status, pageable);
  }

  /**
   * Get invoice by ID
   */
  @Transactional(readOnly = true)
  public Invoice getInvoiceById(Long invoiceId) {
    return invoiceRepository.findById(invoiceId)
        .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
  }

  /**
   * Search invoices with filters
   */
  @Transactional(readOnly = true)
  public Page<Invoice> searchInvoices(Long tenantId, InvoiceStatus status, Long buyerEntityId,
                                      LocalDateTime fromDate, LocalDateTime toDate, String searchTerm,
                                      Pageable pageable) {
    tenantService.validateTenantExists(tenantId);
    return invoiceRepository.searchInvoices(tenantId, status, buyerEntityId, fromDate, toDate, searchTerm, pageable);
  }

  /**
   * Approve invoice (move from DRAFT to GENERATED)
   */
  public Invoice approveInvoice(Long tenantId, Long invoiceId, String approvedBy) {
    tenantService.validateTenantExists(tenantId);

    Invoice invoice = getInvoiceById(invoiceId);

    if (invoice.getStatus() != InvoiceStatus.DRAFT) {
      throw new IllegalStateException("Invoice cannot be approved in current status: " + invoice.getStatus());
    }

    invoice.setStatus(InvoiceStatus.GENERATED);
    Invoice savedInvoice = invoiceRepository.save(invoice);

    log.info("Invoice {} approved by {}", invoice.getInvoiceNumber(), approvedBy);

    return savedInvoice;
  }

  /**
   * Delete invoice (only if no dispatch batch is DISPATCHED)
   */
  public void deleteInvoice(Long tenantId, Long invoiceId) {
    tenantService.validateTenantExists(tenantId);

    Invoice invoice = getInvoiceById(invoiceId);

    // Check if any associated dispatch batch is DISPATCHED
    if (invoice.getDispatchBatch() != null) {
      DispatchBatch dispatchBatch = invoice.getDispatchBatch();
      if (dispatchBatch.getDispatchBatchStatus() == DispatchBatch.DispatchBatchStatus.DISPATCHED) {
        throw new IllegalStateException("Cannot delete invoice. Associated dispatch batch is already dispatched.");
      }

      // Revert dispatch batch status back to READY_TO_DISPATCH
      dispatchBatchService.revertStatusToReadyToDispatch(dispatchBatch.getId());
    }

    // Soft delete invoice
    invoice.setDeleted(true);
    invoice.setDeletedAt(LocalDateTime.now());
    invoiceRepository.save(invoice);

    log.info("Invoice {} deleted successfully", invoice.getInvoiceNumber());
  }

  /**
   * Get pending invoices count
   */
  @Transactional(readOnly = true)
  public long getPendingInvoicesCount(Long tenantId) {
    return invoiceRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, InvoiceStatus.DRAFT);
  }

  /**
   * Get overdue invoices
   */
  @Transactional(readOnly = true)
  public List<Invoice> getOverdueInvoices(Long tenantId) {
    return invoiceRepository.findOverdueInvoices(tenantId, LocalDate.now());
  }

  /**
   * Get ready to dispatch batches for invoice generation
   */
  @Transactional(readOnly = true)
  public Page<DispatchBatch> getReadyToDispatchBatches(Long tenantId, Pageable pageable) {
    return dispatchBatchService.getReadyToDispatchBatches(tenantId, pageable);
  }

  /**
   * Get count of ready to dispatch batches
   */
  @Transactional(readOnly = true)
  public long getReadyToDispatchBatchesCount(Long tenantId) {
    return dispatchBatchService.getReadyToDispatchBatchesCount(tenantId);
  }

  /**
   * Validate dispatch batches for invoicing
   */
  private void validateDispatchBatchesForInvoicing(List<DispatchBatch> dispatchBatches, Long tenantId) {
    // Check all batches are READY_TO_DISPATCH and belong to same tenant
    Long firstBillingEntityId = null;

    for (DispatchBatch batch : dispatchBatches) {
      if (batch.getTenant().getId() != tenantId) {
        throw new IllegalStateException("Dispatch batch " + batch.getDispatchBatchNumber() +
                                        " does not belong to tenant " + tenantId);
      }

      if (batch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH) {
        throw new IllegalStateException("Dispatch batch " + batch.getDispatchBatchNumber() +
                                        " must be in READY_TO_DISPATCH status. Current status: " +
                                        batch.getDispatchBatchStatus());
      }

      // Ensure all batches have same billing entity for multi-batch invoicing
      if (firstBillingEntityId == null) {
        firstBillingEntityId = batch.getBillingEntity().getId();
      } else if (!batch.getBillingEntity().getId().equals(firstBillingEntityId)) {
        throw new IllegalStateException("All selected dispatch batches must have the same billing entity for multi-batch invoicing.");
      }

      // Check if an invoice already exists for this dispatch batch
      Optional<Invoice> existingInvoice = invoiceRepository.findByDispatchBatchIdAndDeletedFalse(batch.getId());
      if (existingInvoice.isPresent()) {
        throw new IllegalStateException("An invoice already exists for dispatch batch: " + batch.getDispatchBatchNumber());
      }
    }
  }

  /**
   * Create invoice with custom parameters from UI
   */
  private Invoice createInvoiceWithCustomParameters(Long tenantId, List<DispatchBatch> dispatchBatches,
                                                    InvoiceGenerationRequest request) {

    // Invoice settings will be retrieved in generateInvoiceNumber method

    // Determine invoice type
    InvoiceType invoiceType = "JOB_WORK".equals(request.getInvoiceType()) ?
                              InvoiceType.JOB_WORK : InvoiceType.REGULAR;

    // Use first dispatch batch for basic invoice details (all should have same billing entity)
    DispatchBatch firstBatch = dispatchBatches.get(0);

    // Create invoice
    Invoice invoice = Invoice.builder()
        .invoiceNumber(generateInvoiceNumber(tenantId, invoiceType))
        .invoiceDate(LocalDateTime.now())
        .dueDate(LocalDateTime.now().toLocalDate().plusDays(30)) // Default 30 days
        .status(InvoiceStatus.DRAFT)
        .invoiceType(invoiceType)
        .tenant(firstBatch.getTenant())
        .recipientBuyerEntity(firstBatch.getBillingEntity())
        .placeOfSupply(firstBatch.getBillingEntity().getAddress())
        .paymentTerms(request.getPaymentTerms() != null ? request.getPaymentTerms() : "Net 30 days")
        .build();

    // Calculate amounts using custom parameters
    calculateInvoiceAmountsWithCustomParameters(invoice, dispatchBatches, request);

    return invoice;
  }

  /**
   * Calculate invoice amounts using custom parameters from UI
   */
  private void calculateInvoiceAmountsWithCustomParameters(Invoice invoice,
                                                           List<DispatchBatch> dispatchBatches,
                                                           InvoiceGenerationRequest request) {

    // Calculate total pieces from all dispatch batches
    int totalPieces = dispatchBatches.stream()
        .mapToInt(batch -> batch.getProcessedItemDispatchBatch() != null ?
                           batch.getProcessedItemDispatchBatch().getTotalDispatchPiecesCount() : 0)
        .sum();

    // Calculate basic amounts using custom unit rate
    BigDecimal totalTaxableValue = request.getUnitRate().multiply(BigDecimal.valueOf(totalPieces));

    // Set taxable value
    invoice.setTotalTaxableValue(totalTaxableValue);

    // Calculate tax amounts using custom rates
    // For simplicity, assume intra-state (CGST + SGST). In real implementation,
    // you'd check buyer's state vs supplier's state
    BigDecimal cgstAmount = totalTaxableValue.multiply(request.getCgstRate()).divide(BigDecimal.valueOf(100));
    BigDecimal sgstAmount = totalTaxableValue.multiply(request.getSgstRate()).divide(BigDecimal.valueOf(100));
    BigDecimal igstAmount = BigDecimal.ZERO; // For intra-state

    invoice.setTotalCgstAmount(cgstAmount);
    invoice.setTotalSgstAmount(sgstAmount);
    invoice.setTotalIgstAmount(igstAmount);

    // Calculate final total
    invoice.calculateTotals();
  }
}