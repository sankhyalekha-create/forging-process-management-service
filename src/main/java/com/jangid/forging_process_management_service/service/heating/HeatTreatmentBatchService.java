package com.jangid.forging_process_management_service.service.heating;

import com.jangid.forging_process_management_service.assemblers.heating.HeatTreatmentBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.machining.MachiningBatchAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.forging.Furnace;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchNotInExpectedStatusException;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.dto.HeatTreatmentBatchAssociationsDTO;
import com.jangid.forging_process_management_service.dto.MachiningBatchAssociationsDTO;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.exception.heating.FurnaceOccupiedException;
import com.jangid.forging_process_management_service.exception.heating.HeatTreatmentBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.heating.HeatTreatmentBatchRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.machining.MachiningBatchService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HeatTreatmentBatchService {

  @Autowired
  private HeatTreatmentBatchRepository heatTreatmentBatchRepository;
  @Autowired
  private TenantService tenantService;
  @Autowired
  private FurnaceService furnaceService;
  @Autowired
  private HeatTreatmentBatchAssembler heatTreatmentBatchAssembler;
  @Autowired
  private MachiningBatchService machiningBatchService;

  @Transactional
  public HeatTreatmentBatchRepresentation applyHeatTreatmentBatch(long tenantId, long furnaceId, HeatTreatmentBatchRepresentation representation) {
    tenantService.validateTenantExists(tenantId);
    boolean exists = heatTreatmentBatchRepository.existsByHeatTreatmentBatchNumberAndTenantIdAndDeletedFalse(representation.getHeatTreatmentBatchNumber(), tenantId);
    if (exists) {
      log.error("Heat Treatment Batch with batch number: {} already exists with the tenant: {}!", representation.getHeatTreatmentBatchNumber(), tenantId);
      throw new IllegalStateException("Heat Treatment Batch with batch number =" + representation.getHeatTreatmentBatchNumber() +"with the tenant ="+tenantId);
    }

    // Check if this batch number was previously used and deleted
    if (isHeatTreatmentBatchNumberPreviouslyUsed(representation.getHeatTreatmentBatchNumber(), tenantId)) {
      log.warn("Heat Treatment Batch with batch number: {} was previously used and deleted for tenant: {}", 
               representation.getHeatTreatmentBatchNumber(), tenantId);
    }

    boolean isAnyBatchItemHasSelectedPiecesMoreThanActualForgedPieces =
        representation.getProcessedItemHeatTreatmentBatches().stream()
            .anyMatch(processedItemHeatTreatmentBatch ->
                          processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount() >
                          processedItemHeatTreatmentBatch.getProcessedItem().getAvailableForgePiecesCountForHeat()
            );

    if (isAnyBatchItemHasSelectedPiecesMoreThanActualForgedPieces) {
      log.error("For any item, pieces selected for heatTreatmentBatch is more than actually forged pieces of item! furnaceId=" + furnaceId + " tenantId=" + tenantId);
      throw new RuntimeException("For any item, pieces selected for heatTreatmentBatch is more than actually forged pieces of item! furnaceId=" + furnaceId + " tenantId=" + tenantId);
    }
    Furnace furnace = getFurnaceUsingTenantIdAndFurnaceId(tenantId, furnaceId);
    boolean isHeatTreatmentBatchAppliedOnFurnace = isHeatTreatmentBatchAppliedOnFurnace(furnaceId);

    if (isHeatTreatmentBatchAppliedOnFurnace) {
      log.error("Furnace={} is already having a heatTreatmentBatch set. Cannot apply a new heatTreatmentBatch on this furnace", furnaceId);
      throw new FurnaceOccupiedException("Cannot apply a new heatTreatmentBatch on this furnace as Furnace " + furnaceId + " is already occupied");
    }

    HeatTreatmentBatch inputHeatTreatmentBatch = heatTreatmentBatchAssembler.createAssemble(representation);

    if (inputHeatTreatmentBatch.getTotalWeight() > furnace.getFurnaceCapacity()) {
      log.error(
          "Selected items' total weight for heatTreatment has exceeded the furnace capacity! Can not perform heatTreatment operation. Furnace=" + furnace.getFurnaceName() + " tenantId=" + tenantId);
      throw new RuntimeException(
          "Selected items' total weight for heatTreatment has exceeded the furnace capacity! Can not perform heatTreatment operation. Furnace=" + furnace.getFurnaceName() + " tenantId=" + tenantId);
    }

    inputHeatTreatmentBatch.setFurnace(furnace);
    Tenant tenant = tenantService.getTenantById(tenantId);
    inputHeatTreatmentBatch.setTenant(tenant);
//    List<ProcessedItemHeatTreatmentBatch> processedItemHeatTreatmentBatches = representation.getProcessedItemHeatTreatmentBatches().stream().map(processedItemHeatTreatmentBatchRepresentation -> {
//      return processedItemHeatTreatmentBatchAssembler.createAssemble(processedItemHeatTreatmentBatchRepresentation);
//    }).toList();
    LocalDateTime applyAt = ConvertorUtils.convertStringToLocalDateTime(representation.getApplyAt());

    inputHeatTreatmentBatch.getProcessedItemHeatTreatmentBatches().forEach(processedItemHeatTreatmentBatch -> {
      ProcessedItem processedItem = processedItemHeatTreatmentBatch.getProcessedItem();

      if (processedItem.getForge().getStartAt().compareTo(applyAt) > 0) {
        log.error("The provided apply at time={} is before to selected forge={} start at time={} !", applyAt,
                  processedItem.getForge().getForgeTraceabilityNumber(), processedItem.getForge().getStartAt());
        throw new RuntimeException(
            "The provided apply at time=" + applyAt + " is before to selected forge=" + processedItem.getForge().getForgeTraceabilityNumber() + " start at time=" + processedItem.getForge()
                .getStartAt()
            + " !");
      }

      int currentAvailableForgePiecesForHeat = processedItem.getAvailableForgePiecesCountForHeat();
      if (currentAvailableForgePiecesForHeat < processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount()) {
        throw new IllegalArgumentException("Piece count exceeds available forge pieces count.");
      }

      processedItem.setAvailableForgePiecesCountForHeat(currentAvailableForgePiecesForHeat - processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount());
      processedItemHeatTreatmentBatch.setItemStatus(ItemStatus.HEAT_TREATMENT_NOT_STARTED);
      processedItemHeatTreatmentBatch.setHeatTreatmentBatch(inputHeatTreatmentBatch);
    });
//    List<ProcessedItem> processedItems = representation.getProcessedItems().stream().map(processedItemRepresentation ->
//                                                                                         {
//                                                                                           ProcessedItem processedItem = processedItemService.getProcessedItemById(processedItemRepresentation.getId());
//                                                                                           processedItem.setHeatTreatBatchPiecesCount(
//                                                                                               processedItemRepresentation.getHeatTreatBatchPiecesCount() != null ? Integer.valueOf(
//                                                                                                   processedItemRepresentation.getHeatTreatBatchPiecesCount()) : null);
//                                                                                           return processedItem;
//                                                                                         }).toList();
//
//    processedItems.forEach(processedItem -> {
//                             int currentAvailableForgePiecesForHeat = processedItem.getAvailableForgePiecesCountForHeat();
//                             processedItem.setAvailableForgePiecesCountForHeat(currentAvailableForgePiecesForHeat - processedItem.getHeatTreatBatchPiecesCount());
//                             processedItem.setItemStatus(ItemStatus.HEAT_TREATMENT_NOT_STARTED);
//                           }
//    );
//    processedItems.forEach(inputHeatTreatmentBatch::addItem);
//    processedItems.forEach(processedItem -> processedItem.setHeatTreatmentBatch(inputHeatTreatmentBatch));


    HeatTreatmentBatch createdHeatTreatmentBatch = heatTreatmentBatchRepository.save(inputHeatTreatmentBatch); // Save entity
    furnace.setFurnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_APPLIED);
    furnaceService.saveFurnace(furnace);

    // Return the created HeatTreatmentBatch
    HeatTreatmentBatchRepresentation createdRepresentation = heatTreatmentBatchAssembler.dissemble(createdHeatTreatmentBatch);
    createdRepresentation.setHeatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.IDLE.name());
    return createdRepresentation;
  }


  public Furnace getFurnaceUsingTenantIdAndFurnaceId(long tenantId, long furnaceId) {
    boolean isFurnaceOfTenantExists = furnaceService.isFurnaceByTenantExists(tenantId);
    if (!isFurnaceOfTenantExists) {
      log.error("Furnace={} for the tenant={} does not exist!", furnaceId, tenantId);
      throw new ResourceNotFoundException("Furnace for the tenant does not exist!");
    }
    return furnaceService.getFurnaceByIdAndTenantId(furnaceId, tenantId);
  }

  public boolean isHeatTreatmentBatchAppliedOnFurnace(long furnaceId) {
    Optional<HeatTreatmentBatch> heatTreatmentBatchOptional = heatTreatmentBatchRepository.findAppliedHeatTreatmentBatchOnFurnace(furnaceId);
    if (heatTreatmentBatchOptional.isPresent()) {
      log.info("HeatTreatmentBatch={} already applied on furnaceId={}", heatTreatmentBatchOptional.get().getId(), furnaceId);
      return true;
    }
    return false;
  }

  //Start HeatTreatmentBatch (Update start Time, Update Item Status, Update FurnaceStatus)
  @Transactional
  public HeatTreatmentBatchRepresentation startHeatTreatmentBatch(long tenantId, long furnaceId, long heatTreatmentBatchId, String startAt) {
    tenantService.validateTenantExists(tenantId);
    Furnace furnace = getFurnaceUsingTenantIdAndFurnaceId(tenantId, furnaceId);
    boolean isHeatTreatmentBatchAppliedOnFurnace = isHeatTreatmentBatchAppliedOnFurnace(furnaceId);

    if (!isHeatTreatmentBatchAppliedOnFurnace) {
      log.error("Furnace={} does not have a heatTreatmentBatch set. Can not start forge existingHeatTreatmentBatch on this furnace as it does not have heatTreatmentBatch", furnaceId);
      throw new ForgeNotFoundException("heatTreatmentBatch does not exists for furnace!");
    }
    HeatTreatmentBatch existingHeatTreatmentBatch = getHeatTreatmentBatchById(heatTreatmentBatchId);

    if (existingHeatTreatmentBatch.getStartAt() != null) {
      log.error("The heatTreatmentBatch={} for furnace={} has already been started!", heatTreatmentBatchId, furnace.getFurnaceName());
      throw new HeatTreatmentBatchNotInExpectedStatusException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + "has already been started!");
    }

    LocalDateTime applyAtLocalDateTime = existingHeatTreatmentBatch.getApplyAt();
    LocalDateTime startAtLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(startAt);

    if (applyAtLocalDateTime.compareTo(startAtLocalDateTime) > 0) {
      log.error("The provided start at time={} is before to heat treatment batch={} apply at time={} !", startAtLocalDateTime,
                existingHeatTreatmentBatch.getHeatTreatmentBatchNumber(), applyAtLocalDateTime);
      throw new RuntimeException(
          "The provided start at time=" + startAtLocalDateTime + " is before to heat treatment batch=" + existingHeatTreatmentBatch.getHeatTreatmentBatchNumber() + " apply at time=" + applyAtLocalDateTime
          + " !");
    }

    if (!HeatTreatmentBatch.HeatTreatmentBatchStatus.IDLE.equals(existingHeatTreatmentBatch.getHeatTreatmentBatchStatus())) {
      log.error("The heatTreatmentBatch={} for furnace={} is not in IDLE status to start it!", heatTreatmentBatchId, furnace.getFurnaceName());
      throw new HeatTreatmentBatchNotInExpectedStatusException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + "Not in IDLE status to start it!");
    }

    existingHeatTreatmentBatch.setHeatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.IN_PROGRESS);
    existingHeatTreatmentBatch.setStartAt(ConvertorUtils.convertStringToLocalDateTime(startAt));
    existingHeatTreatmentBatch.getProcessedItemHeatTreatmentBatches().forEach(processedItemHeatTreatmentBatch -> {
      processedItemHeatTreatmentBatch.setItemStatus(ItemStatus.HEAT_TREATMENT_IN_PROGRESS);
    });

    HeatTreatmentBatch startedHeatTreatmentBatch = heatTreatmentBatchRepository.save(existingHeatTreatmentBatch);

    furnace.setFurnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_IN_PROGRESS);
    furnaceService.saveFurnace(furnace);

    return heatTreatmentBatchAssembler.dissemble(startedHeatTreatmentBatch);
  }

  //End HeatTreatmentBatch (Update End Time, Update Item Status, Update FurnaceStatus)

  @Transactional
  public HeatTreatmentBatchRepresentation endHeatTreatmentBatch(long tenantId, long furnaceId, long heatTreatmentBatchId, HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    tenantService.validateTenantExists(tenantId);
    Furnace furnace = getFurnaceUsingTenantIdAndFurnaceId(tenantId, furnaceId);
    boolean isHeatTreatmentBatchAppliedOnFurnace = isHeatTreatmentBatchAppliedOnFurnace(furnace.getId());

    if (!isHeatTreatmentBatchAppliedOnFurnace) {
      log.error("Furnace={} does not have a existing heatTreatmentBatch set. Can not end heatTreatmentBatch on this furnace as it does not have existing heatTreatmentBatch", furnaceId);
      throw new HeatTreatmentBatchNotFoundException("HeatTreatmentBatch does not exists for furnace=" + furnaceId);
    }

    HeatTreatmentBatch existingHeatTreatmentBatch = getHeatTreatmentBatchById(heatTreatmentBatchId);

    if (existingHeatTreatmentBatch.getEndAt() != null) {
      log.error("The heatTreatmentBatch={} for furnace={} has already been ended!", heatTreatmentBatchId, furnace.getFurnaceName());
      throw new HeatTreatmentBatchNotInExpectedStatusException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + "has already been ended!");
    }

    if (!HeatTreatmentBatch.HeatTreatmentBatchStatus.IN_PROGRESS.equals(existingHeatTreatmentBatch.getHeatTreatmentBatchStatus())) {
      log.error("The heatTreatmentBatch={} for furnace={} is not in IN_PROGRESS status to end it!", heatTreatmentBatchId, furnace.getFurnaceName());
      throw new HeatTreatmentBatchNotInExpectedStatusException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + "Not in IN_PROGRESS status to end it!");
    }
    LocalDateTime endAt = ConvertorUtils.convertStringToLocalDateTime(heatTreatmentBatchRepresentation.getEndAt());
    if (existingHeatTreatmentBatch.getStartAt().compareTo(endAt) > 0) {
      log.error("The heatTreatmentBatch={} for furnace={} end time is before the start time!", heatTreatmentBatchId, furnace.getFurnaceName());
      throw new RuntimeException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + " end time is before the start time!");
    }

    existingHeatTreatmentBatch.setHeatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.COMPLETED);
    existingHeatTreatmentBatch.setEndAt(endAt);

    existingHeatTreatmentBatch.getProcessedItemHeatTreatmentBatches().forEach(processedItemHeatTreatmentBatch -> {
      processedItemHeatTreatmentBatch.setItemStatus(ItemStatus.HEAT_TREATMENT_COMPLETED);

      // Check if the processed item exists in the representation and update it
      heatTreatmentBatchRepresentation.getProcessedItemHeatTreatmentBatches().stream()
          .filter(processedItemHeatTreatmentBatchRepresentation -> processedItemHeatTreatmentBatchRepresentation.getId().equals(processedItemHeatTreatmentBatch.getId()))
          .findFirst()
          .ifPresent(processedItemHeatTreatmentBatchRepresentation -> {
                       int actualHeatTreatmentBatchPiecesCount = processedItemHeatTreatmentBatchRepresentation.getActualHeatTreatBatchPiecesCount();
                       processedItemHeatTreatmentBatch.setActualHeatTreatBatchPiecesCount(actualHeatTreatmentBatchPiecesCount);
                       processedItemHeatTreatmentBatch.setInitialMachiningBatchPiecesCount(actualHeatTreatmentBatchPiecesCount);
                       processedItemHeatTreatmentBatch.setAvailableMachiningBatchPiecesCount(actualHeatTreatmentBatchPiecesCount);
                     }

          );
    });

    HeatTreatmentBatch completedHeatTreatmentBatch = heatTreatmentBatchRepository.save(existingHeatTreatmentBatch);

    furnace.setFurnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_NOT_APPLIED);
    furnaceService.saveFurnace(furnace);

    return heatTreatmentBatchAssembler.dissemble(completedHeatTreatmentBatch);
  }


  public HeatTreatmentBatch getHeatTreatmentBatchById(long heatTreatmentBatchId) {
    Optional<HeatTreatmentBatch> heatTreatmentBatchOptional = heatTreatmentBatchRepository.findByIdAndDeletedFalse(heatTreatmentBatchId);
    if (heatTreatmentBatchOptional.isEmpty()) {
      log.error("HeatTreatmentBatch does not exists for heatTreatmentBatchId={}", heatTreatmentBatchId);
      throw new HeatTreatmentBatchNotFoundException("HeatTreatmentBatch does not exists for heatTreatmentBatchId=" + heatTreatmentBatchId);
    }
    return heatTreatmentBatchOptional.get();
  }
  //getAppliedHeatTreatmentByFurnace

  public HeatTreatmentBatch getAppliedHeatTreatmentByFurnace(long furnaceId) {
    Optional<HeatTreatmentBatch> heatTreatmentBatchOptional = heatTreatmentBatchRepository.findAppliedHeatTreatmentBatchOnFurnace(furnaceId);
    if (heatTreatmentBatchOptional.isEmpty()) {
      log.error("HeatTreatmentBatch does not exists for furnace={}", furnaceId);
      throw new ForgeNotFoundException("HeatTreatmentBatch does not exists for furnace!");
    }
    return heatTreatmentBatchOptional.get();
  }

