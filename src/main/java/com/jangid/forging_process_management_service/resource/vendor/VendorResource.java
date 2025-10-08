package com.jangid.forging_process_management_service.resource.vendor;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorEntityRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorListRepresentation;
import com.jangid.forging_process_management_service.service.vendor.VendorService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

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

    @PostMapping("vendor")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntity<?> addVendor(
            @RequestBody VendorRepresentation vendorRepresentation) {
        try {
            if (vendorRepresentation.getVendorName() == null) {
                log.error("invalid vendor input!");
                throw new RuntimeException("invalid vendor input!");
            }

            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            VendorRepresentation createdVendor = vendorService.createVendor(tenantIdLongValue, vendorRepresentation);
            return new ResponseEntity<>(createdVendor, HttpStatus.CREATED);
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "addVendor");
        }
    }

    @GetMapping("vendors")
    public ResponseEntity<?> getAllVendorsOfTenant(
            @RequestParam(value = "page") String page,
            @RequestParam(value = "size") String size) {
        try {
            Long tId = TenantContextHolder.getAuthenticatedTenantId();

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
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getAllVendorsOfTenant");
        }
    }

    @DeleteMapping("vendor/{vendorId}")
    public ResponseEntity<?> deleteVendor(
            @PathVariable("vendorId") String vendorId) {
        try {
            if ( vendorId == null) {
                log.error("invalid input for vendor delete!");
                throw new RuntimeException("invalid input for vendor delete!");
            }
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            Long vendorIdLongValue = GenericResourceUtils.convertResourceIdToLong(vendorId)
                    .orElseThrow(() -> new RuntimeException("Not valid vendorId!"));

            vendorService.deleteVendor(tenantIdLongValue, vendorIdLongValue);
            return ResponseEntity.noContent().build();
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "deleteVendor");
        }
    }

    @GetMapping("vendors/search")
    public ResponseEntity<?> searchVendors(
            @RequestParam String searchType,
            @RequestParam String searchQuery) {
        try {
            if (searchType == null || searchQuery == null || searchQuery.isBlank()) {
                log.error("Invalid input for searchVendors. SearchType: {}, SearchQuery: {}",
                        searchType, searchQuery);
                throw new IllegalArgumentException("Invalid input for searchVendors.");
            }

            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            List<VendorRepresentation> vendors = vendorService.searchVendors(tenantIdLongValue, searchType, searchQuery);
            VendorListRepresentation vendorListRepresentation = VendorListRepresentation.builder()
                    .vendorRepresentations(vendors).build();
            return ResponseEntity.ok(vendorListRepresentation);
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "searchVendors");
        }
    }

    @GetMapping("vendor/{vendorId}/billing-type")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntity<?> getVendorBillingType(
            @ApiParam(value = "Identifier of the vendor", required = true) @PathVariable String vendorId) {
        try {
            if ( vendorId == null || vendorId.isEmpty()) {
                log.error("Invalid input for getting vendor billing type!");
                throw new IllegalArgumentException("Invalid input for getting vendor billing type!");
            }

            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            Long vendorIdLongValue = GenericResourceUtils.convertResourceIdToLong(vendorId)
                    .orElseThrow(() -> new RuntimeException("Not a valid vendor id!"));

            List<VendorEntityRepresentation> vendorBillingEntities = vendorService.getVendorBillingType(tenantIdLongValue, vendorIdLongValue);
            return ResponseEntity.ok(vendorBillingEntities);
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getVendorBillingType");
        }
    }

    @GetMapping("vendor/{vendorId}/shipping-type")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntity<?> getVendorShippingType(
            @ApiParam(value = "Identifier of the vendor", required = true) @PathVariable String vendorId) {
        try {
            if ( vendorId == null || vendorId.isEmpty()) {
                log.error("Invalid input for getting vendor shipping type!");
                throw new IllegalArgumentException("Invalid input for getting vendor shipping type!");
            }

            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            Long vendorIdLongValue = GenericResourceUtils.convertResourceIdToLong(vendorId)
                    .orElseThrow(() -> new RuntimeException("Not a valid vendor id!"));

            List<VendorEntityRepresentation> vendorShippingEntities = vendorService.getVendorShippingType(tenantIdLongValue, vendorIdLongValue);
            return ResponseEntity.ok(vendorShippingEntities);
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getVendorShippingType");
        }
    }
} 