package com.jangid.forging_process_management_service.resource.gst;

import com.jangid.forging_process_management_service.assemblers.dispatch.DispatchBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.gst.DeliveryChallanAssembler;
import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.dto.gst.CancelChallanRequest;
import com.jangid.forging_process_management_service.dto.gst.ChallanGenerationRequest;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.gst.ChallanStatus;
import com.jangid.forging_process_management_service.entities.gst.DeliveryChallan;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.gst.DeliveryChallanRepresentation;
import com.jangid.forging_process_management_service.service.gst.ChallanService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Api(tags = "Challan Management")
public class ChallanResource {

  private final ChallanService challanService;
  private final DeliveryChallanAssembler challanAssembler;
  private final DispatchBatchAssembler dispatchBatchAssembler;

  /**
   * Get all challans for a tenant with pagination and optional filters
   */
  @GetMapping("tenant/{tenantId}/accounting/challans")
  @ApiOperation(value = "Get all challans for a tenant with pagination and filters")
  public ResponseEntity<?> getAllChallans(
      @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
      @ApiParam(value = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @ApiParam(value = "Page size") @RequestParam(defaultValue = "20") int size,
      @ApiParam(value = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
      @ApiParam(value = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String sortDir,
      @ApiParam(value = "Filter by challan status") @RequestParam(required = false) String status,
      @ApiParam(value = "Filter by challan date from (yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) String fromDate,
      @ApiParam(value = "Filter by challan date to (yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) String toDate,
      @ApiParam(value = "Search term for challan number") @RequestParam(required = false) String search) {
    try {
      Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
      Pageable pageable = PageRequest.of(page, size, sort);

      Page<DeliveryChallan> challans;

      if (status != null || fromDate != null || toDate != null || search != null) {
        // Use search with filters
        ChallanStatus challanStatus = status != null ? ChallanStatus.valueOf(status) : null;
        LocalDateTime fromDateTime = fromDate != null ? ConvertorUtils.convertStringToLocalDateTime(fromDate) : null;
        LocalDateTime toDateTime = toDate != null ? ConvertorUtils.convertStringToLocalDateTime(toDate) : null;

        challans = challanService.searchChallans(tenantId, challanStatus, fromDateTime, toDateTime, search, pageable);
      } else {
        // Get all challans without specific filters
        challans = challanService.getChallansByTenant(tenantId, pageable);
      }

      Page<DeliveryChallanRepresentation> response = challans.map(challanAssembler::disassemble);
      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getAllChallans");
    }
  }

  /**
   * Get challan by ID
   */
  @GetMapping("/accounting/challans/{challanId}")
  @ApiOperation(value = "Get challan by ID")
  public ResponseEntity<?> getChallanById(
      @ApiParam(value = "Challan ID", required = true) @PathVariable Long challanId) {
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("Getting challan by id: {} for tenant: {}", challanId, tenantId);
      DeliveryChallan challan = challanService.getChallanById(challanId);
      return new ResponseEntity<>(challanAssembler.disassemble(challan), HttpStatus.OK);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getChallanById");
    }
  }

  /**
   * Get challan by dispatch batch ID
   */
  @GetMapping("/accounting/challans/dispatch-batch/{dispatchBatchId}")
  @ApiOperation(value = "Get challan by dispatch batch ID")
  public ResponseEntity<?> getChallanByDispatchBatchId(
      @ApiParam(value = "Dispatch Batch ID", required = true) @PathVariable Long dispatchBatchId) {
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("Getting challan for dispatch batch: {} for tenant: {}", dispatchBatchId, tenantId);
      DeliveryChallan challan = challanService.getChallanByDispatchBatchId(dispatchBatchId);
      return new ResponseEntity<>(challanAssembler.disassemble(challan), HttpStatus.OK);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getChallanByDispatchBatchId");
    }
  }

  /**
   * Generate challan from dispatch batches
   */
  @PostMapping("/accounting/challans/generate")
  @ApiOperation(value = "Generate challan from dispatch batches")
  public ResponseEntity<?> generateChallan(
      @ApiParam(value = "Challan generation request", required = true)
      @Valid @RequestBody ChallanGenerationRequest request) {
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("Generating challan for tenant: {} with {} dispatch batch(es) - Type: {}",
               tenantId, 
               request.getDispatchBatchIds().size(),
               request.getChallanType());

      DeliveryChallan challan = challanService.generateChallan(tenantId, request);
      return new ResponseEntity<>(challanAssembler.disassemble(challan), HttpStatus.CREATED);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "generateChallan");
    }
  }

  /**
   * Delete a challan (only DRAFT status)
   */
  @DeleteMapping("/accounting/challans/{challanId}")
  @ApiOperation(value = "Delete a challan")
  public ResponseEntity<?> deleteChallan(
      @ApiParam(value = "Challan ID", required = true) @PathVariable Long challanId) {
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("Deleting challan: {} for tenant: {}", challanId, tenantId);
      challanService.deleteChallan(tenantId, challanId);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "deleteChallan");
    }
  }

  /**
   * Mark challan as dispatched
   * Note: This only updates the status. The challan date remains unchanged.
   */
  @PutMapping("/accounting/challans/{challanId}/dispatch")
  @ApiOperation(value = "Mark challan as dispatched")
  public ResponseEntity<?> markChallanAsDispatched(
      @ApiParam(value = "Challan ID", required = true) @PathVariable Long challanId) {
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("Marking challan: {} as DISPATCHED for tenant: {}", challanId, tenantId);

      DeliveryChallan dispatchedChallan = challanService.markAsDispatched(tenantId, challanId);
      return new ResponseEntity<>(challanAssembler.disassemble(dispatchedChallan), HttpStatus.OK);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "markChallanAsDispatched");
    }
  }

  /**
   * Mark challan as delivered
   */
  @PutMapping("/accounting/challans/{challanId}/deliver")
  @ApiOperation(value = "Mark challan as delivered")
  public ResponseEntity<?> markChallanAsDelivered(
      @ApiParam(value = "Challan ID", required = true) @PathVariable Long challanId,
      @ApiParam(value = "Marked By (username)", required = true) @RequestParam String markedBy,
      @ApiParam(value = "Delivered Date (yyyy-MM-dd)", required = true) @RequestParam String actualDeliveryDate) {
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("Marking challan: {} as DELIVERED for tenant: {} by {}", challanId, tenantId, markedBy);
      
      LocalDate deliveryDate = ConvertorUtils.convertStringToLocalDate(actualDeliveryDate);

      DeliveryChallan deliveredChallan = challanService.markAsDelivered(tenantId, challanId, deliveryDate);
      return new ResponseEntity<>(challanAssembler.disassemble(deliveredChallan), HttpStatus.OK);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "markChallanAsDelivered");
    }
  }

  /**
   * Cancel a challan
   */
  @PostMapping("/accounting/challans/{challanId}/cancel")
  @ApiOperation(value = "Cancel a challan")
  public ResponseEntity<?> cancelChallan(
      @ApiParam(value = "Challan ID", required = true) @PathVariable Long challanId,
      @ApiParam(value = "Cancellation request", required = true) @Valid @RequestBody CancelChallanRequest request) {
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("Cancelling challan: {} for tenant: {} by {}", challanId, tenantId, request.getCancelledBy());
      
      DeliveryChallan cancelledChallan = challanService.cancelChallan(
          tenantId, 
          challanId, 
          request.getCancelledBy(), 
          request.getCancellationReason()
      );
      return new ResponseEntity<>(challanAssembler.disassemble(cancelledChallan), HttpStatus.OK);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "cancelChallan");
    }
  }

  /**
   * Get challans by status
   */
  @GetMapping("/accounting/challans/status")
  @ApiOperation(value = "Get challans by status")
  public ResponseEntity<?> getChallansByStatus(
      @ApiParam(value = "Challan Status", required = true, 
                allowableValues = "DRAFT,GENERATED,DISPATCHED,DELIVERED,CONVERTED_TO_INVOICE,CANCELLED") 
      @RequestParam String status,
      @ApiParam(value = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @ApiParam(value = "Page size") @RequestParam(defaultValue = "20") int size) {
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("Getting challans with status {} for tenant: {}", status, tenantId);

      ChallanStatus challanStatus = ChallanStatus.valueOf(status);
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

      Page<DeliveryChallan> challans = challanService.searchChallans(
          tenantId, challanStatus, null, null, null, pageable);
      Page<DeliveryChallanRepresentation> response = challans.map(challanAssembler::disassemble);

      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getChallansByStatus");
    }
  }

  /**
   * Get dispatch batches ready for challan generation
   */
  @GetMapping("/accounting/challans/ready-batches")
  @ApiOperation(value = "Get dispatch batches ready for challan generation")
  public ResponseEntity<?> getReadyToChallanBatches(
      @ApiParam(value = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @ApiParam(value = "Page size") @RequestParam(defaultValue = "20") int size) {
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("Getting ready to challan batches for tenant: {}", tenantId);

      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "updatedAt"));
      Page<DispatchBatch> readyBatches = challanService.getReadyToChallanBatches(tenantId, pageable);
      Page<DispatchBatchRepresentation> response = readyBatches.map(dispatchBatchAssembler::dissemble);

      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getReadyToChallanBatches");
    }
  }

  /**
   * Get challan dashboard statistics
   */
  @GetMapping("/accounting/challans/dashboard")
  @ApiOperation(value = "Get challan dashboard statistics")
  public ResponseEntity<?> getChallanDashboardStats() {
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("Getting challan dashboard stats for tenant: {}", tenantId);

      var stats = challanService.getChallanDashboardStats(tenantId);
      return new ResponseEntity<>(stats, HttpStatus.OK);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getChallanDashboardStats");
    }
  }

  /**
   * Update an existing challan (only DRAFT status can be updated)
   */
  @PutMapping("/accounting/challans/{challanId}")
  @ApiOperation(value = "Update an existing challan")
  public ResponseEntity<?> updateChallan(
      @ApiParam(value = "Challan ID", required = true) @PathVariable Long challanId,
      @ApiParam(value = "Challan update request", required = true) 
      @Valid @RequestBody ChallanGenerationRequest request) {
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("Updating challan: {} for tenant: {}", challanId, tenantId);

      DeliveryChallan updatedChallan = challanService.updateChallan(tenantId, challanId, request);
      return new ResponseEntity<>(challanAssembler.disassemble(updatedChallan), HttpStatus.OK);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateChallan");
    }
  }
}


