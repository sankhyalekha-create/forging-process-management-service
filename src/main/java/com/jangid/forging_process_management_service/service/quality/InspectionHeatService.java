package com.jangid.forging_process_management_service.service.quality;

import com.jangid.forging_process_management_service.entities.quality.InspectionHeat;
import com.jangid.forging_process_management_service.repositories.quality.InspectionHeatRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InspectionHeatService {

  @Autowired
  private InspectionHeatRepository inspectionHeatRepository;

  public InspectionHeat saveInspectionHeat(InspectionHeat inspectionHeat) {
    return inspectionHeatRepository.save(inspectionHeat);
  }

  public InspectionHeat getInspectionHeatById(Long id) {
    return inspectionHeatRepository.findById(id).orElse(null);
  }
} 