package com.jangid.forging_process_management_service.service.inventory;

import com.jangid.forging_process_management_service.entities.inventory.RawMaterial;
import com.jangid.forging_process_management_service.repositories.inventory.RawMaterialRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
