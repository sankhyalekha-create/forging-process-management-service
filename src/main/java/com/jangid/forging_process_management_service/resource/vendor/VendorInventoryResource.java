package com.jangid.forging_process_management_service.resource.vendor;

import com.jangid.forging_process_management_service.assemblers.vendor.VendorInventoryAssembler;
import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entities.vendor.VendorInventory;
import com.jangid.forging_process_management_service.entities.vendor.VendorInventoryTransaction;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorInventoryRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorInventoryListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorInventoryTransactionRepresentation;
import com.jangid.forging_process_management_service.dto.vendor.VendorInventoryTransferRequest;
import com.jangid.forging_process_management_service.dto.vendor.VendorInventoryReturnRequest;
import com.jangid.forging_process_management_service.service.vendor.VendorInventoryService;
import com.jangid.forging_process_management_service.service.vendor.VendorInventoryTransactionService;
import com.jangid.forging_process_management_service.service.vendor.VendorService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.stream.Collectors;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.CalculatedVendorInventoryRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.CalculatedVendorInventoryListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.CalculatedVendorInventorySummary;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorInventoryTransactionSummary;


@Api(value = "Vendor Inventory Management")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
@Slf4j
public class VendorInventoryResource {

    @Autowired
    private final VendorInventoryService vendorInventoryService;

    @Autowired
    private final VendorInventoryTransactionService vendorInventoryTransactionService;

    @Autowired
    private final VendorService vendorService;

    @Autowired
    private VendorInventoryAssembler vendorInventoryAssembler;

