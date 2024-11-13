package com.jangid.forging_process_management_service.service.inventory;

import com.jangid.forging_process_management_service.entities.inventory.RawMaterial;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialHeatListRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.inventory.RawMaterialHeatRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RawMaterialHeatService {

  @Autowired
  private RawMaterialHeatRepository rawMaterialHeatRepository;

  @Autowired
  private RawMaterialService rawMaterialService;

  public Heat getRawMaterialHeatByHeatNumberAndInvoiceNumber(long tenantId, String heatNumber, String invoiceNumber){
    RawMaterial rawMaterial = rawMaterialService.getRawMaterialByInvoiceNumber(tenantId, invoiceNumber);
    Optional<Heat> rawMaterialHeatOptional = rawMaterialHeatRepository.findByHeatNumberAndRawMaterialProductIdAndDeletedFalse(heatNumber, rawMaterial.getId());
    if(rawMaterialHeatOptional.isEmpty()){
      log.error("RawMaterialHeat with heatNumber="+heatNumber+" not found for tenant="+tenantId);
      throw new ResourceNotFoundException("RawMaterialHeat with heatNumber="+heatNumber+" not found for tenant="+tenantId);
    }
    return rawMaterialHeatOptional.get();
  }

  public Heat getRawMaterialHeatById(long heatId){
    Optional<Heat> rawMaterialHeatOptional = rawMaterialHeatRepository.findByIdAndDeletedFalse(heatId);
    if(rawMaterialHeatOptional.isEmpty()){
      log.error("RawMaterialHeat with heatId="+heatId+" not found!");
      throw new ResourceNotFoundException("RawMaterialHeat with heatId="+heatId+" not found!");
    }
    return rawMaterialHeatOptional.get();
  }

  @Transactional
  public void updateRawMaterialHeat(Heat heat){
    rawMaterialHeatRepository.save(heat);
  }

  public RawMaterialHeatListRepresentation getRawMaterialHeatListRepresentation(List<Heat> heats) {
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
    return RawMaterialHeatListRepresentation.builder().build();
  }
}
