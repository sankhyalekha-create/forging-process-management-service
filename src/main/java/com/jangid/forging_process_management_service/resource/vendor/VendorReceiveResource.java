package com.jangid.forging_process_management_service.resource.vendor;

import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorReceiveBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorQualityCheckCompletionRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.service.vendor.VendorReceiveService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Api(value = "Vendor Receive Management")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
@Slf4j
public class VendorReceiveResource {

    @Autowired
    private final VendorReceiveService vendorReceiveService;

    @PostMapping("tenant/{tenantId}/vendor-receive-batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a new vendor receive batch")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Vendor receive batch created successfully"),
            @ApiResponse(code = 400, message = "Bad request - validation failed"),
            @ApiResponse(code = 404, message = "Vendor, dispatch batch, or entity not found"),
            @ApiResponse(code = 409, message = "Batch number already exists")
    })
    public ResponseEntity<?> createVendorReceiveBatch(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @ApiParam(value = "Vendor receive batch details", required = true) @Valid @RequestBody VendorReceiveBatchRepresentation representation) {

        try {
            if (tenantId == null || tenantId.isEmpty() || representation == null) {
                log.error("Invalid vendor receive batch input!");
                throw new RuntimeException("Invalid vendor receive batch input!");
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

            log.info("Creating vendor receive batch for tenant: {}", tenantIdLongValue);
            VendorReceiveBatchRepresentation created = vendorReceiveService.createVendorReceiveBatch(representation, tenantIdLongValue);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException) {
                log.error("Vendor receive batch exists with the given batch number: {}", representation.getVendorReceiveBatchNumber());
                return new ResponseEntity<>(new ErrorResponse("Vendor receive batch exists with the given batch number"), HttpStatus.CONFLICT);
            }
            log.error("Error creating vendor receive batch: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error creating vendor receive batch"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("tenant/{tenantId}/vendor-receive-batch/{batchId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get vendor receive batch by ID")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Vendor receive batch retrieved successfully"),
            @ApiResponse(code = 404, message = "Vendor receive batch not found")
    })
    public ResponseEntity<?> getVendorReceiveBatch(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @ApiParam(value = "Batch ID", required = true) @PathVariable String batchId) {

        try {
            if (tenantId == null || tenantId.isEmpty() || batchId == null || batchId.isEmpty()) {
                log.error("Invalid input for getting vendor receive batch!");
                throw new RuntimeException("Invalid input for getting vendor receive batch!");
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
            Long batchIdLongValue = GenericResourceUtils.convertResourceIdToLong(batchId)
                    .orElseThrow(() -> new RuntimeException("Not valid batchId!"));

            VendorReceiveBatchRepresentation batch = vendorReceiveService.getVendorReceiveBatch(batchIdLongValue, tenantIdLongValue);
            return ResponseEntity.ok(batch);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException && exception.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error getting vendor receive batch: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error getting vendor receive batch"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("tenant/{tenantId}/vendor-receive-batch/{batchId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete a vendor receive batch")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Vendor receive batch deleted successfully"),
            @ApiResponse(code = 404, message = "Vendor receive batch not found"),
            @ApiResponse(code = 409, message = "Vendor receive batch cannot be deleted due to business rules")
    })
    public ResponseEntity<?> deleteVendorReceiveBatch(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @ApiParam(value = "Vendor receive batch ID", required = true) @PathVariable String batchId) {

        try {
            if (tenantId == null || tenantId.isEmpty() || batchId == null || batchId.isEmpty()) {
                log.error("Invalid input for deleting vendor receive batch!");
                return new ResponseEntity<>(new ErrorResponse("Invalid input for deleting vendor receive batch!"), HttpStatus.BAD_REQUEST);
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
            Long batchIdLongValue = GenericResourceUtils.convertResourceIdToLong(batchId)
                    .orElseThrow(() -> new RuntimeException("Not valid batchId!"));

            vendorReceiveService.deleteVendorReceiveBatch(batchIdLongValue, tenantIdLongValue);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            if (exception instanceof RuntimeException && exception.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            if (exception instanceof IllegalStateException) {
                log.error("Error deleting vendor receive batch: {}", exception.getMessage());
                return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.CONFLICT);
            }
            log.error("Error deleting vendor receive batch: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error deleting vendor receive batch"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("tenant/{tenantId}/vendor-dispatch-batch/{dispatchBatchId}/vendor-receive-batches")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all vendor receive batches for a vendor dispatch batch")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Vendor receive batches retrieved successfully"),
            @ApiResponse(code = 404, message = "Vendor dispatch batch not found")
    })
    public ResponseEntity<?> getVendorReceiveBatchesForDispatchBatch(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @ApiParam(value = "Vendor dispatch batch ID", required = true) @PathVariable String dispatchBatchId) {

        try {
            if (tenantId == null || tenantId.isEmpty() || dispatchBatchId == null || dispatchBatchId.isEmpty()) {
                log.error("Invalid input for getting vendor receive batches!");
                throw new RuntimeException("Invalid input for getting vendor receive batches!");
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
            Long dispatchBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(dispatchBatchId)
                    .orElseThrow(() -> new RuntimeException("Not valid dispatchBatchId!"));

            List<VendorReceiveBatchRepresentation> receiveBatches = vendorReceiveService.getVendorReceiveBatchesForDispatchBatch(dispatchBatchIdLongValue, tenantIdLongValue);
            return ResponseEntity.ok(receiveBatches);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException && exception.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error getting vendor receive batches: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error getting vendor receive batches"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("tenant/{tenantId}/vendor-receive-batch/{batchId}/complete-quality-check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Complete quality check for a vendor receive batch")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Quality check completed successfully"),
            @ApiResponse(code = 400, message = "Bad request - validation failed"),
            @ApiResponse(code = 404, message = "Vendor receive batch not found"),
            @ApiResponse(code = 409, message = "Batch is locked or quality check not required")
    })
    public ResponseEntity<?> completeQualityCheck(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @ApiParam(value = "Vendor receive batch ID", required = true) @PathVariable String batchId,
            @ApiParam(value = "Quality check completion details", required = true) @Valid @RequestBody VendorQualityCheckCompletionRepresentation completionRequest) {

        try {
            if (tenantId == null || tenantId.isEmpty() || batchId == null || batchId.isEmpty() || completionRequest == null) {
                log.error("Invalid input for completing quality check!");
                return new ResponseEntity<>(new ErrorResponse("Invalid input for completing quality check!"), HttpStatus.BAD_REQUEST);
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
            Long batchIdLongValue = GenericResourceUtils.convertResourceIdToLong(batchId)
                    .orElseThrow(() -> new RuntimeException("Not valid batchId!"));

            log.info("Completing quality check for vendor receive batch: {} for tenant: {}", batchIdLongValue, tenantIdLongValue);
            VendorReceiveBatchRepresentation updated = vendorReceiveService.completeQualityCheck(
                batchIdLongValue, completionRequest, tenantIdLongValue);
            return ResponseEntity.ok(updated);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException && exception.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            if (exception instanceof IllegalStateException || exception instanceof RuntimeException && exception.getMessage().contains("locked")) {
                log.error("Error completing quality check: {}", exception.getMessage());
                return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.CONFLICT);
            }
            log.error("Error completing quality check: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error completing quality check"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("tenant/{tenantId}/vendor-receive-batches/pending-quality-check")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get vendor receive batches pending quality check")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Pending quality check batches retrieved successfully"),
            @ApiResponse(code = 404, message = "Tenant not found")
    })
    public ResponseEntity<?> getVendorReceiveBatchesPendingQualityCheck(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId) {

        try {
            if (tenantId == null || tenantId.isEmpty()) {
                log.error("Invalid input for getting pending quality check batches!");
                return new ResponseEntity<>(new ErrorResponse("Invalid input for getting pending quality check batches!"), HttpStatus.BAD_REQUEST);
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

            List<VendorReceiveBatchRepresentation> pendingBatches = vendorReceiveService.getVendorReceiveBatchesPendingQualityCheck(tenantIdLongValue);
            return ResponseEntity.ok(pendingBatches);
        } catch (Exception exception) {
            log.error("Error getting pending quality check batches: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error getting pending quality check batches"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}