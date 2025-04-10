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
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class RawMaterialHeatService {

  @Autowired
  private HeatRepository heatRepository;

  @Autowired
  private ProductService productService;

  public Heat getRawMaterialHeatByHeatNumberAndTenantId(String heatNumber, long tenantId) {
    Optional<Heat> heatOptional = heatRepository.findHeatByHeatNumberAndTenantId(heatNumber, tenantId);
    if (heatOptional.isEmpty()) {
      log.error("Heat with heatNumber=" + heatNumber + " not found for tenant=" + tenantId);
      throw new ResourceNotFoundException("Heat with heatNumber=" + heatNumber + " not found for tenant=" + tenantId);
    }
    return heatOptional.get();
  }

  public Heat getRawMaterialHeatById(long heatId) {
    Optional<Heat> rawMaterialHeatOptional = heatRepository.findByIdAndDeletedFalse(heatId);
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

  @Transactional
  public void returnHeatsInBatch(Map<Long, Double> heatQuantitiesToUpdate) {
    // Use a single update query to update quantities in bulk
    heatQuantitiesToUpdate.forEach((heatId, returnedQuantity) -> {
      heatRepository.incrementAvailableHeatQuantity(heatId, returnedQuantity);
    });
  }

  public Heat getHeatById(long heatId){
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
}
