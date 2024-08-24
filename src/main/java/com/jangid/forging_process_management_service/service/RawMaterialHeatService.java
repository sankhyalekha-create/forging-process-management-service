package com.jangid.forging_process_management_service.service;

import com.jangid.forging_process_management_service.entities.RawMaterial;
import com.jangid.forging_process_management_service.repositories.RawMaterialRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RawMaterialHeatService {
  private final RawMaterialRepository rawMaterialRepository;

  public RawMaterialHeatService(RawMaterialRepository rawMaterialRepository) {
    this.rawMaterialRepository = rawMaterialRepository;
  }


  @Transactional
  public RawMaterial addRawMaterial(RawMaterial rawMaterial) {
    return rawMaterialRepository.save(rawMaterial);
  }

  public List<RawMaterial> getAllRawMaterialsOfTenant(long tenantId){
    return rawMaterialRepository.findByTenantId(tenantId);
  }

  public RawMaterial getRawMaterialById(long materialId){
    Optional<RawMaterial> optionalRawMaterial = rawMaterialRepository.findById(materialId);
    if (optionalRawMaterial.isEmpty()){
      throw new RuntimeException("RawMaterial with id="+materialId+" not found!");
    }
    return optionalRawMaterial.get();
  }
}
