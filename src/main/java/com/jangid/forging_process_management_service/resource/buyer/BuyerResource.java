package com.jangid.forging_process_management_service.resource.buyer;

import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.buyer.BuyerNotFoundException;
import com.jangid.forging_process_management_service.service.buyer.BuyerService;
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
public class BuyerResource {

    @Autowired
    private BuyerService buyerService;

    @PostMapping("tenant/{tenantId}/buyer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntity<BuyerRepresentation> addBuyer(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @RequestBody BuyerRepresentation buyerRepresentation) {
        try {
            if (tenantId == null || tenantId.isEmpty() || buyerRepresentation.getBuyerName() == null) {
                log.error("invalid buyer input!");
                throw new RuntimeException("invalid buyer input!");
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid id!"));

            BuyerRepresentation createdBuyer = buyerService.createBuyer(tenantIdLongValue, buyerRepresentation);
            return new ResponseEntity<>(createdBuyer, HttpStatus.CREATED);
        } catch (Exception exception) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("tenant/{tenantId}/buyers")
    public ResponseEntity<?> getAllBuyersOfTenant(
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
            return ResponseEntity.ok(buyerService.getAllBuyersOfTenantWithoutPagination(tId));
        }

        Page<BuyerRepresentation> buyers = buyerService.getAllBuyersOfTenant(tId, pageNumber, sizeNumber);
        return ResponseEntity.ok(buyers);
    }

    @DeleteMapping("tenant/{tenantId}/buyer/{buyerId}")
    public ResponseEntity<?> deleteBuyer(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("buyerId") String buyerId) {
        try {
            if (tenantId == null || tenantId.isEmpty() || buyerId == null) {
                log.error("invalid input for buyer delete!");
                throw new RuntimeException("invalid input for buyer delete!");
            }
            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

            Long buyerIdLongValue = GenericResourceUtils.convertResourceIdToLong(buyerId)
                    .orElseThrow(() -> new RuntimeException("Not valid buyerId!"));

            buyerService.deleteBuyer(tenantIdLongValue, buyerIdLongValue);
            return ResponseEntity.noContent().build();
        } catch (Exception exception) {
            if (exception instanceof BuyerNotFoundException) {
                return ResponseEntity.notFound().build();
            }
            if (exception instanceof IllegalStateException) {
                log.error("Error while deleting buyer: {}", exception.getMessage());
                return new ResponseEntity<>(new ErrorResponse(exception.getMessage()),
                        HttpStatus.CONFLICT);
            }
            log.error("Error while deleting buyer: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error while deleting buyer"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("tenant/{tenantId}/buyers/search")
    public ResponseEntity<BuyerListRepresentation> searchBuyers(
            @PathVariable String tenantId,
            @RequestParam String searchType,
            @RequestParam String searchQuery) {

        if (tenantId == null || tenantId.isBlank() || searchType == null || searchQuery == null || searchQuery.isBlank()) {
            log.error("Invalid input for searchBuyers. TenantId: {}, SearchType: {}, SearchQuery: {}",
                    tenantId, searchType, searchQuery);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input for searchBuyers.");
        }

        Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tenantId!"));

        List<BuyerRepresentation> buyers = buyerService.searchBuyers(tenantIdLongValue, searchType, searchQuery);
        BuyerListRepresentation buyerListRepresentation = BuyerListRepresentation.builder()
                .buyerRepresentations(buyers).build();
        return ResponseEntity.ok(buyerListRepresentation);
    }
} 