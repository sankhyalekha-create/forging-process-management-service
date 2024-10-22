package com.jangid.forging_process_management_service.service.inventory;

import com.jangid.forging_process_management_service.entities.inventory.RawMaterialHeat;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.inventory.RawMaterialHeatRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class RawMaterialHeatService {

  @Autowired
  private RawMaterialHeatRepository rawMaterialHeatRepository;

  public RawMaterialHeat getRawMaterialHeatById(long heatId){
    Optional<RawMaterialHeat> rawMaterialHeatOptional = rawMaterialHeatRepository.findByIdAndDeletedFalse(heatId);
    if(rawMaterialHeatOptional.isEmpty()){
      log.error("RawMaterialHeat with heatId="+heatId+" not found!");
      throw new ResourceNotFoundException("RawMaterialHeat with heatId="+heatId+" not found!");
    }
    return rawMaterialHeatOptional.get();
  }

  @Transactional
  public void updateRawMaterialHeat(RawMaterialHeat heat){
    rawMaterialHeatRepository.save(heat);
  }


}
