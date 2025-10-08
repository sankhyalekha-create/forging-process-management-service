package com.jangid.forging_process_management_service.resource.buyer;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerEntityRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerListRepresentation;
import com.jangid.forging_process_management_service.service.buyer.BuyerService;
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
public class BuyerResource {

    @Autowired
    private BuyerService buyerService;

    @PostMapping("buyer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntity<?> addBuyer(@RequestBody BuyerRepresentation buyerRepresentation) {
        try {
            if (buyerRepresentation.getBuyerName() == null) {
                log.error("invalid buyer input!");
                throw new RuntimeException("invalid buyer input!");
            }

            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

            BuyerRepresentation createdBuyer = buyerService.createBuyer(tenantId, buyerRepresentation);
            return new ResponseEntity<>(createdBuyer, HttpStatus.CREATED);
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "addBuyer");
        }
    }

    @GetMapping("buyers")
    public ResponseEntity<?> getAllBuyersOfTenant(
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
                return ResponseEntity.ok(buyerService.getAllBuyersOfTenantWithoutPagination(tId));
            }

            Page<BuyerRepresentation> buyers = buyerService.getAllBuyersOfTenant(tId, pageNumber, sizeNumber);
            return ResponseEntity.ok(buyers);
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getAllBuyersOfTenant");
        }
    }

    @DeleteMapping("buyer/{buyerId}")
    public ResponseEntity<?> deleteBuyer(
            @PathVariable("buyerId") String buyerId) {
        try {
            if (buyerId == null) {
                log.error("invalid input for buyer delete!");
                throw new RuntimeException("invalid input for buyer delete!");
            }
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

            Long buyerIdLongValue = GenericResourceUtils.convertResourceIdToLong(buyerId)
                    .orElseThrow(() -> new RuntimeException("Not valid buyerId!"));

            buyerService.deleteBuyer(tenantId, buyerIdLongValue);
            return ResponseEntity.noContent().build();
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "deleteBuyer");
        }
    }

    @GetMapping("buyers/search")
    public ResponseEntity<?> searchBuyers(
            @RequestParam String searchType,
            @RequestParam String searchQuery) {
        try {
            if (searchType == null || searchQuery == null || searchQuery.isBlank()) {
                log.error("Invalid input for searchBuyers. SearchType: {}, SearchQuery: {}", searchType, searchQuery);
                throw new IllegalArgumentException("Invalid input for searchBuyers.");
            }

            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

            List<BuyerRepresentation> buyers = buyerService.searchBuyers(tenantId, searchType, searchQuery);
            BuyerListRepresentation buyerListRepresentation = BuyerListRepresentation.builder()
                    .buyerRepresentations(buyers).build();
            return ResponseEntity.ok(buyerListRepresentation);
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "searchBuyers");
        }
    }

    @GetMapping("buyer/{buyerId}/billing-type")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntity<?> getBuyerBillingType(
            @ApiParam(value = "Identifier of the buyer", required = true) @PathVariable String buyerId) {
        try {
            if (buyerId == null || buyerId.isEmpty()) {
                log.error("Invalid input for getting buyer billing type!");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input for getting buyer billing type!");
            }

            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
            Long buyerIdLongValue = GenericResourceUtils.convertResourceIdToLong(buyerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid buyer id!"));

            List<BuyerEntityRepresentation> buyerBillingEntities = buyerService.getBuyerBillingType(tenantId, buyerIdLongValue);
            return ResponseEntity.ok(buyerBillingEntities);
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getBuyerBillingType");
        }
    }

    @GetMapping("buyer/{buyerId}/shipping-type")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntity<?> getBuyerShippingType(
            
            @ApiParam(value = "Identifier of the buyer", required = true) @PathVariable String buyerId) {
        try {
            if (buyerId == null || buyerId.isEmpty()) {
                log.error("Invalid input for getting buyer shipping type!");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input for getting buyer shipping type!");
            }

            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

            Long buyerIdLongValue = GenericResourceUtils.convertResourceIdToLong(buyerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid buyer id!"));

            List<BuyerEntityRepresentation> buyerShippingEntities = buyerService.getBuyerShippingType(tenantId, buyerIdLongValue);
            return ResponseEntity.ok(buyerShippingEntities);
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getBuyerShippingType");
        }
    }
} 