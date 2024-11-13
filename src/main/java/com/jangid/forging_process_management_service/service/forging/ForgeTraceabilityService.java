package com.jangid.forging_process_management_service.service.forging;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeTraceabilityAssembler;
import com.jangid.forging_process_management_service.entities.forging.ForgeTraceability;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeTraceabilityRepresentation;
import com.jangid.forging_process_management_service.exception.forging.ForgeTraceabilityNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgingLineOccupiedException;
import com.jangid.forging_process_management_service.repositories.forging.ForgeTraceabilityRepository;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ForgeTraceabilityService {

  @Autowired
  private ForgeTraceabilityRepository forgeTraceabilityRepository;
  @Autowired
  private TenantService tenantService;

  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;

  @Autowired
  private RawMaterialService rawMaterialService;

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
    validateTenantExists(tenantId);
    ForgingLine forgingLine = getForgingLineUsingTenantIdAndForgingLineId(tenantId, forgingLineId);
    boolean isForgeTraceabilityAppliedOnForgingLine = isForgeTraceabilityAppliedOnForgingLine(forgingLineId);

    if (isForgeTraceabilityAppliedOnForgingLine) {
      log.error("ForgingLine={} is already having a forge traceability set. Can not create a new forge traceability on this forging line", forgingLineId);
      throw new ForgingLineOccupiedException("Can not create a new forge traceability on this forging line as ForgingLine " + forgingLineId + " is already having a forge traceability");

    }

    Heat heat = rawMaterialHeatService.getRawMaterialHeatByHeatNumberAndInvoiceNumber(tenantId, representation.getHeatNumber(), representation.getInvoiceNumber());
    double newHeatQuantity = heat.getAvailableHeatQuantity() - Float.valueOf(representation.getHeatIdQuantityUsed());
    log.info("Updating AvailableHeatQuantity for heat={} to {}", heat.getId(), newHeatQuantity);
    heat.setAvailableHeatQuantity(newHeatQuantity);
    rawMaterialHeatService.updateRawMaterialHeat(heat);

    ForgeTraceability inputForgeTraceability = forgeTraceabilityAssembler.assemble(representation);

    inputForgeTraceability.setForgingLine(forgingLine);
    inputForgeTraceability.setHeatId(heat.getId());
    inputForgeTraceability.setCreatedAt(LocalDateTime.now());
    ForgeTraceability createdForgeTraceability = forgeTraceabilityRepository.save(inputForgeTraceability);

    ForgeTraceabilityRepresentation createdRepresentation = forgeTraceabilityAssembler.dissemble(createdForgeTraceability);
    createdRepresentation.setHeatNumber(heat.getHeatNumber());
    createdRepresentation.setForgingStatus(ForgeTraceability.ForgeTraceabilityStatus.IDLE.name());
    createdRepresentation.setInvoiceNumber(representation.getInvoiceNumber());
    return createdRepresentation;
  }

  @Transactional
  public ForgeTraceabilityRepresentation updateForgeTraceability(long tenantId, long forgingLineId, long forgeTraceabilityId, ForgeTraceabilityRepresentation representation) {
    validateTenantExists(tenantId);
    ForgingLine forgingLine = getForgingLineUsingTenantIdAndForgingLineId(tenantId, forgingLineId);
    boolean isForgeTraceabilityAppliedOnForgingLine = isForgeTraceabilityAppliedOnForgingLine(forgingLine.getId());

    if (!isForgeTraceabilityAppliedOnForgingLine) {
      log.error("ForgingLine={} does not have a forge traceability set. Can not edit forge traceability on this forging line as it does not have forge traceability", forgingLineId);
      throw new ForgeTraceabilityNotFoundException("ForgeTraceability does not exists for forgingLine!");
    }
    ForgeTraceability traceability = getForgeTraceabilityById(forgeTraceabilityId);

    traceability.setForgePieceWeight(Float.valueOf(representation.getForgePieceWeight()));
    traceability.setForgingStatus(ForgeTraceability.ForgeTraceabilityStatus.valueOf(representation.getForgingStatus()));
    traceability.setActualForgeCount(representation.getActualForgeCount());
    traceability.setStartAt(ConvertorUtils.convertStringToLocalDateTime(representation.getStartAt()));
    traceability.setEndAt(ConvertorUtils.convertStringToLocalDateTime(representation.getEndAt()));

    ForgeTraceability updatedForgeTraceability = forgeTraceabilityRepository.save(traceability);

    if (ForgeTraceability.ForgeTraceabilityStatus.IN_PROGRESS.name().equals(representation.getForgingStatus())) {
      forgingLine.setForgingStatus(ForgingLine.ForgingLineStatus.RUNNING);
    } else {
      forgingLine.setForgingStatus(ForgingLine.ForgingLineStatus.NOT_RUNNING);
    }
    forgingLineService.saveForgingLine(forgingLine);

    return forgeTraceabilityAssembler.dissemble(updatedForgeTraceability);
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

  @Transactional
  public void deleteForgeTraceability(ForgeTraceability forgeTraceability) {
    forgeTraceability.setDeleted(true);
    forgeTraceability.setDeletedAt(LocalDateTime.now());
    forgeTraceabilityRepository.save(forgeTraceability);
  }

  private void validateTenantExists(long tenantId){
    boolean isTenantExists = tenantService.isTenantExists(tenantId);
    if(!isTenantExists){
      log.error("Tenant with id="+tenantId+" not found!");
      throw new ResourceNotFoundException("Tenant with id="+tenantId+" not found!");
    }
  }

  public ForgeTraceability getForgeTraceabilityById(long forgeTraceabilityId){
    Optional<ForgeTraceability> forgeTraceabilityOptional = forgeTraceabilityRepository.findByIdAndDeletedFalse(forgeTraceabilityId);
    if (forgeTraceabilityOptional.isEmpty()) {
      log.error("ForgeTraceability does not exists for forgeTraceabilityId={}", forgeTraceabilityId);
      throw new ForgeTraceabilityNotFoundException("ForgeTraceability does not exists for forgeTraceabilityId="+forgeTraceabilityId);
    }
    return forgeTraceabilityOptional.get();
  }

  public List<ForgeTraceability> getForgeTraceabilitiesByHeatNumber(long tenantId, String heatNumber){
//    return rawMaterialService.getRawMaterialByHeatNumber(tenantId, heatNumber).stream()
//        .flatMap(rm -> rm.getHeats().stream())
//        .filter(rmh -> heatNumber.equals(rmh.getHeatNumber()))
//        .flatMap(rmh -> getForgeTraceabilitiesByHeatId(rmh.getId()).stream())
//        .collect(Collectors.toList());
    return null;
  }

  public List<ForgeTraceability> getForgeTraceabilitiesByHeatId(long heatId){
    return forgeTraceabilityRepository.findByHeatIdAndDeletedFalse(heatId);
  }
}

