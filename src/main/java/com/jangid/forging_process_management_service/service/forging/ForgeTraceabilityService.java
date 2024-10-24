package com.jangid.forging_process_management_service.service.forging;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeTraceabilityAssembler;
import com.jangid.forging_process_management_service.entities.forging.ForgeTraceability;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterialHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeTraceabilityRepresentation;
import com.jangid.forging_process_management_service.exception.forging.ForgeTraceabilityNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgingLineOccupiedException;
import com.jangid.forging_process_management_service.repositories.forging.ForgeTraceabilityRepository;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

  @Autowired
  private ForgeTraceabilityAssembler forgeTraceabilityAssembler;

  public List<ForgeTraceability> getAllForgeTraceabilitiesByForgingId(long forgingId) {
    return forgeTraceabilityRepository.findByForgingLineIdAndDeletedFalse(forgingId);
  }

  public Optional<ForgeTraceability> getForgeTraceabilityById(Long id) {
    return forgeTraceabilityRepository.findById(id);
  }

  @Transactional
  public ForgeTraceabilityRepresentation createForgeTraceability(long tenantId, long forgingLineId, ForgeTraceabilityRepresentation representation) {

    ForgingLine forgingLine = getForgingLineUsingTenantIdAndForgingLineId(tenantId, forgingLineId);
    boolean isForgeTraceabilityAppliedOnForgingLine = isForgeTraceabilityAppliedOnForgingLine(forgingLineId);

    if (isForgeTraceabilityAppliedOnForgingLine) {
      log.error("ForgingLine={} is already having a forge traceability set. Can not create a new forge traceability on this forging line", forgingLineId);
      throw new ForgingLineOccupiedException("Can not create a new forge traceability on this forging line as ForgingLine " + forgingLineId + " is already having a forge traceability");

    }

    RawMaterialHeat heat = rawMaterialHeatService.getRawMaterialHeatByHeatNumberAndInvoiceNumber(tenantId, representation.getHeatNumber(), representation.getInvoiceNumber());
    float newHeatQuantity = heat.getAvailableHeatQuantity() - Float.valueOf(representation.getHeatIdQuantityUsed());
    log.info("Updating AvailableHeatQuantity for heat={} to {}", heat.getId(), newHeatQuantity);
    heat.setAvailableHeatQuantity(newHeatQuantity);
    rawMaterialHeatService.updateRawMaterialHeat(heat);

    ForgeTraceability inputForgeTraceability = forgeTraceabilityAssembler.assemble(representation);

    inputForgeTraceability.setForgingLine(forgingLine);
    inputForgeTraceability.setHeatId(heat.getId());
    inputForgeTraceability.setCreatedAt(LocalDateTime.now());
    ForgeTraceability createdForgeTraceability = forgeTraceabilityRepository.save(inputForgeTraceability);

    ForgeTraceabilityRepresentation createdRepresentation = forgeTraceabilityAssembler.dissemble(createdForgeTraceability);
    createdRepresentation.setHeatNumber(heat.heatNumber);
    createdRepresentation.setForgingStatus(ForgeTraceability.ForgeTraceabilityStatus.IDLE.name());
    createdRepresentation.setInvoiceNumber(representation.getInvoiceNumber());
    return createdRepresentation;
  }

  public ForgingLine getForgingLineUsingTenantIdAndForgingLineId(long tenantId, long forgingLineId){
    boolean isForgingLineOfTenantExists = forgingLineService.isForgingLineByTenantExists(tenantId);
    if (!isForgingLineOfTenantExists){
      log.error("Forging Line={} for the tenant={} does not exist!", forgingLineId, tenantId);
      throw new ResourceNotFoundException("Forging Line for the tenant does not exist!");
    }
    return forgingLineService.getForgingLineById(forgingLineId);
  }

  public boolean isForgeTraceabilityAppliedOnForgingLine(long forgingLineId){
    Optional<ForgeTraceability> forgeTraceabilityOptional = forgeTraceabilityRepository.findAppliedForgingTraceabilityOnForgingLine(forgingLineId);
    if(forgeTraceabilityOptional.isPresent()){
      log.info("ForgeTraceability={} already applied on forgingLineId={}", forgeTraceabilityOptional.get().getId(), forgingLineId);
      return true;
    }
    return false;
  }

  public ForgeTraceability getForgeTraceabilityByForgingLine(long forgingLineId){
    Optional<ForgeTraceability> forgeTraceabilityOptional = forgeTraceabilityRepository.findAppliedForgingTraceabilityOnForgingLine(forgingLineId);
    if (forgeTraceabilityOptional.isEmpty()) {
      log.error("ForgeTraceability does not exists for forgingLineId={}", forgingLineId);
      throw new ForgeTraceabilityNotFoundException("ForgeTraceability does not exists for forgingLine!");
    }
    return forgeTraceabilityOptional.get();
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

  @Transactional
  public void deleteForgeTraceability(ForgeTraceability forgeTraceability) {
    forgeTraceability.setDeleted(true);
    forgeTraceability.setDeletedAt(LocalDateTime.now());
    forgeTraceabilityRepository.save(forgeTraceability);
  }
}

