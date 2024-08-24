package com.jangid.forging_process_management_service.service;

import com.self.processmanagement.entities.RawMaterial;
import com.self.processmanagement.repositories.RawMaterialRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RawMaterialService {
  private final RawMaterialRepository rawMaterialRepository;

  public RawMaterialService(RawMaterialRepository rawMaterialRepository) {
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
      log.error("RawMaterial with id="+materialId+" not found!");
      throw new RuntimeException("RawMaterial with id="+materialId+" not found!");
    }
    return optionalRawMaterial.get();
  }
}
