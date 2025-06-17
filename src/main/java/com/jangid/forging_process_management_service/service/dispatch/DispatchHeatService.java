package com.jangid.forging_process_management_service.service.dispatch;

import com.jangid.forging_process_management_service.entities.dispatch.DispatchHeat;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchHeatRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DispatchHeatService {

  @Autowired
  private DispatchHeatRepository dispatchHeatRepository;

  public DispatchHeat saveDispatchHeat(DispatchHeat dispatchHeat) {
    return dispatchHeatRepository.save(dispatchHeat);
  }

  public DispatchHeat getDispatchHeatById(Long id) {
    return dispatchHeatRepository.findById(id).orElse(null);
  }
} 