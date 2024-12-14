//package com.jangid.forging_process_management_service.service.heating;
//
//import com.jangid.forging_process_management_service.assemblers.heating.BatchItemSelectionAssembler;
//import com.jangid.forging_process_management_service.assemblers.heating.HeatTreatmentBatchAssembler;
//import com.jangid.forging_process_management_service.entities.forging.Forge;
//import com.jangid.forging_process_management_service.entities.forging.Furnace;
//import com.jangid.forging_process_management_service.entities.heating.BatchItemSelection;
//import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
//import com.jangid.forging_process_management_service.entities.product.Item;
//import com.jangid.forging_process_management_service.entities.product.ItemStatus;
//import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchNotInExpectedStatusException;
//import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
//import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
//import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
//import com.jangid.forging_process_management_service.exception.heating.FurnaceOccupiedException;
//import com.jangid.forging_process_management_service.exception.heating.HeatTreatmentBatchNotFoundException;
//import com.jangid.forging_process_management_service.repositories.heating.HeatTreatmentBatchRepository;
//import com.jangid.forging_process_management_service.service.TenantService;
//import com.jangid.forging_process_management_service.service.forging.ForgeService;
//import com.jangid.forging_process_management_service.service.product.ItemService;
//import com.jangid.forging_process_management_service.utils.ConvertorUtils;
//
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//@Slf4j
//@Service
//public class HeatTreatmentBatchService {
//
//  @Autowired
//  private HeatTreatmentBatchRepository heatTreatmentBatchRepository;
//  @Autowired
//  private TenantService tenantService;
//  @Autowired
//  private FurnaceService furnaceService;
//  @Autowired
//  private ForgeService forgeService;
//
//  @Autowired
//  private ItemService itemService;
//  @Autowired
//  private HeatTreatmentBatchAssembler heatTreatmentBatchAssembler;
//
//  @Autowired
//  private BatchItemSelectionAssembler batchItemSelectionAssembler;
//
//  @Transactional
//  public HeatTreatmentBatchRepresentation applyHeatTreatmentBatch(long tenantId, long furnaceId, HeatTreatmentBatchRepresentation representation) {
//    tenantService.validateTenantExists(tenantId);
//    boolean isAnyBatchItemHasSelectedPiecesMoreThanActualForgedPieces =
//        representation.getBatchItems().stream()
//            .anyMatch(batchItem ->
//                          Integer.parseInt(batchItem.getHeatTreatBatchPiecesCount()) >
//                          Integer.parseInt(batchItem.getAvailableForgedPiecesCount())
//            );
//
//    if (isAnyBatchItemHasSelectedPiecesMoreThanActualForgedPieces) {
//      log.error("For any item, pieces selected for heatTreatmentBatch is more than actually forged pieces of item! furnaceId=" + furnaceId + " tenantId=" + tenantId);
//      throw new RuntimeException("For any item, pieces selected for heatTreatmentBatch is more than actually forged pieces of item! furnaceId=" + furnaceId + " tenantId=" + tenantId);
//    }
//    Furnace furnace = getFurnaceUsingTenantIdAndFurnaceId(tenantId, furnaceId);
//    boolean isHeatTreatmentBatchAppliedOnFurnace = isHeatTreatmentBatchAppliedOnFurnace(furnaceId);
//
//    if (isHeatTreatmentBatchAppliedOnFurnace) {
//      log.error("Furnace={} is already having a heatTreatmentBatch set. Cannot apply a new heatTreatmentBatch on this furnace", furnaceId);
//      throw new FurnaceOccupiedException("Cannot apply a new heatTreatmentBatch on this furnace as Furnace " + furnaceId + " is already occupied");
//    }
//
//    // Create and save the HeatTreatmentBatch
//    HeatTreatmentBatch inputHeatTreatmentBatch = heatTreatmentBatchAssembler.createAssemble(representation);
//    inputHeatTreatmentBatch.setFurnace(furnace);
//    List<BatchItemSelection> batchItemSelections = representation.getBatchItems().stream().map(batchItemSelectionAssembler::createAssemble).toList();
//
//    batchItemSelections.forEach(inputHeatTreatmentBatch::addItem);
//    batchItemSelections.forEach(batchItem -> batchItem.setHeatTreatmentBatch(inputHeatTreatmentBatch));
//
//    if (inputHeatTreatmentBatch.getTotalWeight() > furnace.getFurnaceCapacity()) {
//      log.error(
//          "Selected items' total weight for heatTreatment has exceeded the furnace capacity! Can not perform heatTreatment operation. Furnace=" + furnace.getFurnaceName() + " tenantId=" + tenantId);
//      throw new RuntimeException(
//          "Selected items' total weight for heatTreatment has exceeded the furnace capacity! Can not perform heatTreatment operation. Furnace=" + furnace.getFurnaceName() + " tenantId=" + tenantId);
//    }
//
//    inputHeatTreatmentBatch.getBatchItems().forEach(batchItem -> {
//                                                      Forge forge = batchItem.getForge();
//                                                      forge.setAvailableForgedPiecesCount(batchItem.getForge().getAvailableForgedPiecesCount() - batchItem.getHeatTreatBatchPiecesCount());
//                                                      forgeService.saveForge(forge);
//                                                    }
//    );
//
//    HeatTreatmentBatch createdHeatTreatmentBatch = heatTreatmentBatchRepository.save(inputHeatTreatmentBatch); // Save entity
//    furnace.setFurnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_APPLIED);
//    furnaceService.saveFurnace(furnace);
//
//    // Return the created HeatTreatmentBatch
//    HeatTreatmentBatchRepresentation createdRepresentation = heatTreatmentBatchAssembler.dissemble(createdHeatTreatmentBatch);
//    createdRepresentation.setHeatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.IDLE.name());
//    return createdRepresentation;
//  }
//
//
//  public Furnace getFurnaceUsingTenantIdAndFurnaceId(long tenantId, long furnaceId) {
//    boolean isFurnaceOfTenantExists = furnaceService.isFurnaceByTenantExists(tenantId);
//    if (!isFurnaceOfTenantExists) {
//      log.error("Furnace={} for the tenant={} does not exist!", furnaceId, tenantId);
//      throw new ResourceNotFoundException("Furnace for the tenant does not exist!");
//    }
//    return furnaceService.getFurnaceByIdAndTenantId(furnaceId, tenantId);
//  }
//
//  public boolean isHeatTreatmentBatchAppliedOnFurnace(long furnaceId) {
//    Optional<HeatTreatmentBatch> heatTreatmentBatchOptional = heatTreatmentBatchRepository.findAppliedHeatTreatmentBatchOnFurnace(furnaceId);
//    if (heatTreatmentBatchOptional.isPresent()) {
//      log.info("HeatTreatmentBatch={} already applied on furnaceId={}", heatTreatmentBatchOptional.get().getId(), furnaceId);
//      return true;
//    }
//    return false;
//  }
//
//  //Start HeatTreatmentBatch (Update start Time, Update Item Status, Update FurnaceStatus)
//  @Transactional
//  public HeatTreatmentBatchRepresentation startHeatTreatmentBatch(long tenantId, long furnaceId, long heatTreatmentBatchId, String startAt) {
//    tenantService.validateTenantExists(tenantId);
//    Furnace furnace = getFurnaceUsingTenantIdAndFurnaceId(tenantId, furnaceId);
//    boolean isHeatTreatmentBatchAppliedOnFurnace = isHeatTreatmentBatchAppliedOnFurnace(furnaceId);
//
//    if (!isHeatTreatmentBatchAppliedOnFurnace) {
//      log.error("Furnace={} does not have a heatTreatmentBatch set. Can not start forge existingHeatTreatmentBatch on this furnace as it does not have heatTreatmentBatch", furnaceId);
//      throw new ForgeNotFoundException("heatTreatmentBatch does not exists for furnace!");
//    }
//    HeatTreatmentBatch existingHeatTreatmentBatch = getHeatTreatmentBatchById(heatTreatmentBatchId);
//
//    if (existingHeatTreatmentBatch.getStartAt() != null) {
//      log.error("The heatTreatmentBatch={} for furnace={} has already been started!", heatTreatmentBatchId, furnace.getFurnaceName());
//      throw new HeatTreatmentBatchNotInExpectedStatusException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + "has already been started!");
//    }
//
//    if (!HeatTreatmentBatch.HeatTreatmentBatchStatus.IDLE.equals(existingHeatTreatmentBatch.getHeatTreatmentBatchStatus())) {
//      log.error("The heatTreatmentBatch={} for furnace={} is not in IDLE status to start it!", heatTreatmentBatchId, furnace.getFurnaceName());
//      throw new HeatTreatmentBatchNotInExpectedStatusException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + "Not in IDLE status to start it!");
//    }
//
//    existingHeatTreatmentBatch.setHeatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.IN_PROGRESS);
//    existingHeatTreatmentBatch.setStartAt(ConvertorUtils.convertStringToLocalDateTime(startAt));
//    existingHeatTreatmentBatch.getBatchItems().forEach(batchItem -> {
//      batchItem.getForge().getItem().setItemStatus(ItemStatus.HEAT_TREATMENT_IN_PROGRESS);
//    });
//
//    HeatTreatmentBatch startedHeatTreatmentBatch = heatTreatmentBatchRepository.save(existingHeatTreatmentBatch);
//
//    furnace.setFurnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_IN_PROGRESS);
//    furnaceService.saveFurnace(furnace);
//
//    return heatTreatmentBatchAssembler.dissemble(startedHeatTreatmentBatch);
//  }
//
//  //End HeatTreatmentBatch (Update End Time, Update Item Status, Update FurnaceStatus)
//
//  @Transactional
//  public HeatTreatmentBatchRepresentation endHeatTreatmentBatch(long tenantId, long furnaceId, long heatTreatmentBatchId, HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
//    tenantService.validateTenantExists(tenantId);
//    Furnace furnace = getFurnaceUsingTenantIdAndFurnaceId(tenantId, furnaceId);
//    boolean isHeatTreatmentBatchAppliedOnFurnace = isHeatTreatmentBatchAppliedOnFurnace(furnace.getId());
//
//    if (!isHeatTreatmentBatchAppliedOnFurnace) {
//      log.error("Furnace={} does not have a existing heatTreatmentBatch set. Can not end heatTreatmentBatch on this furnace as it does not have existing heatTreatmentBatch", furnaceId);
//      throw new HeatTreatmentBatchNotFoundException("HeatTreatmentBatch does not exists for furnace=!" + furnaceId);
//    }
//    HeatTreatmentBatch existingHeatTreatmentBatch = getHeatTreatmentBatchById(heatTreatmentBatchId);
//
//    if (existingHeatTreatmentBatch.getEndAt() != null) {
//      log.error("The heatTreatmentBatch={} for furnace={} has already been ended!", heatTreatmentBatchId, furnace.getFurnaceName());
//      throw new HeatTreatmentBatchNotInExpectedStatusException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + "has already been ended!");
//    }
//
//    if (!HeatTreatmentBatch.HeatTreatmentBatchStatus.IN_PROGRESS.equals(existingHeatTreatmentBatch.getHeatTreatmentBatchStatus())) {
//      log.error("The heatTreatmentBatch={} for furnace={} is not in IN_PROGRESS status to end it!", heatTreatmentBatchId, furnace.getFurnaceName());
//      throw new HeatTreatmentBatchNotInExpectedStatusException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + "Not in IN_PROGRESS status to end it!");
//    }
//    LocalDateTime endAt = ConvertorUtils.convertStringToLocalDateTime(heatTreatmentBatchRepresentation.getEndAt());
//    if (existingHeatTreatmentBatch.getStartAt().compareTo(endAt) >= 0) {
//      log.error("The heatTreatmentBatch={} for furnace={} end time is before or equal to start time!", heatTreatmentBatchId, furnace.getFurnaceName());
//      throw new RuntimeException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + " end time is before or equal to start time!");
//    }
//
//    existingHeatTreatmentBatch.setHeatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.COMPLETED);
//    existingHeatTreatmentBatch.setEndAt(endAt);
//
//    existingHeatTreatmentBatch.getBatchItems().forEach(batchItem -> {
//      if (batchItem.getHeatTreatBatchPiecesCount().equals(batchItem.getAvailableForgedPiecesCount())) {
//        Item item = batchItem.getForge().getItem();
//        item.setItemStatus(ItemStatus.HEAT_TREATMENT_COMPLETED);
//        itemService.saveItem(item);
//      }
//    });
//
//    HeatTreatmentBatch completedHeatTreatmentBatch = heatTreatmentBatchRepository.save(existingHeatTreatmentBatch);
//
//    furnace.setFurnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_NOT_APPLIED);
//    furnaceService.saveFurnace(furnace);
//
//    return heatTreatmentBatchAssembler.dissemble(completedHeatTreatmentBatch);
//  }
//
//
//  public HeatTreatmentBatch getHeatTreatmentBatchById(long heatTreatmentBatchId) {
//    Optional<HeatTreatmentBatch> heatTreatmentBatchOptional = heatTreatmentBatchRepository.findByIdAndDeletedFalse(heatTreatmentBatchId);
//    if (heatTreatmentBatchOptional.isEmpty()) {
//      log.error("HeatTreatmentBatch does not exists for heatTreatmentBatchId={}", heatTreatmentBatchId);
//      throw new ForgeNotFoundException("Forge does not exists for heatTreatmentBatchId=" + heatTreatmentBatchId);
//    }
//    return heatTreatmentBatchOptional.get();
//  }
//
//}