//  getAllHeatTreatmentBatchByTenantId

  public Page<HeatTreatmentBatchRepresentation> getAllHeatTreatmentBatchByTenantId(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

//    List<Long> furnaceIds = furnaceService.getAllFurnacesOfTenant(tenantId)
//        .stream()
//        .map(Furnace::getId)
//        .collect(Collectors.toList());

    Page<HeatTreatmentBatch> heatTreatmentBatchPage = heatTreatmentBatchRepository
        .findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId, pageable);

    return heatTreatmentBatchPage.map(heatTreatmentBatchAssembler::dissemble);
  }

  /**
   * Get all associations for a heat treatment batch
   *
   * @param heatTreatmentBatchId The ID of the heat treatment batch
   * @param tenantId The ID of the tenant
   * @return DTO containing heat treatment batch details and all associated machining batches
   */
  @Transactional(readOnly = true)
  public HeatTreatmentBatchAssociationsDTO getHeatTreatmentBatchAssociations(Long heatTreatmentBatchId, Long tenantId) {
    tenantService.validateTenantExists(tenantId);
    
    // Get the heat treatment batch
    HeatTreatmentBatch heatTreatmentBatch = getHeatTreatmentBatchById(heatTreatmentBatchId);
    
    if (heatTreatmentBatch == null || heatTreatmentBatch.getTenant().getId() != tenantId) {
      log.error("Heat treatment batch not found or doesn't belong to tenant. ID: {}, Tenant: {}", 
                heatTreatmentBatchId, tenantId);
      throw new HeatTreatmentBatchNotFoundException("Heat treatment batch not found");
    }
    
    // Convert to representation
    HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation = 
        heatTreatmentBatchAssembler.dissemble(heatTreatmentBatch);
    
    // Get all machining batches associated with this heat treatment batch
    List<MachiningBatchAssociationsDTO> machiningBatchesWithAssociations = 
        getMachiningBatchesWithAssociationsForHeatTreatmentBatch(heatTreatmentBatchId, tenantId);
    
    // Create and return the associations DTO
    return HeatTreatmentBatchAssociationsDTO.builder()
        .heatTreatmentBatchId(heatTreatmentBatchId)
        .heatTreatmentBatch(heatTreatmentBatchRepresentation)
        .machiningBatches(machiningBatchesWithAssociations)
        .build();
  }
  
  /**
   * Helper method to fetch all machining batches with their associations for a heat treatment batch
   * 
   * @param heatTreatmentBatchId The ID of the heat treatment batch
   * @param tenantId The ID of the tenant
   * @return List of machining batch association DTOs
   */
  private List<MachiningBatchAssociationsDTO> getMachiningBatchesWithAssociationsForHeatTreatmentBatch(Long heatTreatmentBatchId, Long tenantId) {
    List<com.jangid.forging_process_management_service.entities.machining.MachiningBatch> machiningBatches = 
        heatTreatmentBatchRepository.findMachiningBatchesByHeatTreatmentBatchId(heatTreatmentBatchId);
    
    return machiningBatches.stream()
        .map(machiningBatch -> {
            // For each machining batch, get the full associations with inspection and dispatch batches
            return machiningBatchService.getMachiningBatchAssociations(machiningBatch.getId(), tenantId);
        })
        .collect(Collectors.toList());
  }

  @Transactional
  public void deleteHeatTreatmentBatch(long tenantId, long heatTreatmentBatchId) {
    // 1. Validate tenant exists
    tenantService.validateTenantExists(tenantId);

    // 2. Validate heat treatment batch exists
    HeatTreatmentBatch heatTreatmentBatch = getHeatTreatmentBatchById(heatTreatmentBatchId);

    // 3. Validate heat treatment batch status is COMPLETED
    if (!HeatTreatmentBatch.HeatTreatmentBatchStatus.COMPLETED.equals(heatTreatmentBatch.getHeatTreatmentBatchStatus())) {
      log.error("Cannot delete heat treatment batch={} as it is not in COMPLETED status!", heatTreatmentBatchId);
      throw new IllegalStateException("This heat treatment batch cannot be deleted as it is not in the COMPLETED status.");
    }

    // 4. Validate no processedItemHeatTreatmentBatch is part of any MachiningBatch
    validateNoMachiningBatchExistsForProcessedItemHeatTreatmentBatches(heatTreatmentBatch);

    // 5. Revert the availableForgePiecesCountForHeat for each processedItem
    LocalDateTime now = LocalDateTime.now();
    for (ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch : heatTreatmentBatch.getProcessedItemHeatTreatmentBatches()) {
      ProcessedItem processedItem = processedItemHeatTreatmentBatch.getProcessedItem();

      // Revert the pieces count that were allocated to heat treatment
      processedItem.setAvailableForgePiecesCountForHeat(
          processedItem.getAvailableForgePiecesCountForHeat() + processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount()
      );

      // 6. Soft delete each ProcessedItemHeatTreatmentBatch
      processedItemHeatTreatmentBatch.setDeleted(true);
      processedItemHeatTreatmentBatch.setDeletedAt(now);
    }

    // 7. Store the original batch number and modify the batch number for deletion
    heatTreatmentBatch.setOriginalHeatTreatmentBatchNumber(heatTreatmentBatch.getHeatTreatmentBatchNumber());
    heatTreatmentBatch.setHeatTreatmentBatchNumber(
        heatTreatmentBatch.getHeatTreatmentBatchNumber() + "_deleted_" + heatTreatmentBatch.getId() + "_" + now.toEpochSecond(java.time.ZoneOffset.UTC)
    );

    // 8. Soft delete the HeatTreatmentBatch
    heatTreatmentBatch.setDeleted(true);
    heatTreatmentBatch.setDeletedAt(now);
    heatTreatmentBatchRepository.save(heatTreatmentBatch);

    log.info("Successfully deleted heat treatment batch={}, original batch number={}", 
        heatTreatmentBatchId, heatTreatmentBatch.getOriginalHeatTreatmentBatchNumber());
  }

  private void validateNoMachiningBatchExistsForProcessedItemHeatTreatmentBatches(HeatTreatmentBatch heatTreatmentBatch) {
    for (ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch : heatTreatmentBatch.getProcessedItemHeatTreatmentBatches()) {
      // Check if any machining batch uses this processedItemHeatTreatmentBatch
      boolean isAnyMachiningBatchExistsForHeatTreatmentBatch = processedItemHeatTreatmentBatch.getMachiningBatches().stream().anyMatch(mb -> !mb.isDeleted());
      if (isAnyMachiningBatchExistsForHeatTreatmentBatch) {
        log.error("Cannot delete heat treatment batch={} as it has associated machining batches", heatTreatmentBatch.getId());
        throw new IllegalStateException("This heat treatment batch cannot be deleted as a machining batch entry exists for it.");
      }
    }
  }

  public boolean isHeatTreatmentBatchNumberPreviouslyUsed(String heatTreatmentBatchNumber, Long tenantId) {
    return heatTreatmentBatchRepository.existsByHeatTreatmentBatchNumberAndTenantIdAndOriginalHeatTreatmentBatchNumber(
        heatTreatmentBatchNumber, tenantId);
  }

  /**
   * Search for heat treatment batches by various criteria with pagination
   * @param tenantId The tenant ID
   * @param searchType The type of search (ITEM_NAME, FORGE_TRACEABILITY_NUMBER, HEAT_TREATMENT_BATCH_NUMBER, or FURNACE_NAME)
   * @param searchTerm The search term (substring matching for all search types)
   * @param page The page number (0-based)
   * @param size The page size
   * @return Page of HeatTreatmentBatchRepresentation containing the search results
   */
  @Transactional(readOnly = true)
  public Page<HeatTreatmentBatchRepresentation> searchHeatTreatmentBatches(Long tenantId, String searchType, String searchTerm, int page, int size) {
    if (searchTerm == null || searchTerm.trim().isEmpty() || searchType == null || searchType.trim().isEmpty()) {
      Pageable pageable = PageRequest.of(page, size);
      return Page.empty(pageable);
    }

    tenantService.validateTenantExists(tenantId);
    Pageable pageable = PageRequest.of(page, size);
    Page<HeatTreatmentBatch> heatTreatmentBatchPage;
    
    switch (searchType.toUpperCase()) {
      case "ITEM_NAME":
        heatTreatmentBatchPage = heatTreatmentBatchRepository.findHeatTreatmentBatchesByItemNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "FORGE_TRACEABILITY_NUMBER":
        heatTreatmentBatchPage = heatTreatmentBatchRepository.findHeatTreatmentBatchesByForgeTraceabilityNumberContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "HEAT_TREATMENT_BATCH_NUMBER":
        heatTreatmentBatchPage = heatTreatmentBatchRepository.findHeatTreatmentBatchesByHeatTreatmentBatchNumberContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "FURNACE_NAME":
        heatTreatmentBatchPage = heatTreatmentBatchRepository.findHeatTreatmentBatchesByFurnaceNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      default:
        log.error("Invalid search type: {}", searchType);
        throw new IllegalArgumentException("Invalid search type: " + searchType + ". Valid types are: ITEM_NAME, FORGE_TRACEABILITY_NUMBER, HEAT_TREATMENT_BATCH_NUMBER, FURNACE_NAME");
    }
    
    return heatTreatmentBatchPage.map(heatTreatmentBatchAssembler::dissemble);
  }
}
