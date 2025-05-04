package com.jangid.forging_process_management_service.resource.inventory;

import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.InwardOutwardStatisticsRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.inventory.InventoryStatisticsService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class InventoryStatisticsResource {

    @Autowired
    private final InventoryStatisticsService inventoryStatisticsService;

    /**
     * Get inward vs outward statistics for a date range
     *
     * @param tenantId The tenant ID
     * @param fromMonth The starting month (1-12)
     * @param fromYear The starting year
     * @param toMonth The ending month (1-12)
     * @param toYear The ending year
     * @return Inward-outward statistics for the given date range
     */
    @GetMapping("tenant/{tenantId}/inventory/inward-outward-statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntity<InwardOutwardStatisticsRepresentation> getInwardOutwardStatistics(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @RequestParam(value = "fromMonth", required = true) int fromMonth,
            @RequestParam(value = "fromYear", required = true) int fromYear,
            @RequestParam(value = "toMonth", required = true) int toMonth,
            @RequestParam(value = "toYear", required = true) int toYear) {
        
        try {
            // Validate input parameters
            if (fromMonth < 1 || fromMonth > 12 || toMonth < 1 || toMonth > 12) {
                log.error("Invalid month values: fromMonth={}, toMonth={}", fromMonth, toMonth);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            if (fromYear > toYear || (fromYear == toYear && fromMonth > toMonth)) {
                log.error("Invalid date range: fromMonth={}, fromYear={}, toMonth={}, toYear={}", 
                         fromMonth, fromYear, toMonth, toYear);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
            
            InwardOutwardStatisticsRepresentation statistics = 
                inventoryStatisticsService.getInwardOutwardStatistics(tenantIdLongValue, fromMonth, fromYear, toMonth, toYear);
            
            return ResponseEntity.ok(statistics);
        } catch (Exception exception) {
            log.error("Error while fetching inward-outward statistics: {}", exception.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 