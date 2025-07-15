package com.jangid.forging_process_management_service.resource.vendor;

import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorEntityRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.ValidationException;
import com.jangid.forging_process_management_service.exception.vendor.VendorNotFoundException;
import com.jangid.forging_process_management_service.service.vendor.VendorService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class VendorResource {

    @Autowired
    private VendorService vendorService;

    @PostMapping("tenant/{tenantId}/vendor")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntity<?> addVendor(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @RequestBody VendorRepresentation vendorRepresentation) {
        try {
            if (tenantId == null || tenantId.isEmpty() || vendorRepresentation.getVendorName() == null) {
                log.error("invalid vendor input!");
                throw new RuntimeException("invalid vendor input!");
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid id!"));

            VendorRepresentation createdVendor = vendorService.createVendor(tenantIdLongValue, vendorRepresentation);
            return new ResponseEntity<>(createdVendor, HttpStatus.CREATED);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException) {
                // Generate a more descriptive error message
                String errorMessage = exception.getMessage();
                log.error("Vendor creation failed: {}", errorMessage);
                
                if (errorMessage.contains("with name=")) {
                    return new ResponseEntity<>(
                        new ErrorResponse("A vendor with the name '" + vendorRepresentation.getVendorName() + "' already exists for this tenant"),
                        HttpStatus.CONFLICT);
                }
            } else if (exception instanceof ValidationException) {
                // Handle validation errors, particularly GSTIN uniqueness
                String errorMessage = exception.getMessage();
                log.error("Validation error: {}", errorMessage);
                
                if (errorMessage.contains("GSTIN/UIN")) {
                    return new ResponseEntity<>(
                        new ErrorResponse(errorMessage),
                        HttpStatus.CONFLICT);
                } else {
                    return new ResponseEntity<>(
                        new ErrorResponse(errorMessage),
                        HttpStatus.BAD_REQUEST);
                }
            } else if (exception instanceof IllegalArgumentException) {
                log.error("Invalid vendor data: {}", exception.getMessage());
                return new ResponseEntity<>(
                    new ErrorResponse(exception.getMessage()),
                    HttpStatus.BAD_REQUEST);
            }
            
            log.error("Error creating vendor: {}", exception.getMessage());
            return new ResponseEntity<>(
                new ErrorResponse("Error creating vendor: " + exception.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("tenant/{tenantId}/vendors")
    public ResponseEntity<?> getAllVendorsOfTenant(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @RequestParam(value = "page") String page,
            @RequestParam(value = "size") String size) {
        Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        Integer pageNumber = (page == null || page.isBlank()) ? -1
                : GenericResourceUtils.convertResourceIdToInt(page)
                        .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

        Integer sizeNumber = (size == null || size.isBlank()) ? -1
                : GenericResourceUtils.convertResourceIdToInt(size)
                        .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

        if (pageNumber == -1 || sizeNumber == -1) {
            return ResponseEntity.ok(vendorService.getAllVendorsOfTenantWithoutPagination(tId));
        }

        Page<VendorRepresentation> vendors = vendorService.getAllVendorsOfTenant(tId, pageNumber, sizeNumber);
        return ResponseEntity.ok(vendors);
    }

    @DeleteMapping("tenant/{tenantId}/vendor/{vendorId}")
    public ResponseEntity<?> deleteVendor(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("vendorId") String vendorId) {
        try {
            if (tenantId == null || tenantId.isEmpty() || vendorId == null) {
                log.error("invalid input for vendor delete!");
                throw new RuntimeException("invalid input for vendor delete!");
            }
            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

            Long vendorIdLongValue = GenericResourceUtils.convertResourceIdToLong(vendorId)
                    .orElseThrow(() -> new RuntimeException("Not valid vendorId!"));

            vendorService.deleteVendor(tenantIdLongValue, vendorIdLongValue);
            return ResponseEntity.noContent().build();
        } catch (Exception exception) {
            if (exception instanceof VendorNotFoundException) {
                return ResponseEntity.notFound().build();
            }
            if (exception instanceof IllegalStateException) {
                log.error("Error while deleting vendor: {}", exception.getMessage());
                return new ResponseEntity<>(new ErrorResponse(exception.getMessage()),
                        HttpStatus.CONFLICT);
            }
            log.error("Error while deleting vendor: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error while deleting vendor"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("tenant/{tenantId}/vendors/search")
    public ResponseEntity<VendorListRepresentation> searchVendors(
            @PathVariable String tenantId,
            @RequestParam String searchType,
            @RequestParam String searchQuery) {

        if (tenantId == null || tenantId.isBlank() || searchType == null || searchQuery == null || searchQuery.isBlank()) {
            log.error("Invalid input for searchVendors. TenantId: {}, SearchType: {}, SearchQuery: {}",
                    tenantId, searchType, searchQuery);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input for searchVendors.");
        }

        Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tenantId!"));

        List<VendorRepresentation> vendors = vendorService.searchVendors(tenantIdLongValue, searchType, searchQuery);
        VendorListRepresentation vendorListRepresentation = VendorListRepresentation.builder()
                .vendorRepresentations(vendors).build();
        return ResponseEntity.ok(vendorListRepresentation);
    }

    @GetMapping("tenant/{tenantId}/vendor/{vendorId}/billing-type")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntity<?> getVendorBillingType(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @ApiParam(value = "Identifier of the vendor", required = true) @PathVariable String vendorId) {
        try {
            if (tenantId == null || tenantId.isEmpty() || vendorId == null || vendorId.isEmpty()) {
                log.error("Invalid input for getting vendor billing type!");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input for getting vendor billing type!");
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tenant id!"));

            Long vendorIdLongValue = GenericResourceUtils.convertResourceIdToLong(vendorId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid vendor id!"));

            List<VendorEntityRepresentation> vendorBillingEntities = vendorService.getVendorBillingType(tenantIdLongValue, vendorIdLongValue);
            return ResponseEntity.ok(vendorBillingEntities);
        } catch (Exception exception) {
            if (exception instanceof VendorNotFoundException) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error while getting vendor billing type: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error while getting vendor billing type"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("tenant/{tenantId}/vendor/{vendorId}/shipping-type")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntity<?> getVendorShippingType(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @ApiParam(value = "Identifier of the vendor", required = true) @PathVariable String vendorId) {
        try {
            if (tenantId == null || tenantId.isEmpty() || vendorId == null || vendorId.isEmpty()) {
                log.error("Invalid input for getting vendor shipping type!");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input for getting vendor shipping type!");
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tenant id!"));

            Long vendorIdLongValue = GenericResourceUtils.convertResourceIdToLong(vendorId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid vendor id!"));

            List<VendorEntityRepresentation> vendorShippingEntities = vendorService.getVendorShippingType(tenantIdLongValue, vendorIdLongValue);
            return ResponseEntity.ok(vendorShippingEntities);
        } catch (Exception exception) {
            if (exception instanceof VendorNotFoundException) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error while getting vendor shipping type: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error while getting vendor shipping type"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 