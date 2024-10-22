package com.jangid.forging_process_management_service.service.forging;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeTraceabilityAssembler;
import com.jangid.forging_process_management_service.entities.forging.ForgeTraceability;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterialHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeTraceabilityRepresentation;
import com.jangid.forging_process_management_service.repositories.forging.ForgeTraceabilityRepository;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ForgeTraceabilityService {

  @Autowired
  private ForgeTraceabilityRepository forgeTraceabilityRepository;

  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;

  @Autowired
  private ForgingLineService forgingLineService;

  public List<ForgeTraceability> getAllForgeTraceabilitiesByForgingId(long forgingId) {
    return forgeTraceabilityRepository.findByForgingLineIdAndDeletedFalse(forgingId);
  }

  public Optional<ForgeTraceability> getForgeTraceabilityById(Long id) {
    return forgeTraceabilityRepository.findById(id);
  }

  @Transactional
  public ForgeTraceabilityRepresentation createForgeTraceability(long forgingLineId, ForgeTraceabilityRepresentation representation) {
    RawMaterialHeat heat = rawMaterialHeatService.getRawMaterialHeatById(Long.valueOf(representation.getHeatId()));
    heat.setAvailableHeatQuantity(heat.getHeatQuantity() - Float.valueOf(representation.getHeatIdQuantityUsed()));
    rawMaterialHeatService.updateRawMaterialHeat(heat);

    ForgeTraceability forgeTraceability = ForgeTraceabilityAssembler.assemble(representation);
    ForgingLine forgingLine = forgingLineService.getForgingLineById(forgingLineId);
    forgeTraceability.setForgingLine(forgingLine);
    ForgeTraceability createdForgeTraceability = forgeTraceabilityRepository.save(forgeTraceability);

    ForgeTraceabilityRepresentation createdRepresentation = ForgeTraceabilityAssembler.dissemble(createdForgeTraceability);
    createdRepresentation.setHeatNumber(heat.heatNumber);
    createdRepresentation.setForgingStatus(ForgingLine.ForgingLineStatus.IN_PROGRESS.name());
    return createdRepresentation;
  }

  public ForgeTraceability updateForgeTraceability(Long id, ForgeTraceability updatedForgeTraceability) {
    return forgeTraceabilityRepository.findById(id).map(forgeTraceability -> {
      forgeTraceability.setHeatId(updatedForgeTraceability.getHeatId());
      forgeTraceability.setHeatIdQuantityUsed(updatedForgeTraceability.getHeatIdQuantityUsed());
      forgeTraceability.setStartAt(updatedForgeTraceability.getStartAt());
      forgeTraceability.setEndAt(updatedForgeTraceability.getEndAt());
      forgeTraceability.setForgePieceWeight(updatedForgeTraceability.getForgePieceWeight());
      forgeTraceability.setActualForgeCount(updatedForgeTraceability.getActualForgeCount());
      forgeTraceability.setForgingStatus(updatedForgeTraceability.getForgingStatus());
      return forgeTraceabilityRepository.save(forgeTraceability);
    }).orElseThrow(() -> new ResourceNotFoundException("ForgeTraceability not found"));
  }

  public void deleteForgeTraceability(Long id) {
    forgeTraceabilityRepository.deleteById(id);
  }
}

