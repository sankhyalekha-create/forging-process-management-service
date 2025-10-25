package com.jangid.forging_process_management_service.resource.transporter;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entitiesRepresentation.transporter.TransporterListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.transporter.TransporterRepresentation;
import com.jangid.forging_process_management_service.service.transporter.TransporterService;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * REST controller for Transporter operations.
 * Provides endpoints for creating, retrieving, searching, and deleting transporters.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
@Api(tags = "Transporter Management", description = "APIs for managing transporters for GST-compliant invoice generation")
public class TransporterResource {
  
  @Autowired
  private TransporterService transporterService;
  
  /**
   * Creates a new transporter.
   *
   * @param transporterRepresentation the transporter data
   * @return ResponseEntity with created transporter or error
   */
  @PostMapping("transporter")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create a new transporter", notes = "Creates a new transporter for the authenticated tenant")
  public ResponseEntity<?> addTransporter(
    @ApiParam(value = "Transporter details", required = true) @RequestBody TransporterRepresentation transporterRepresentation) {
    try {
      if (transporterRepresentation.getTransporterName() == null || transporterRepresentation.getTransporterName().isBlank()) {
        log.error("Invalid transporter input - transporter name is required!");
        throw new RuntimeException("Transporter name is required!");
      }
      
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      
      TransporterRepresentation createdTransporter = transporterService.createTransporter(tenantId, transporterRepresentation);
      return new ResponseEntity<>(createdTransporter, HttpStatus.CREATED);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "addTransporter");
    }
  }
  
  /**
   * Retrieves all transporters for the authenticated tenant.
   *
   * @param page the page number (optional)
   * @param size the page size (optional)
   * @return ResponseEntity with transporters or error
   */
  @GetMapping("transporters")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all transporters", notes = "Retrieves all transporters for the authenticated tenant with optional pagination")
  public ResponseEntity<?> getAllTransportersOfTenant(
    @ApiParam(value = "Page number (0-based)", example = "0") @RequestParam(value = "page") String page,
    @ApiParam(value = "Page size", example = "10") @RequestParam(value = "size") String size) {
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      
      Integer pageNumber = (page == null || page.isBlank()) ? -1
        : GenericResourceUtils.convertResourceIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page=" + page));
      
      Integer sizeNumber = (size == null || size.isBlank()) ? -1
        : GenericResourceUtils.convertResourceIdToInt(size)
        .orElseThrow(() -> new RuntimeException("Invalid size=" + size));
      
      if (pageNumber == -1 || sizeNumber == -1) {
        return ResponseEntity.ok(transporterService.getAllTransportersOfTenantWithoutPagination(tenantId));
      }
      
      Page<TransporterRepresentation> transporters = transporterService.getAllTransportersOfTenant(tenantId, pageNumber, sizeNumber);
      return ResponseEntity.ok(transporters);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getAllTransportersOfTenant");
    }
  }
  
  /**
   * Updates an existing transporter.
   *
   * @param transporterId the transporter ID
   * @param transporterRepresentation the updated transporter data
   * @return ResponseEntity with updated transporter or error
   */
  @PutMapping("transporter/{transporterId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update a transporter", notes = "Updates an existing transporter for the authenticated tenant")
  public ResponseEntity<?> updateTransporter(
    @ApiParam(value = "Identifier of the transporter", required = true) @PathVariable("transporterId") String transporterId,
    @ApiParam(value = "Updated transporter details", required = true) @RequestBody TransporterRepresentation transporterRepresentation) {
    try {
      if (transporterId == null || transporterId.isBlank()) {
        log.error("Invalid input for transporter update - transporterId is required!");
        throw new RuntimeException("Transporter ID is required!");
      }
      
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      
      Long transporterIdLongValue = GenericResourceUtils.convertResourceIdToLong(transporterId)
        .orElseThrow(() -> new RuntimeException("Invalid transporter ID!"));
      
      TransporterRepresentation updatedTransporter = transporterService.updateTransporter(
        tenantId, transporterIdLongValue, transporterRepresentation);
      return ResponseEntity.ok(updatedTransporter);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateTransporter");
    }
  }
  
  /**
   * Deletes a transporter (soft delete).
   *
   * @param transporterId the transporter ID
   * @return ResponseEntity with no content or error
   */
  @DeleteMapping("transporter/{transporterId}")
  @ApiOperation(value = "Delete a transporter", notes = "Soft deletes a transporter for the authenticated tenant")
  public ResponseEntity<?> deleteTransporter(
    @ApiParam(value = "Identifier of the transporter", required = true) @PathVariable("transporterId") String transporterId) {
    try {
      if (transporterId == null || transporterId.isBlank()) {
        log.error("Invalid input for transporter delete - transporterId is required!");
        throw new RuntimeException("Transporter ID is required!");
      }
      
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      
      Long transporterIdLongValue = GenericResourceUtils.convertResourceIdToLong(transporterId)
        .orElseThrow(() -> new RuntimeException("Invalid transporter ID!"));
      
      transporterService.deleteTransporter(tenantId, transporterIdLongValue);
      return ResponseEntity.noContent().build();
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "deleteTransporter");
    }
  }
  
  /**
   * Searches for transporters by various criteria.
   *
   * @param searchType the search type (name, gstin, transporter_id)
   * @param searchQuery the search query
   * @return ResponseEntity with matching transporters or error
   */
  @GetMapping("transporters/search")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Search transporters", notes = "Search transporters by name, GSTIN, or transporter ID")
  public ResponseEntity<?> searchTransporters(
    @ApiParam(value = "Search type: name, gstin, or transporter_id", required = true, example = "name") 
    @RequestParam String searchType,
    @ApiParam(value = "Search query", required = true, example = "XYZ Transport") 
    @RequestParam String searchQuery) {
    try {
      if (searchType == null || searchQuery == null || searchQuery.isBlank()) {
        log.error("Invalid input for searchTransporters. SearchType: {}, SearchQuery: {}", searchType, searchQuery);
        throw new IllegalArgumentException("Search type and search query are required.");
      }
      
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      
      List<TransporterRepresentation> transporters = transporterService.searchTransporters(tenantId, searchType, searchQuery);
      TransporterListRepresentation transporterListRepresentation = TransporterListRepresentation.builder()
        .transporterRepresentations(transporters)
        .build();
      return ResponseEntity.ok(transporterListRepresentation);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "searchTransporters");
    }
  }
  
  /**
   * Retrieves a specific transporter by ID.
   *
   * @param transporterId the transporter ID
   * @return ResponseEntity with transporter details or error
   */
  @GetMapping("transporter/{transporterId}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get transporter by ID", notes = "Retrieves a specific transporter by its ID")
  public ResponseEntity<?> getTransporterById(
    @ApiParam(value = "Identifier of the transporter", required = true) @PathVariable("transporterId") String transporterId) {
    try {
      if (transporterId == null || transporterId.isBlank()) {
        log.error("Invalid input for getTransporterById - transporterId is required!");
        throw new RuntimeException("Transporter ID is required!");
      }
      
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      
      Long transporterIdLongValue = GenericResourceUtils.convertResourceIdToLong(transporterId)
        .orElseThrow(() -> new RuntimeException("Invalid transporter ID!"));
      
      // Get the transporter entity and convert to representation
      var transporter = transporterService.getTransporterByIdAndTenantId(transporterIdLongValue, tenantId);
      return ResponseEntity.ok(transporterService.searchTransporters(tenantId, "name", transporter.getTransporterName()).get(0));
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getTransporterById");
    }
  }
}

