package com.jangid.forging_process_management_service.service.forging;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.forging.ForgeHeat;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.product.Item;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
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


  public Page<ForgeRepresentation> getAllForges(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

    List<Long> forgingLineIds = forgingLineService.getAllForgingLinesByTenant(tenantId)
        .stream()
        .map(ForgingLine::getId)
        .collect(Collectors.toList());

    if (forgingLineIds.isEmpty()) {
      return Page.empty(pageable);
    }

    Page<Forge> forgePage = forgeRepository.findByForgingLineIdInAndDeletedFalseOrderByUpdatedAtDesc(
        forgingLineIds, pageable);

    return forgePage.map(forgeAssembler::dissemble);
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

    LocalDateTime applyAtLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(representation.getApplyAt());

    // Update heat quantities
    representation.getForgeHeats().forEach(forgeHeat -> {

      Heat heat = rawMaterialHeatService.getRawMaterialHeatByHeatNumberAndTenantId(forgeHeat.getHeat().getHeatNumber(), tenantId);
      double newHeatQuantity = heat.getAvailableHeatQuantity() - Double.parseDouble(forgeHeat.getHeatQuantityUsed());
      if (newHeatQuantity < 0) {
        log.error("Insufficient heat quantity for heat={} on tenantId={}", heat.getId(), tenantId);
        throw new IllegalArgumentException("Insufficient heat quantity for heat " + heat.getId());
      }
      if (heat.getCreatedAt().compareTo(applyAtLocalDateTime) > 0) {
        log.error("The provided apply at time={} is before to heat={} created at time={} !", applyAtLocalDateTime,
                  heat.getHeatNumber(), heat.getCreatedAt());
        throw new RuntimeException(
            "The provided apply at time=" + applyAtLocalDateTime + " is before to heat=" + heat.getHeatNumber() + " created at time=" + heat.getCreatedAt()
            + " !");
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
    inputForge.setApplyAt(applyAtLocalDateTime);

    Item item = itemService.getItemByIdAndTenantId(representation.getProcessedItem().getItem().getId(), tenantId);
    Double itemWeight = item.getItemWeight();
    Double totalHeatReserved = representation.getForgeHeats().stream()
        .mapToDouble(forgeHeat -> Double.parseDouble(forgeHeat.getHeatQuantityUsed()))
        .sum();

    ProcessedItem processedItem = ProcessedItem.builder()
        .item(item)
        .forge(inputForge)
        .expectedForgePiecesCount((int) Math.floor(totalHeatReserved / itemWeight))
        .createdAt(LocalDateTime.now())
        .build();

    inputForge.setProcessedItem(processedItem);
    Tenant tenant = tenantService.getTenantById(tenantId);
    inputForge.setTenant(tenant);

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

    // Construct the prefix for the forge traceability number
    String forgePrefix = initialsOfTenant + forgingLineName + localDate;

    // Fetch the list of forge entries for this forging line on the current day
    List<Forge> forgesForTheDay = forgeRepository.findLastForgeForTheDay(forgingLineId, forgePrefix);

    // Determine the counter value based on the latest entry
    int counter = forgesForTheDay.stream()
        .findFirst()
        .map(forge -> Integer.parseInt(forge.getForgeTraceabilityNumber()
                                           .substring(forge.getForgeTraceabilityNumber().lastIndexOf("-") + 1)) + 1)
        .orElse(1);

    return forgePrefix + "-" + counter;
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

    LocalDateTime startTimeLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(startAt);

    if (existingForge.getApplyAt().compareTo(startTimeLocalDateTime) > 0) {
      log.error("The forge having forge traceability number={} provided start time={} is before apply at time={} !", existingForge.getForgeTraceabilityNumber(), startTimeLocalDateTime,
                existingForge.getApplyAt());
      throw new RuntimeException(
          "The forge having forge traceability number=" + existingForge.getForgeTraceabilityNumber() + " provided start time=" + startTimeLocalDateTime + " is before apply at time="
          + existingForge.getApplyAt());
    }

    if (!Forge.ForgeStatus.IDLE.equals(existingForge.getForgingStatus())) {
      log.error("The forge={} having traceability={} is not in IDLE status to start it!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new ForgeNotInExpectedStatusException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + "Not in IDLE status to start it!");
    }

    existingForge.setForgingStatus(Forge.ForgeStatus.IN_PROGRESS);
    existingForge.setStartAt(ConvertorUtils.convertStringToLocalDateTime(startAt));
    existingForge.setForgeTraceabilityNumber(getForgeTraceabilityNumber(tenantId, forgingLine.getForgingLineName(), forgingLineId));

    Forge startedForge = forgeRepository.save(existingForge);

    forgingLine.setForgingLineStatus(ForgingLine.ForgingLineStatus.FORGE_IN_PROGRESS);
    forgingLineService.saveForgingLine(forgingLine);

    return forgeAssembler.dissemble(startedForge);
  }

  @Transactional
  public ForgeRepresentation endForge(long tenantId, long forgingLineId, long forgeId, ForgeRepresentation representation) {
    tenantService.validateTenantExists(tenantId);
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

    int actualForgedPieces = getActualForgedPieces(representation.getActualForgeCount());

    existingForge.setForgingStatus(Forge.ForgeStatus.COMPLETED);
    ProcessedItem existingForgeProcessedItem = existingForge.getProcessedItem();
    existingForgeProcessedItem.setActualForgePiecesCount(actualForgedPieces);
    existingForgeProcessedItem.setAvailableForgePiecesCountForHeat(actualForgedPieces);
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
  public void deleteForge(Long tenantId, Long forgeId) {
    // 1. Validate tenant exists
    tenantService.validateTenantExists(tenantId);

    // 2. Validate forge exists and belongs to tenant
    Forge forge = getForgeByIdAndTenantId(forgeId, tenantId);

    // 3. Validate forge status is COMPLETED
    if (Forge.ForgeStatus.COMPLETED != forge.getForgingStatus()) {
        log.error("Cannot delete forge as it is not in COMPLETED status. Current status: {}", forge.getForgingStatus());
        throw new IllegalStateException("Cannot delete forge as it is not in COMPLETED status");
    }

    // 4. Check if forge's processed item is used in any active heat treatment batches
    ProcessedItem processedItem = forge.getProcessedItem();
    boolean hasActiveHeatTreatmentBatches = processedItem.getProcessedItemHeatTreatmentBatches().stream()
        .anyMatch(batch -> !batch.isDeleted() && !batch.getHeatTreatmentBatch().isDeleted());
    
    if (hasActiveHeatTreatmentBatches) {
        log.error("Cannot delete forge id={} as it has active heat treatment batches", forgeId);
        throw new IllegalStateException("Cannot delete forge as it has items that are used in heat treatment batches");
    }

    // 5. Return heat quantities to original heats
    LocalDateTime currentTime = LocalDateTime.now();
    forge.getForgeHeats().forEach(forgeHeat -> {
        Heat heat = forgeHeat.getHeat();
        double quantityToReturn = forgeHeat.getHeatQuantityUsed();
        double newAvailableQuantity = heat.getAvailableHeatQuantity() + quantityToReturn;
        heat.setAvailableHeatQuantity(newAvailableQuantity);
        rawMaterialHeatService.updateRawMaterialHeat(heat); // Persist the updated heat

        // Soft delete forge heat
        forgeHeat.setDeleted(true);
        forgeHeat.setDeletedAt(currentTime);
    });

    // 6. Soft delete ProcessedItem
    processedItem.setDeleted(true);
    processedItem.setDeletedAt(currentTime);

    // 7. Soft delete Forge
    forge.setDeleted(true);
    forge.setDeletedAt(currentTime);

    // Save the updated forge which will cascade to processed item and forge heats
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

  public Forge getForgeByIdAndTenantId(long forgeId, long tenantId) {
    Optional<Forge> forgeOptional = forgeRepository.findByIdAndTenantIdAndDeletedFalse(forgeId, tenantId);
    if (forgeOptional.isEmpty()) {
      log.error("Forge does not exists for forgeId={}, tenantId={}", forgeId, tenantId);
      throw new ForgeNotFoundException("Forge does not exists for forgeId=" + forgeId + ", tenantId=" + tenantId);
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
  public Forge saveForge(Forge forge) {
    return forgeRepository.save(forge);
  }
}