    @GetMapping("vendor/{vendorId}/inventory")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get vendor inventory for a specific vendor with optional pagination")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Vendor inventory retrieved successfully"),
            @ApiResponse(code = 404, message = "Vendor not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<?> getVendorInventory(
            @ApiParam(value = "Identifier of the vendor", required = true) @PathVariable String vendorId,
            @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(value = "page") String page,
            @ApiParam(value = "Page size", required = false) @RequestParam(value = "size") String size) {

        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            Long vendorIdLongValue = GenericResourceUtils.convertResourceIdToLong(vendorId)
                    .orElseThrow(() -> new RuntimeException("Not valid vendorId!"));

            // Validate that vendor belongs to the tenant
            vendorService.validateVendorExists(vendorIdLongValue, tenantIdLongValue);

            Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                                  : GenericResourceUtils.convertResourceIdToInt(page)
                                 .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

            Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                                  : GenericResourceUtils.convertResourceIdToInt(size)
                                 .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

            if (pageNumber == -1 || sizeNumber == -1) {
                List<VendorInventory> vendorInventories = vendorInventoryService.getAllInventoryByVendor(vendorIdLongValue);
                List<VendorInventoryRepresentation> representations = vendorInventories.stream()
                        .map(vendorInventoryAssembler::dissemble)
                        .collect(Collectors.toList());
                VendorInventoryListRepresentation vendorInventoryListRepresentation = VendorInventoryListRepresentation.builder()
                        .vendorInventories(representations)
                        .build();
                return ResponseEntity.ok(vendorInventoryListRepresentation);
            }

            Page<VendorInventory> vendorInventoryPage = vendorInventoryService.getVendorInventory(vendorIdLongValue, pageNumber, sizeNumber);
            Page<VendorInventoryRepresentation> representationPage = vendorInventoryPage.map(vendorInventoryAssembler::dissemble);

            return ResponseEntity.ok(representationPage);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getVendorInventory");
        }
    }

    @GetMapping("vendor/{vendorId}/inventory/available")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get available vendor inventory for a specific vendor with optional pagination")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Available vendor inventory retrieved successfully"),
            @ApiResponse(code = 404, message = "Vendor not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<?> getAvailableVendorInventory(
            @ApiParam(value = "Identifier of the vendor", required = true) @PathVariable String vendorId,
            @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(value = "page") String page,
            @ApiParam(value = "Page size", required = false) @RequestParam(value = "size") String size) {

        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            Long vendorIdLongValue = GenericResourceUtils.convertResourceIdToLong(vendorId)
                    .orElseThrow(() -> new RuntimeException("Not valid vendorId!"));

            // Validate that vendor belongs to the tenant
            vendorService.validateVendorExists(vendorIdLongValue, tenantIdLongValue);

            Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                                  : GenericResourceUtils.convertResourceIdToInt(page)
                                 .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

            Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                                  : GenericResourceUtils.convertResourceIdToInt(size)
                                 .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

            if (pageNumber == -1 || sizeNumber == -1) {
                // Non-paginated response
                List<VendorInventory> availableInventory = vendorInventoryService.getAvailableInventoryByVendor(vendorIdLongValue);
                List<VendorInventoryRepresentation> representations = availableInventory.stream()
                        .map(vendorInventoryAssembler::dissemble)
                        .collect(Collectors.toList());
                VendorInventoryListRepresentation vendorInventoryListRepresentation = VendorInventoryListRepresentation.builder()
                        .vendorInventories(representations)
                        .build();
                return ResponseEntity.ok(vendorInventoryListRepresentation);
            }

            // Paginated response
            Page<VendorInventory> availableInventoryPage = vendorInventoryService.getAvailableInventoryByVendor(vendorIdLongValue, pageNumber, sizeNumber);
            Page<VendorInventoryRepresentation> representationPage = availableInventoryPage.map(vendorInventoryAssembler::dissemble);

            return ResponseEntity.ok(representationPage);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getAvailableVendorInventory");
        }
    }

    @GetMapping("vendor/{vendorId}/calculated-inventory")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get calculated vendor inventory based on dispatch and receive batches")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Calculated vendor inventory retrieved successfully"),
            @ApiResponse(code = 404, message = "Vendor not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<?> getCalculatedVendorInventory(
            @ApiParam(value = "Identifier of the vendor", required = true) @PathVariable String vendorId,
            @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(value = "page") String page,
            @ApiParam(value = "Page size", required = false) @RequestParam(value = "size") String size) {

        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            Long vendorIdLongValue = GenericResourceUtils.convertResourceIdToLong(vendorId)
                    .orElseThrow(() -> new RuntimeException("Not valid vendorId!"));

            // Validate that vendor belongs to the tenant
            vendorService.validateVendorExists(vendorIdLongValue, tenantIdLongValue);

            Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                                  : GenericResourceUtils.convertResourceIdToInt(page)
                                 .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

            Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                                  : GenericResourceUtils.convertResourceIdToInt(size)
                                 .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

            // Get calculated inventory from dispatch/receive batches
            if (pageNumber == -1 || sizeNumber == -1) {
                // Non-paginated response
                List<CalculatedVendorInventoryRepresentation> calculatedInventory = 
                    vendorInventoryService.getCalculatedInventoryByVendor(vendorIdLongValue, tenantIdLongValue);
                CalculatedVendorInventoryListRepresentation listRepresentation = CalculatedVendorInventoryListRepresentation.builder()
                        .calculatedInventories(calculatedInventory)
                        .build();
                return ResponseEntity.ok(listRepresentation);
            }

            // Paginated response
            Page<CalculatedVendorInventoryRepresentation> calculatedInventoryPage = 
                vendorInventoryService.getCalculatedInventoryByVendor(vendorIdLongValue, tenantIdLongValue, pageNumber, sizeNumber);

            return ResponseEntity.ok(calculatedInventoryPage);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getCalculatedVendorInventory");
        }
    }

    @GetMapping("vendor/{vendorId}/calculated-inventory/summary")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get calculated vendor inventory summary for quick overview")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Calculated vendor inventory summary retrieved successfully"),
            @ApiResponse(code = 404, message = "Vendor not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<?> getCalculatedVendorInventorySummary(
            @ApiParam(value = "Identifier of the vendor", required = true) @PathVariable String vendorId) {

        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            Long vendorIdLongValue = GenericResourceUtils.convertResourceIdToLong(vendorId)
                    .orElseThrow(() -> new RuntimeException("Not valid vendorId!"));

            // Validate that vendor belongs to the tenant
            vendorService.validateVendorExists(vendorIdLongValue, tenantIdLongValue);

            CalculatedVendorInventorySummary summary = vendorInventoryService.getCalculatedInventorySummary(vendorIdLongValue, tenantIdLongValue);

            return ResponseEntity.ok(summary);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getCalculatedVendorInventorySummary");
        }
    }


    // New Batch APIs
    
    @PostMapping("vendor-inventory/batch-transfer-to-vendor")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Batch transfer multiple heats from tenant inventory to vendor inventory")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successfully transferred materials to vendor", response = VendorInventoryTransactionRepresentation.class),
        @ApiResponse(code = 400, message = "Invalid transfer request"),
        @ApiResponse(code = 404, message = "Vendor or heat not found")
    })
    public ResponseEntity<?> batchTransferMaterialToVendor(
            @ApiParam(value = "Transfer request containing vendor ID and list of heats to transfer", required = true) @RequestBody VendorInventoryTransferRequest request) {
        
        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            VendorInventoryTransaction transaction = vendorInventoryTransactionService.batchTransferMaterialToVendor(tenantIdLongValue, request);
            
            // Convert to representation (you'll need to create an assembler for this)
            VendorInventoryTransactionRepresentation representation = convertToRepresentation(transaction);
            
            log.info("Successfully completed batch transfer to vendor {}. Transaction ID: {}", request.getVendorId(), transaction.getId());
            return ResponseEntity.ok(representation);
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "batchTransferToVendor");
        }
    }
    
    @PostMapping("vendor-inventory/batch-return-from-vendor")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Batch return multiple items from vendor inventory back to tenant inventory")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successfully returned materials to tenant", response = VendorInventoryTransactionRepresentation.class),
        @ApiResponse(code = 400, message = "Invalid return request"),
        @ApiResponse(code = 404, message = "Vendor inventory not found")
    })
    public ResponseEntity<?> batchReturnMaterialFromVendor(
            @ApiParam(value = "Return request containing vendor ID and list of items to return", required = true) @RequestBody VendorInventoryReturnRequest request) {
        
        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            VendorInventoryTransaction transaction = vendorInventoryTransactionService.batchReturnMaterialFromVendor(tenantIdLongValue, request);
            
            // Convert to representation (you'll need to create an assembler for this)
            VendorInventoryTransactionRepresentation representation = convertToRepresentation(transaction);
            
            log.info("Successfully completed batch return from vendor {}. Transaction ID: {}", request.getVendorId(), transaction.getId());
            return ResponseEntity.ok(representation);
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "batchReturnFromVendor");
        }
    }



    @GetMapping("vendor-inventory-transactions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get vendor inventory transactions")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Vendor inventory transactions retrieved successfully"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<?> getVendorInventoryTransactions(
            @ApiParam(value = "Vendor ID to filter by", required = false) @RequestParam(required = false) Long vendorId,
            @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "Page size", required = false) @RequestParam(defaultValue = "10") int size) {

        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            Page<VendorInventoryTransaction> transactionPage = vendorInventoryTransactionService.getVendorInventoryTransactions(tenantIdLongValue, vendorId, page, size);
            Page<VendorInventoryTransactionRepresentation> representationPage = transactionPage.map(this::convertToRepresentation);

            return ResponseEntity.ok(representationPage);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getVendorInventoryTransactions");
        }
    }

    @GetMapping("vendor-inventory-transactions/{transactionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get vendor inventory transaction by ID")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Vendor inventory transaction retrieved successfully"),
        @ApiResponse(code = 404, message = "Transaction not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<?> getVendorInventoryTransactionById(
            @ApiParam(value = "Transaction ID", required = true) @PathVariable String transactionId) {

        try {
            Long transactionIdLongValue = GenericResourceUtils.convertResourceIdToLong(transactionId)
                    .orElseThrow(() -> new RuntimeException("Not valid transactionId!"));

            VendorInventoryTransaction transaction = vendorInventoryTransactionService.getVendorInventoryTransactionById(transactionIdLongValue);
            VendorInventoryTransactionRepresentation representation = convertToRepresentation(transaction);

            return ResponseEntity.ok(representation);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getVendorInventoryTransaction");
        }
    }

    // Helper method to convert entity to representation
    private VendorInventoryTransactionRepresentation convertToRepresentation(VendorInventoryTransaction transaction) {
        VendorInventoryTransactionRepresentation representation = new VendorInventoryTransactionRepresentation();
        representation.setId(transaction.getId());
        representation.setVendorId(transaction.getVendor().getId());
        representation.setVendorName(transaction.getVendor().getVendorName());
        representation.setTransactionType(transaction.getTransactionType());
        representation.setTransactionDateTime(transaction.getTransactionDateTime());
        representation.setRemarks(transaction.getRemarks());
        representation.setTotalQuantityTransferred(transaction.getTotalQuantityTransferred());
        representation.setTotalPiecesTransferred(transaction.getTotalPiecesTransferred());
        representation.setCreatedAt(transaction.getCreatedAt());
        
        // Convert transaction items
        if (transaction.getTransactionItems() != null) {
            List<VendorInventoryTransactionRepresentation.VendorInventoryTransactionItemRepresentation> itemRepresentations = 
                transaction.getTransactionItems().stream()
                    .map(item -> {
                        VendorInventoryTransactionRepresentation.VendorInventoryTransactionItemRepresentation itemRep = 
                            new VendorInventoryTransactionRepresentation.VendorInventoryTransactionItemRepresentation();
                        itemRep.setId(item.getId());
                        itemRep.setHeatId(item.getHeat().getId());
                        itemRep.setHeatNumber(item.getHeatNumber());
                        itemRep.setQuantityTransferred(item.getQuantityTransferred());
                        itemRep.setPiecesTransferred(item.getPiecesTransferred());
                        itemRep.setTestCertificateNumber(item.getTestCertificateNumber());
                        itemRep.setLocation(item.getLocation());
                        itemRep.setIsInPieces(item.getIsInPieces());
                        itemRep.setCreatedAt(item.getCreatedAt());
                        return itemRep;
                    })
                    .collect(Collectors.toList());
            representation.setTransactionItems(itemRepresentations);
        }
        
        return representation;
    }

    @GetMapping("vendor/{vendorId}/inventory-transactions/summary")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get vendor inventory transaction summary for a specific vendor")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Vendor inventory transaction summary retrieved successfully"),
            @ApiResponse(code = 404, message = "Vendor not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<?> getVendorInventoryTransactionSummary(
            @ApiParam(value = "Identifier of the vendor", required = true) @PathVariable String vendorId) {

        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            Long vendorIdLongValue = GenericResourceUtils.convertResourceIdToLong(vendorId)
                    .orElseThrow(() -> new RuntimeException("Not valid vendorId!"));

            // Validate that vendor belongs to the tenant
            vendorService.validateVendorExists(vendorIdLongValue, tenantIdLongValue);

            VendorInventoryTransactionSummary summary = vendorInventoryTransactionService
                    .getVendorInventoryTransactionSummary(tenantIdLongValue, vendorIdLongValue);

            return ResponseEntity.ok(summary);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getVendorInventoryTransactionSummary");
        }
    }
} 