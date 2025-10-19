package com.jangid.forging_process_management_service.resource.gst;

import com.jangid.forging_process_management_service.assemblers.gst.InvoiceAssembler;
import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.InvoiceStatus;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.dto.gst.InvoiceGenerationRequest;
import com.jangid.forging_process_management_service.entitiesRepresentation.gst.InvoiceRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.service.gst.InvoiceService;
import com.jangid.forging_process_management_service.assemblers.dispatch.DispatchBatchAssembler;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Api(tags = "Invoice Management")
public class InvoiceResource {

  private final InvoiceService invoiceService;
  private final InvoiceAssembler invoiceAssembler;
  private final DispatchBatchAssembler dispatchBatchAssembler;

  /**
   * Get all invoices for a tenant with pagination and optional filters.
   */
  @GetMapping("tenant/{tenantId}/accounting/invoices")
  @ApiOperation(value = "Get all invoices for a tenant with pagination and filters")
  public ResponseEntity<Page<InvoiceRepresentation>> getAllInvoices(
      @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
      @ApiParam(value = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @ApiParam(value = "Page size") @RequestParam(defaultValue = "20") int size,
      @ApiParam(value = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
      @ApiParam(value = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String sortDir,
      @ApiParam(value = "Filter by invoice status") @RequestParam(required = false) String status,
      @ApiParam(value = "Filter by buyer entity ID") @RequestParam(required = false) Long buyerId,
      @ApiParam(value = "Filter by invoice date from (yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) String fromDate,
      @ApiParam(value = "Filter by invoice date to (yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) String toDate,
      @ApiParam(value = "Search term for invoice number") @RequestParam(required = false) String search) {

    Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<Invoice> invoices;

    if (status != null || buyerId != null || fromDate != null || toDate != null || search != null) {
      // Use search with filters
      InvoiceStatus invoiceStatus = status != null ? InvoiceStatus.valueOf(status) : null;
      LocalDateTime fromDateTime = fromDate != null ? ConvertorUtils.convertStringToLocalDateTime(fromDate) : null;
      LocalDateTime toDateTime = toDate != null ? ConvertorUtils.convertStringToLocalDateTime(toDate) : null;

      invoices = invoiceService.searchInvoices(tenantId, invoiceStatus, buyerId,
                                               fromDateTime, toDateTime, search, pageable);
    } else {
      // Get all invoices without specific filters
      invoices = invoiceService.getInvoicesByTenant(tenantId, pageable);
    }

    Page<InvoiceRepresentation> response = invoices.map(invoiceAssembler::disassemble);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  /**
   * Get invoice by ID.
   */
  @GetMapping("/tenant/{tenantId}/accounting/invoices/{invoiceId}")
  @ApiOperation(value = "Get invoice by ID")
  public ResponseEntity<InvoiceRepresentation> getInvoiceById(
      @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
      @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId) {

    log.info("Getting invoice by id: {} for tenant: {}", invoiceId, tenantId);
    Invoice invoice = invoiceService.getInvoiceById(invoiceId);
    return new ResponseEntity<>(invoiceAssembler.disassemble(invoice), HttpStatus.OK);
  }

  /**
   * Generate invoice from a single dispatch batch.
   */
  @PostMapping("/tenant/{tenantId}/accounting/invoices/generate-from-dispatch/{dispatchBatchId}")
  @ApiOperation(value = "Generate invoice from a single dispatch batch")
  public ResponseEntity<InvoiceRepresentation> generateInvoiceFromDispatch(
      @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
      @ApiParam(value = "Dispatch Batch ID", required = true) @PathVariable Long dispatchBatchId) {

    log.info("Generating invoice for tenant: {} from dispatch batch: {}", tenantId, dispatchBatchId);
    Invoice invoice = invoiceService.generateInvoiceFromDispatchBatch(tenantId, dispatchBatchId);
    return new ResponseEntity<>(invoiceAssembler.disassemble(invoice), HttpStatus.CREATED);
  }

  /**
   * Generate invoice from multiple dispatch batches.
   */
  @PostMapping("/tenant/{tenantId}/accounting/invoices/generate-from-multiple-dispatches")
  @ApiOperation(value = "Generate invoice from multiple dispatch batches")
  public ResponseEntity<InvoiceRepresentation> generateInvoiceFromMultipleDispatches(
      @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
      @ApiParam(value = "List of Dispatch Batch IDs", required = true) @RequestBody List<Long> dispatchBatchIds) {

    Invoice invoice = invoiceService.generateInvoiceFromMultipleDispatchBatches(tenantId, dispatchBatchIds);
    return new ResponseEntity<>(invoiceAssembler.disassemble(invoice), HttpStatus.CREATED);
  }

  /**
   * Generate invoice with detailed parameters from UI.
   */
  @PostMapping("/tenant/{tenantId}/accounting/invoices/generate-with-details")
  @ApiOperation(value = "Generate invoice with detailed parameters from UI")
  public ResponseEntity<InvoiceRepresentation> generateInvoiceWithDetails(
      @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
      @ApiParam(value = "Invoice generation request with detailed parameters", required = true)
      @RequestBody InvoiceGenerationRequest request) {

    log.info("Generating invoice with details for tenant: {} with {} dispatch batches",
             tenantId, request.getDispatchBatchIds().size());

    Invoice invoice = invoiceService.generateInvoiceWithDetails(tenantId, request);
    return new ResponseEntity<>(invoiceAssembler.disassemble(invoice), HttpStatus.CREATED);
  }

  /**
   * Approve an invoice.
   */
  @PutMapping("/tenant/{tenantId}/accounting/invoices/{invoiceId}/approve")
  @ApiOperation(value = "Approve an invoice")
  public ResponseEntity<InvoiceRepresentation> approveInvoice(
      @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
      @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId,
      @ApiParam(value = "Approved By (username)", required = true) @RequestParam String approvedBy) {

    log.info("Approving invoice: {} for tenant: {} by {}", invoiceId, tenantId, approvedBy);
    Invoice approvedInvoice = invoiceService.approveInvoice(tenantId, invoiceId, approvedBy);
    return new ResponseEntity<>(invoiceAssembler.disassemble(approvedInvoice), HttpStatus.OK);
  }

  /**
   * Delete an invoice.
   */
  @DeleteMapping("/tenant/{tenantId}/accounting/invoices/{invoiceId}")
  @ApiOperation(value = "Delete an invoice")
  public ResponseEntity<Void> deleteInvoice(
      @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
      @ApiParam(value = "Invoice ID", required = true) @PathVariable Long invoiceId) {

    log.info("Deleting invoice: {} for tenant: {}", invoiceId, tenantId);
    invoiceService.deleteInvoice(tenantId, invoiceId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  /**
   * Get invoices by status.
   */
  @GetMapping("/tenant/{tenantId}/accounting/invoices/status/{status}")
  @ApiOperation(value = "Get invoices by status")
  public ResponseEntity<Page<InvoiceRepresentation>> getInvoicesByStatus(
      @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
      @ApiParam(value = "Invoice Status", required = true, allowableValues = "DRAFT,GENERATED,SENT,PAID,CANCELLED") @PathVariable String status,
      @ApiParam(value = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @ApiParam(value = "Page size") @RequestParam(defaultValue = "20") int size) {

    log.info("Getting invoices with status {} for tenant: {}", status, tenantId);

    InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(status);
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    Page<Invoice> invoices = invoiceService.getInvoicesByStatus(tenantId, invoiceStatus, pageable);
    Page<InvoiceRepresentation> response = invoices.map(invoiceAssembler::disassemble);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  /**
   * Get dispatch batches ready for invoice generation.
   */
  @GetMapping("/tenant/{tenantId}/accounting/invoices/ready-to-dispatch-batches")
  @ApiOperation(value = "Get dispatch batches ready for invoice generation")
  public ResponseEntity<Page<DispatchBatchRepresentation>> getReadyToDispatchBatches(
      @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
      @ApiParam(value = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @ApiParam(value = "Page size") @RequestParam(defaultValue = "20") int size) {

    log.info("Getting ready to dispatch batches for tenant: {}", tenantId);

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "updatedAt"));
    Page<DispatchBatch> readyBatches = invoiceService.getReadyToDispatchBatches(tenantId, pageable);
    Page<DispatchBatchRepresentation> response = readyBatches.map(dispatchBatchAssembler::dissemble);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  /**
   * Get invoice dashboard statistics.
   */
  @GetMapping("/tenant/{tenantId}/accounting/invoices/dashboard")
  @ApiOperation(value = "Get invoice dashboard statistics")
  public ResponseEntity<Map<String, Object>> getInvoiceDashboardStats(
      @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId) {

    log.info("Getting invoice dashboard stats for tenant: {}", tenantId);

    long pendingApprovalCount = invoiceService.getPendingInvoicesCount(tenantId);
    long generatedCount = invoiceService.getInvoicesByStatus(tenantId, InvoiceStatus.GENERATED, Pageable.unpaged()).getTotalElements();
    long paidCount = invoiceService.getInvoicesByStatus(tenantId, InvoiceStatus.PAID, Pageable.unpaged()).getTotalElements();
    long cancelledCount = invoiceService.getInvoicesByStatus(tenantId, InvoiceStatus.CANCELLED, Pageable.unpaged()).getTotalElements();
    List<Invoice> overdueInvoices = invoiceService.getOverdueInvoices(tenantId);
    long readyToDispatchCount = invoiceService.getReadyToDispatchBatchesCount(tenantId);

    Map<String, Object> stats = Map.of(
        "pendingApprovalCount", pendingApprovalCount,
        "generatedCount", generatedCount,
        "paidCount", paidCount,
        "cancelledCount", cancelledCount,
        "overdueCount", overdueInvoices.size(),
        "readyToDispatchCount", readyToDispatchCount,
        "overdueInvoices", overdueInvoices.stream().map(invoiceAssembler::disassemble).toList()
    );

    return new ResponseEntity<>(stats, HttpStatus.OK);
  }
}
