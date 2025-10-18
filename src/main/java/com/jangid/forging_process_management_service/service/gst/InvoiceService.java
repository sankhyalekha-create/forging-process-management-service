package com.jangid.forging_process_management_service.service.gst;

import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.settings.TenantInvoiceSettings;
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
   * Generate draft invoice from dispatch batch when it reaches READY_TO_DISPATCH status
   */
  public Invoice generateInvoiceFromDispatchBatch(Long tenantId, Long dispatchBatchId) {
    log.info("Generating invoice for tenant: {} and dispatch batch: {}", tenantId, dispatchBatchId);
    
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
    
    // Get tenant billing settings
    TenantInvoiceSettings invoiceSettings = tenantSettingsService.getInvoiceSettings(tenantId);
    
    // Create invoice using GST entity
    Invoice invoice = Invoice.builder()
      .dispatchBatch(dispatchBatch)
      .invoiceNumber(generateInvoiceNumber(tenantId, InvoiceType.REGULAR))
      .invoiceDate(LocalDateTime.now())
      .dueDate(LocalDateTime.now().toLocalDate().plusDays(30)) // Default 30 days
      .status(InvoiceStatus.DRAFT)
      .invoiceType(InvoiceType.REGULAR)
      .tenant(dispatchBatch.getTenant())
      .recipientBuyerEntity(dispatchBatch.getBillingEntity())
      .placeOfSupply(dispatchBatch.getBillingEntity().getAddress())
      .paymentTerms("Net 30 days")
      .build();
    
    // Calculate amounts based on dispatch batch
    calculateInvoiceAmounts(invoice, dispatchBatch, invoiceSettings);
    
    // Save invoice
    Invoice savedInvoice = invoiceRepository.save(invoice);
    
    log.info("Successfully generated invoice: {} for dispatch batch: {}", 
             savedInvoice.getInvoiceNumber(), dispatchBatchId);
    
    return savedInvoice;
  }
  
  /**
   * Calculate invoice amounts based on dispatch batch data
   * GST Invoice entity has simplified structure without separate line items
   */
  private void calculateInvoiceAmounts(Invoice invoice, DispatchBatch dispatchBatch, 
                                     TenantInvoiceSettings settings) {
    
    if (dispatchBatch.getProcessedItemDispatchBatch() == null) {
      throw new IllegalStateException("Dispatch batch must have processed item data");
    }
    
    // Calculate basic amounts
    int totalPieces = dispatchBatch.getProcessedItemDispatchBatch().getTotalDispatchPiecesCount();
    BigDecimal unitRate = BigDecimal.valueOf(100.00); // Default rate - should be configurable
    BigDecimal totalTaxableValue = unitRate.multiply(BigDecimal.valueOf(totalPieces));
    
    // Set taxable value
    invoice.setTotalTaxableValue(totalTaxableValue);
    
    // Calculate tax amounts based on inter-state check
    invoice.updateInterStateFlag(); // This will check state codes
    
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
    
    // Calculate final total using GST Invoice's built-in method
    invoice.calculateTotals();
  }
  
  /**
   * Generate unique invoice number based on tenant settings
   */
  private String generateInvoiceNumber(Long tenantId, InvoiceType invoiceType) {
    TenantInvoiceSettings settings = tenantSettingsService.getInvoiceSettings(tenantId);
    
    String prefix;
    Integer currentSequence;
    String seriesFormat;
    
    // For GST Invoice, we'll use material invoice settings as default
    // since most dispatch batches will be material invoices
    prefix = settings.getMaterialInvoicePrefix();
    currentSequence = settings.getMaterialCurrentSequence();
    seriesFormat = settings.getMaterialSeriesFormat();
    
    // Format: PREFIX + SERIES + SEQUENCE
    // Example: MAT{25-26}00001
    String series = seriesFormat != null ? seriesFormat : "{25-26}";
    String invoiceNumber = prefix + series + String.format("%05d", currentSequence);
    
    // Update sequence in settings (this should be done atomically)
    tenantSettingsService.incrementInvoiceSequence(tenantId, "MATERIAL_INVOICE");
    
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
    
    invoice.setStatus(InvoiceStatus.GENERATED); // Move to GENERATED status
    
    Invoice savedInvoice = invoiceRepository.save(invoice);
    
    log.info("Invoice {} approved by {}", invoice.getInvoiceNumber(), approvedBy);
    
    return savedInvoice;
  }
  
  /**
   * Mark invoice as sent
   */
  public Invoice markInvoiceAsSent(Long tenantId, Long invoiceId) {
    tenantService.validateTenantExists(tenantId);
    
    Invoice invoice = getInvoiceById(invoiceId);
    
    if (invoice.getStatus() != InvoiceStatus.GENERATED) {
      throw new IllegalStateException("Only generated invoices can be marked as sent");
    }
    
    invoice.setStatus(InvoiceStatus.SENT);
    
    Invoice savedInvoice = invoiceRepository.save(invoice);
    
    log.info("Invoice {} marked as sent", invoice.getInvoiceNumber());
    
    return savedInvoice;
  }
  
  /**
   * Mark invoice as paid
   */
  public Invoice markInvoiceAsPaid(Long tenantId, Long invoiceId) {
    tenantService.validateTenantExists(tenantId);
    
    Invoice invoice = getInvoiceById(invoiceId);
    
    if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
      throw new IllegalStateException("Cancelled invoices cannot be marked as paid");
    }
    
    invoice.markAsPaid(); // Uses the built-in method
    
    Invoice savedInvoice = invoiceRepository.save(invoice);
    
    log.info("Invoice {} marked as paid", invoice.getInvoiceNumber());
    
    return savedInvoice;
  }
  
  /**
   * Get pending invoices count (DRAFT status)
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
}
