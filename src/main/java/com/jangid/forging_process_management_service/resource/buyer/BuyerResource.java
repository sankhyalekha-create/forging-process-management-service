package com.jangid.forging_process_management_service.resource.buyer;

import com.jangid.forging_process_management_service.entitiesRepresentation.BuyerRepresentation;
import com.jangid.forging_process_management_service.service.buyer.BuyerService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
} 