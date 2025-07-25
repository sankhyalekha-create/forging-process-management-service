package com.jangid.forging_process_management_service.service.inventory;

import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.product.Product;
import com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatListRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.inventory.HeatNotFoundException;
import com.jangid.forging_process_management_service.repositories.inventory.HeatRepository;
import com.jangid.forging_process_management_service.service.product.ProductService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RawMaterialHeatService {

  @Autowired
  private HeatRepository heatRepository;

  @Autowired
  private ProductService productService;

  public Heat getRawMaterialHeatById(long heatId) {
    Optional<Heat> rawMaterialHeatOptional = heatRepository.findByIdAndActiveTrueAndDeletedFalse(heatId);
    if (rawMaterialHeatOptional.isEmpty()) {
      log.error("RawMaterialHeat with heatId=" + heatId + " not found!");
      throw new ResourceNotFoundException("RawMaterialHeat with heatId=" + heatId + " not found!");
    }
    return rawMaterialHeatOptional.get();
  }

  @Transactional
  public void updateRawMaterialHeat(Heat heat) {
    heatRepository.save(heat);
  }

  public HeatListRepresentation getRawMaterialHeatListRepresentation(List<Heat> heats) {
//    if (heats == null) {
//      log.error("RawMaterialHeat list is null!");
//      return RawMaterialHeatListRepresentation.builder().build();
//    }
//    List<RawMaterialHeatRepresentation> rawMaterialHeatRepresentation = new ArrayList<>();
//    heats.forEach(rmh -> {
//      RawMaterialHeatRepresentation heatRepresentation = RawMaterialHeatAssembler.dissemble(rmh);
//      heatRepresentation.setRawMaterialId(String.valueOf(rmh.getRawMaterial().getId()));
//      heatRepresentation.setRawMaterialInvoiceNumber(String.valueOf(rmh.getRawMaterial().getRawMaterialInvoiceNumber()));
//      rawMaterialHeatRepresentation.add(heatRepresentation);
//    });
//    return RawMaterialHeatListRepresentation.builder()
//        .rawMaterialHeats(rawMaterialHeatRepresentation).build();
    return HeatListRepresentation.builder().build();
  }

  public List<Heat> getProductHeats(long tenantId, long productId) {
    Product product = productService.getProductById(productId);
    if (UnitOfMeasurement.PIECES.equals(product.getUnitOfMeasurement())) {
      return heatRepository.findHeatsHavingPiecesByProductIdAndTenantId(productId, tenantId);
    }
    return heatRepository.findHeatsHavingQuantitiesByProductIdAndTenantId(productId, tenantId);
  }

  /**
   * Get product heats filtered by active status
   */
  public List<Heat> getProductHeatsByActiveStatus(long tenantId, long productId, boolean active) {
    Product product = productService.getProductById(productId);
    
    if (active) {
      // Return active heats
      if (UnitOfMeasurement.PIECES.equals(product.getUnitOfMeasurement())) {
        return heatRepository.findHeatsHavingPiecesByProductIdAndTenantId(productId, tenantId);
      }
      return heatRepository.findHeatsHavingQuantitiesByProductIdAndTenantId(productId, tenantId);
    } else {
      // Return inactive heats
      if (UnitOfMeasurement.PIECES.equals(product.getUnitOfMeasurement())) {
        return heatRepository.findInactiveHeatsHavingPiecesByProductIdAndTenantId(productId, tenantId);
      }
      return heatRepository.findInactiveHeatsHavingQuantitiesByProductIdAndTenantId(productId, tenantId);
    }
  }

  public Heat getHeatById(long heatId){
    Optional<Heat> heatOptional = heatRepository.findByIdAndActiveTrueAndDeletedFalse(heatId);
    if (heatOptional.isEmpty()) {
      log.error("Heat does not exists for heatId={}", heatId);
      throw new HeatNotFoundException("Heat does not exists for heatId=" + heatId);
    }
    return heatOptional.get();
  }

  /**
   * Get heat by ID regardless of active status for status management operations
   */
  public Heat getHeatByIdForStatusManagement(long heatId){
    Optional<Heat> heatOptional = heatRepository.findByIdAndDeletedFalse(heatId);
    if (heatOptional.isEmpty()) {
      log.error("Heat does not exists for heatId={}", heatId);
      throw new HeatNotFoundException("Heat does not exists for heatId=" + heatId);
    }
    return heatOptional.get();
  }

  public Page<Heat> getTenantHeats(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return heatRepository.findHeatsByTenantId(tenantId, pageable);
  }

  public List<Heat> getAllTenantHeats(long tenantId) {
    return heatRepository.findAllHeatsByTenantId(tenantId);
  }


  /**
   * Get inactive heats for a tenant with pagination
   */
  public Page<Heat> getInactiveTenantHeats(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return heatRepository.findHeatsByTenantIdAndActiveStatus(tenantId, false, pageable);
  }

  /**
   * Get all inactive heats for a tenant
   */
  public List<Heat> getAllInactiveTenantHeats(long tenantId) {
    return heatRepository.findAllHeatsByTenantIdAndActiveStatus(tenantId, false);
  }

  /**
   * Mark multiple heats as active
   */
  @Transactional
  public void markHeatsAsActive(List<Long> heatIds) {
    for (Long heatId : heatIds) {
      // Verify heat exists first
      Heat heat = getHeatByIdForStatusManagement(heatId);
      heatRepository.updateHeatActiveStatus(heatId, true);
    }
    log.info("Heats with ids={} have been marked as active", heatIds);
  }

  /**
   * Mark multiple heats as inactive
   */
  @Transactional
  public void markHeatsAsInactive(List<Long> heatIds) {
    for (Long heatId : heatIds) {
      // Verify heat exists first
      Heat heat = getHeatByIdForStatusManagement(heatId);
      heatRepository.updateHeatActiveStatus(heatId, false);
    }
    log.info("Heats with ids={} have been marked as inactive", heatIds);
  }

}
