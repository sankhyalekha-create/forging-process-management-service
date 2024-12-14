package com.jangid.forging_process_management_service.service.forging;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.forging.ForgeHeat;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotInExpectedStatusException;
import com.jangid.forging_process_management_service.exception.forging.ForgingLineOccupiedException;
import com.jangid.forging_process_management_service.repositories.forging.ForgeRepository;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ForgeService {

  @Autowired
  private ForgeRepository forgeRepository;
  @Autowired
  private TenantService tenantService;

  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;

  @Autowired
  private ItemService itemService;

  @Autowired
  private ForgingLineService forgingLineService;

  @Autowired
  private ForgeAssembler forgeAssembler;



  public List<Forge> getAllForges(long tenantId) {
    List<ForgingLine> forgingLines = forgingLineService.getAllForgingLinesByTenant(tenantId);
    return forgingLines.stream()
        .map(forgingLine -> forgeRepository.findByForgingLineIdAndDeletedFalseOrderByUpdatedAtDesc(forgingLine.getId()))
        .flatMap(Collection::stream)
        .toList();
  }

  public Forge getForgeByIdAndForgingLineId(Long id, Long forgingLineId) {
    Optional<Forge> forgeOptional = forgeRepository.findByIdAndAndForgingLineIdAndDeletedFalse(id, forgingLineId);
    if (forgeOptional.isEmpty()) {
      log.error("Forge does not exists for forgeId={} having forgingLineId={}", id, forgingLineId);
      throw new ForgeNotFoundException("Forge does not exists for forgeId=" + id + " having forgingLineId=" + forgingLineId);
    }
    return forgeOptional.get();
  }

  @Transactional // Ensures all database operations succeed or roll back
  public ForgeRepresentation applyForge(long tenantId, long forgingLineId, ForgeRepresentation representation) {
    tenantService.validateTenantExists(tenantId);
    ForgingLine forgingLine = getForgingLineUsingTenantIdAndForgingLineId(tenantId, forgingLineId);
    boolean isForgeAppliedOnForgingLine = isForgeAppliedOnForgingLine(forgingLineId);

    if (isForgeAppliedOnForgingLine) {
      log.error("ForgingLine={} is already having a forge set. Cannot create a new forge on this forging line", forgingLineId);
      throw new ForgingLineOccupiedException("Cannot create a new forge on this forging line as ForgingLine " + forgingLineId + " is already occupied");
    }

    // Update heat quantities
    representation.getForgeHeats().forEach(forgeHeat -> {
      Heat heat = rawMaterialHeatService.getRawMaterialHeatByHeatNumberAndTenantId(forgeHeat.getHeat().getHeatNumber(), tenantId);
      double newHeatQuantity = heat.getAvailableHeatQuantity() - Double.parseDouble(forgeHeat.getHeatQuantityUsed());
      if (newHeatQuantity < 0) {
        log.error("Insufficient heat quantity for heat={} on tenantId={}", heat.getId(), tenantId);
        throw new IllegalArgumentException("Insufficient heat quantity for heat " + heat.getId());
      }
      log.info("Updating AvailableHeatQuantity for heat={} to {}", heat.getId(), newHeatQuantity);
      heat.setAvailableHeatQuantity(newHeatQuantity);
      rawMaterialHeatService.updateRawMaterialHeat(heat); // Persist the updated heat
    });

    // Create and save the forge
    Forge inputForge = forgeAssembler.createAssemble(representation);
    inputForge.getForgeHeats().forEach(forgeHeat -> forgeHeat.setForge(inputForge));
    inputForge.setForgingLine(forgingLine);

    inputForge.setCreatedAt(LocalDateTime.now());
    inputForge.setForgeTraceabilityNumber(getForgeTraceabilityNumber(tenantId, forgingLine.getForgingLineName(), forgingLineId));

    Item item = itemService.getItemByIdAndTenantId(representation.getProcessedItem().getItem().getId(), tenantId);
    Double itemWeight = item.getItemWeight();
    Double totalHeatReserved = representation.getForgeHeats().stream()
        .mapToDouble(forgeHeat -> Double.parseDouble(forgeHeat.getHeatQuantityUsed()))
        .sum();

    ProcessedItem processedItem = ProcessedItem.builder()
        .item(item)
        .forge(inputForge)
        .itemStatus(ItemStatus.FORGING_NOT_STARTED)
        .expectedForgePiecesCount((int) Math.floor(totalHeatReserved / itemWeight))
        .createdAt(LocalDateTime.now())
        .build();

    inputForge.setProcessedItem(processedItem);

    Forge createdForge = forgeRepository.save(inputForge); // Save forge entity
    forgingLine.setForgingLineStatus(ForgingLine.ForgingLineStatus.FORGE_APPLIED);
    forgingLineService.saveForgingLine(forgingLine);

    // Return the created forge representation
    ForgeRepresentation createdRepresentation = forgeAssembler.dissemble(createdForge);
    createdRepresentation.setForgingStatus(Forge.ForgeStatus.IDLE.name());
    return createdRepresentation;
  }

  private String getForgeTraceabilityNumber(long tenantId, String forgingLineName, long forgingLineId) {
    Tenant tenant = tenantService.getTenantById(tenantId);
    String initialsOfTenant = getInitialsOfTenant(tenant.getTenantName());
    String localDate = LocalDate.now().toString();
    Optional<Forge> lastForgeOnForgingLineOptional = forgeRepository.findLastForgeOnForgingLine(forgingLineId);

    int counter = lastForgeOnForgingLineOptional
        .map(forge -> Integer.parseInt(forge.getForgeTraceabilityNumber()
                                           .substring(forge.getForgeTraceabilityNumber().lastIndexOf("-") + 1)) + 1)
        .orElse(1);

    return initialsOfTenant + forgingLineName + localDate + "-" + counter;
  }

  private String getInitialsOfTenant(String tenantName) {
    if (tenantName == null || tenantName.isEmpty()) {
      return "";
    }
    return Arrays.stream(tenantName.trim().split("\\s+"))
        .map(word -> String.valueOf(Character.toUpperCase(word.charAt(0))))
        .collect(Collectors.joining());
  }

  @Transactional
  public ForgeRepresentation startForge(long tenantId, long forgingLineId, long forgeId, String startAt) {
    tenantService.validateTenantExists(tenantId);
    ForgingLine forgingLine = getForgingLineUsingTenantIdAndForgingLineId(tenantId, forgingLineId);
    boolean isForgeAppliedOnForgingLine = isForgeAppliedOnForgingLine(forgingLine.getId());

    if (!isForgeAppliedOnForgingLine) {
      log.error("ForgingLine={} does not have a forge existingForge set. Can not start forge existingForge on this forging line as it does not have forge existingForge", forgingLineId);
      throw new ForgeNotFoundException("Forge does not exists for forgingLine!");
    }
    Forge existingForge = getForgeById(forgeId);

    if (existingForge.getStartAt() != null) {
      log.error("The forge={} having traceability={} has already been started!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new ForgeNotInExpectedStatusException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + "has already been started!");
    }

    if (!Forge.ForgeStatus.IDLE.equals(existingForge.getForgingStatus())) {
      log.error("The forge={} having traceability={} is not in IDLE status to start it!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new ForgeNotInExpectedStatusException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + "Not in IDLE status to start it!");
    }

    existingForge.setForgingStatus(Forge.ForgeStatus.IN_PROGRESS);
    existingForge.setStartAt(ConvertorUtils.convertStringToLocalDateTime(startAt));
    existingForge.getProcessedItem().setItemStatus(ItemStatus.FORGING_IN_PROGRESS);

    Forge startedForge = forgeRepository.save(existingForge);

    forgingLine.setForgingLineStatus(ForgingLine.ForgingLineStatus.FORGE_IN_PROGRESS);
    forgingLineService.saveForgingLine(forgingLine);

    return forgeAssembler.dissemble(startedForge);
  }

  @Transactional
  public ForgeRepresentation endForge(long tenantId, long forgingLineId, long forgeId, ForgeRepresentation representation) {
    tenantService.validateTenantExists(tenantId);
    int actualForgedPieces = getActualForgedPieces(representation.getActualForgeCount());
    ForgingLine forgingLine = getForgingLineUsingTenantIdAndForgingLineId(tenantId, forgingLineId);
    boolean isForgeAppliedOnForgingLine = isForgeAppliedOnForgingLine(forgingLine.getId());

    if (!isForgeAppliedOnForgingLine) {
      log.error("ForgingLine={} does not have a forge existingForge set. Can not end forge existingForge on this forging line as it does not have forge existingForge", forgingLineId);
      throw new ForgeNotFoundException("Forge does not exists for forgingLine!");
    }
    Forge existingForge = getForgeById(forgeId);

    if (existingForge.getEndAt() != null) {
      log.error("The forge={} having traceability={} has already been ended!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new ForgeNotInExpectedStatusException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + "has already been ended!");
    }

    if (!Forge.ForgeStatus.IN_PROGRESS.equals(existingForge.getForgingStatus())) {
      log.error("The forge={} having traceability={} is not in IN_PROGRESS status to end it!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new ForgeNotInExpectedStatusException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + "Not in IN_PROGRESS status to end it!");
    }
    LocalDateTime endAt = ConvertorUtils.convertStringToLocalDateTime(representation.getEndAt());
    if (existingForge.getStartAt().compareTo(endAt) >= 0) {
      log.error("The forge={} having traceability={} end time is before or equal to start time!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new RuntimeException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + " end time is before or equal to start time!");
    }

    existingForge.setForgingStatus(Forge.ForgeStatus.COMPLETED);
    ProcessedItem existingForgeProcessedItem = existingForge.getProcessedItem();
    existingForgeProcessedItem.setItemStatus(ItemStatus.FORGING_COMPLETED);
    existingForgeProcessedItem.setActualForgePiecesCount(actualForgedPieces);
    existingForge.setProcessedItem(existingForgeProcessedItem);
    existingForge.setEndAt(endAt);

    Forge completedForge = forgeRepository.save(existingForge);

    forgingLine.setForgingLineStatus(ForgingLine.ForgingLineStatus.FORGE_NOT_APPLIED);
    forgingLineService.saveForgingLine(forgingLine);

    return forgeAssembler.dissemble(completedForge);
  }

  public ForgingLine getForgingLineUsingTenantIdAndForgingLineId(long tenantId, long forgingLineId) {
    boolean isForgingLineOfTenantExists = forgingLineService.isForgingLineByTenantExists(tenantId);
    if (!isForgingLineOfTenantExists) {
      log.error("Forging Line={} for the tenant={} does not exist!", forgingLineId, tenantId);
      throw new ResourceNotFoundException("Forging Line for the tenant does not exist!");
    }
    return forgingLineService.getForgingLineByIdAndTenantId(forgingLineId, tenantId);
  }

  public boolean isForgeAppliedOnForgingLine(long forgingLineId) {
    Optional<Forge> forgeOptional = forgeRepository.findAppliedForgeOnForgingLine(forgingLineId);
    if (forgeOptional.isPresent()) {
      log.info("Forge={} already applied on forgingLineId={}", forgeOptional.get().getId(), forgingLineId);
      return true;
    }
    return false;
  }

  public Forge getAppliedForgeByForgingLine(long forgingLineId) {
    Optional<Forge> forgeOptional = forgeRepository.findAppliedForgeOnForgingLine(forgingLineId);
    if (forgeOptional.isEmpty()) {
      log.error("Forge does not exists for forgingLineId={}", forgingLineId);
      throw new ForgeNotFoundException("Forge does not exists for forgingLine!");
    }
    return forgeOptional.get();
  }

  @Transactional
  public void deleteForge(Forge forge) {
    returnHeats(forge.getForgeHeats());
    LocalDateTime currentTime = LocalDateTime.now();
    forge.getForgeHeats().forEach(forgeHeat -> {
      forgeHeat.setDeleted(true);
      forgeHeat.setDeletedAt(currentTime);
    });
    forge.setDeleted(true);
    forge.setDeletedAt(currentTime);
    forgeRepository.save(forge);
  }

  private void returnHeats(List<ForgeHeat> forgeHeats) {
    if (forgeHeats == null || forgeHeats.isEmpty()) {
      return;
    }

    // Group heat updates by heatId, summing up the quantities to be returned
    Map<Long, Double> heatQuantitiesToUpdate = forgeHeats.stream()
        .collect(Collectors.groupingBy(
            forgeHeat -> forgeHeat.getHeat().getId(),
            Collectors.summingDouble(ForgeHeat::getHeatQuantityUsed)
        ));

    // Perform bulk updates
    rawMaterialHeatService.returnHeatsInBatch(heatQuantitiesToUpdate);
  }

  private int getActualForgedPieces(String forgeCount) {
    try {
      return Integer.parseInt(forgeCount);
    } catch (Exception e) {
      log.error("Not a valid forgeCount input provided!");
      throw new RuntimeException("Not a valid forgeCount input provided!");
    }
  }

  public Forge getForgeById(long forgeId) {
    Optional<Forge> forgeOptional = forgeRepository.findByIdAndDeletedFalse(forgeId);
    if (forgeOptional.isEmpty()) {
      log.error("Forge does not exists for forgeId={}", forgeId);
      throw new ForgeNotFoundException("Forge does not exists for forgeId=" + forgeId);
    }
    return forgeOptional.get();
  }

  public Forge getForgeByForgeTraceabilityNumber(String forgeTraceabilityNumber) {
    Optional<Forge> forgeOptional = forgeRepository.findByForgeTraceabilityNumberAndDeletedFalse(forgeTraceabilityNumber);
    if (forgeOptional.isEmpty()) {
      log.error("Forge does not exists for forgeTraceabilityNumber={}", forgeTraceabilityNumber);
      throw new ForgeNotFoundException("Forge does not exists for forgeTraceabilityNumber=" + forgeTraceabilityNumber);
    }
    return forgeOptional.get();
  }

  public List<Forge> getForgeTraceabilitiesByHeatNumber(long tenantId, String heatNumber) {
//    return rawMaterialService.getRawMaterialByHeatNumber(tenantId, heatNumber).stream()
//        .flatMap(rm -> rm.getHeats().stream())
//        .filter(rmh -> heatNumber.equals(rmh.getHeatNumber()))
//        .flatMap(rmh -> getForgeTraceabilitiesByHeatId(rmh.getId()).stream())
//        .collect(Collectors.toList());
    return null;
  }

//  public List<Forge> getForgeTraceabilitiesByHeatId(long heatId){
//    return forgeRepository.findByHeatIdAndDeletedFalse(heatId);
//  }
  @Transactional
  public Forge saveForge(Forge forge){
    return forgeRepository.save(forge);
  }
}

