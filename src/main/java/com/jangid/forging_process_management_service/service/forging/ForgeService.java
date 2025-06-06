package com.jangid.forging_process_management_service.service.forging;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.forging.ForgeHeat;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entities.forging.ItemWeightType;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotInExpectedStatusException;
import com.jangid.forging_process_management_service.exception.forging.ForgingLineOccupiedException;
import com.jangid.forging_process_management_service.repositories.forging.ForgeRepository;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.dto.ForgeTraceabilitySearchResultDTO;
import com.jangid.forging_process_management_service.assemblers.dispatch.DispatchBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.heating.HeatTreatmentBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.machining.MachiningBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.quality.InspectionBatchAssembler;
import com.jangid.forging_process_management_service.repositories.heating.HeatTreatmentBatchRepository;
import com.jangid.forging_process_management_service.repositories.machining.MachiningBatchRepository;
import com.jangid.forging_process_management_service.repositories.quality.InspectionBatchRepository;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchBatchRepository;
import com.jangid.forging_process_management_service.assemblers.forging.ForgeHeatAssembler;
import com.jangid.forging_process_management_service.entities.forging.ForgeShift;
import com.jangid.forging_process_management_service.entities.forging.ForgeShiftHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeShiftRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeShiftHeatRepresentation;
import com.jangid.forging_process_management_service.repositories.forging.ForgeShiftRepository;
import com.jangid.forging_process_management_service.assemblers.forging.ForgeShiftAssembler;
import com.jangid.forging_process_management_service.assemblers.forging.ForgeShiftHeatAssembler;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;

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

  @Autowired
  private ForgeHeatAssembler forgeHeatAssembler;

  @Autowired
  private HeatTreatmentBatchAssembler heatTreatmentBatchAssembler;
  
  @Autowired
  private MachiningBatchAssembler machiningBatchAssembler;
  
  @Autowired
  private InspectionBatchAssembler inspectionBatchAssembler;
  
  @Autowired
  private DispatchBatchAssembler dispatchBatchAssembler;

  @Autowired
  private HeatTreatmentBatchRepository heatTreatmentBatchRepository;
  
  @Autowired
  private MachiningBatchRepository machiningBatchRepository;
  
  @Autowired
  private InspectionBatchRepository inspectionBatchRepository;
  
  @Autowired
  private DispatchBatchRepository dispatchBatchRepository;

  @Autowired
  private ForgeShiftRepository forgeShiftRepository;

  @Autowired
  private ForgeShiftAssembler forgeShiftAssembler;

  @Autowired
  private ForgeShiftHeatAssembler forgeShiftHeatAssembler;

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

      Heat heat = rawMaterialHeatService.getRawMaterialHeatById(forgeHeat.getHeat().getId());
      double newHeatQuantity = heat.getAvailableHeatQuantity() - Double.parseDouble(forgeHeat.getHeatQuantityUsed());
      if (newHeatQuantity < 0) {
        log.error("Insufficient heat quantity for heat={} on tenantId={}", heat.getId(), tenantId);
        throw new IllegalArgumentException("Insufficient heat quantity for heat " + heat.getId());
      }
      LocalDateTime heatReceivingDateTime = heat.getRawMaterialProduct().getRawMaterial().getRawMaterialReceivingDate();
      if (heatReceivingDateTime.compareTo(applyAtLocalDateTime) > 0) {
        log.error("The provided apply at time={} is before to heat={} received at time={} !", applyAtLocalDateTime,
                  heat.getHeatNumber(), heatReceivingDateTime);
        throw new RuntimeException(
            "The provided apply at time=" + applyAtLocalDateTime + " is before to heat=" + heat.getHeatNumber() + " received at time=" + heatReceivingDateTime
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
    
    // Get the itemWeightType from the inputForge entity
    ItemWeightType weightType = inputForge.getItemWeightType();
    
    // Determine which weight to use based on itemWeightType
    Double itemWeight = determineItemWeight(item, weightType);
    
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

  private String getForgeTraceabilityNumber(long tenantId, String forgingLineName, long forgingLineId, LocalDateTime startAt) {
    Tenant tenant = tenantService.getTenantById(tenantId);
    String initialsOfTenant = getInitialsOfTenant(tenant.getTenantName());
    String localDate = startAt.toLocalDate().toString();

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
    existingForge.setForgeTraceabilityNumber(getForgeTraceabilityNumber(tenantId, forgingLine.getForgingLineName(), forgingLineId, startTimeLocalDateTime));

    Forge startedForge = forgeRepository.save(existingForge);

    forgingLine.setForgingLineStatus(ForgingLine.ForgingLineStatus.FORGE_IN_PROGRESS);
    forgingLineService.saveForgingLine(forgingLine);

    return forgeAssembler.dissemble(startedForge);
  }

  @Transactional
  public ForgeRepresentation endForgePrevious(long tenantId, long forgingLineId, long forgeId, ForgeRepresentation representation) {
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
    if (existingForge.getStartAt().compareTo(endAt) > 0) {
      log.error("The forge={} having traceability={} end time is before the forge start time!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new RuntimeException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + " end time is before the forge start time!");
    }

    int actualForgedPieces = getActualForgedPieces(representation.getActualForgeCount());
    int expectedPieces = existingForge.getProcessedItem().getExpectedForgePiecesCount();
    
    // Get the itemWeightType from the existing forge entity
    ItemWeightType weightType = existingForge.getItemWeightType();
    
    // Determine which weight to use based on itemWeightType
    Double itemWeight = determineItemWeight(existingForge.getProcessedItem().getItem(), weightType);

    // Process rejections if needed
    int rejectedPiecesCount = 0;
    double otherRejectionsKg = 0.0;
    boolean hasRejections = representation.getRejection() != null && representation.getRejection();
    
    if (hasRejections) {
      // Validate rejection data
      if (representation.getRejectedForgePiecesCount() == null || representation.getRejectedForgePiecesCount().isEmpty()) {
        log.error("Rejection flag is true but rejectedForgePiecesCount is not provided");
        throw new IllegalArgumentException("Rejection flag is true but rejectedForgePiecesCount is not provided");
      }
      
      // Parse rejected pieces count
      try {
        rejectedPiecesCount = Integer.parseInt(representation.getRejectedForgePiecesCount());
      } catch (NumberFormatException e) {
        log.error("Invalid rejected pieces count format: {}", representation.getRejectedForgePiecesCount());
        throw new IllegalArgumentException("Invalid rejected pieces count format: " + representation.getRejectedForgePiecesCount());
      }
      
      // Parse other rejections in kg if provided
      if (representation.getOtherForgeRejectionsKg() != null && !representation.getOtherForgeRejectionsKg().isEmpty()) {
        try {
          otherRejectionsKg = Double.parseDouble(representation.getOtherForgeRejectionsKg());
        } catch (NumberFormatException e) {
          log.error("Invalid other rejections kg format: {}", representation.getOtherForgeRejectionsKg());
          throw new IllegalArgumentException("Invalid other rejections kg format: " + representation.getOtherForgeRejectionsKg());
        }
      }
      
      // Validate that each ForgeHeat has rejection data if rejection flag is true
      for (ForgeHeatRepresentation forgeHeatRep : representation.getForgeHeats()) {
        if (forgeHeatRep.getHeatQuantityUsedInRejectedPieces() == null || forgeHeatRep.getHeatQuantityUsedInRejectedPieces().isEmpty()) {
          String identifier = forgeHeatRep.getId() != null ? "ID=" + forgeHeatRep.getId() : "HeatID=" + forgeHeatRep.getHeatId();
          log.error("Forge heat {} is missing heatQuantityUsedInRejectedPieces data", identifier);
          throw new IllegalArgumentException("Forge heat " + identifier + " is missing heatQuantityUsedInRejectedPieces data");
        }
        
        // Validate rejectedPieces field for each forge heat
        if (forgeHeatRep.getRejectedPieces() == null || forgeHeatRep.getRejectedPieces().isEmpty()) {
          String identifier = forgeHeatRep.getId() != null ? "ID=" + forgeHeatRep.getId() : "HeatID=" + forgeHeatRep.getHeatId();
          log.error("Forge heat {} is missing rejectedPieces data", identifier);
          throw new IllegalArgumentException("Forge heat " + identifier + " is missing rejectedPieces data");
        }
      }
    }

    // Calculate the difference in pieces
    int pieceDifference = expectedPieces - actualForgedPieces;

    // Separate existing and new forge heats from the representation
    List<ForgeHeatRepresentation> existingForgeHeats = representation.getForgeHeats().stream()
        .filter(fhr -> fhr.getId() != null)
        .collect(Collectors.toList());
    
    List<ForgeHeatRepresentation> newForgeHeats = representation.getForgeHeats().stream()
        .filter(fhr -> fhr.getId() == null && fhr.getHeatId() != null)
        .collect(Collectors.toList());

    // Create a map of existing forge heat IDs to their new quantities
    Map<Long, Double> existingHeatQuantities = existingForgeHeats.stream()
        .collect(Collectors.toMap(
            ForgeHeatRepresentation::getId,
            fhr -> Double.parseDouble(fhr.getHeatQuantityUsed())
        ));

    // Calculate total heat quantity for new forge heats
    double newHeatQuantityTotal = newForgeHeats.stream()
        .mapToDouble(fhr -> Double.parseDouble(fhr.getHeatQuantityUsed()))
        .sum();

    // Validate that we have matching quantities for all existing forge heats
    for (ForgeHeat existingForgeHeat : existingForge.getForgeHeats()) {
        if (!existingHeatQuantities.containsKey(existingForgeHeat.getId())) {
            log.error("Missing heat quantity update for forge heat ID={}", existingForgeHeat.getId());
            throw new IllegalArgumentException("Missing heat quantity update for forge heat ID=" + existingForgeHeat.getId());
        }
    }

    // Calculate required total heat quantity based on actual forged pieces and rejections
    double requiredTotalHeatQuantity = actualForgedPieces * itemWeight;
    
    // If has rejections, add rejected pieces weight and other rejections
    if (hasRejections) {
        // Validate the sum of rejected pieces across all forge heats
        double totalRejectedPiecesHeatQuantity = 0.0;
        double totalOtherRejectionsHeatQuantity = 0.0;
        int totalRejectedPiecesFromHeats = 0;
        
        for (ForgeHeatRepresentation forgeHeatRep : representation.getForgeHeats()) {
            if (forgeHeatRep.getHeatQuantityUsedInRejectedPieces() != null && !forgeHeatRep.getHeatQuantityUsedInRejectedPieces().isEmpty()) {
                totalRejectedPiecesHeatQuantity += Double.parseDouble(forgeHeatRep.getHeatQuantityUsedInRejectedPieces());
            }
            
            if (forgeHeatRep.getHeatQuantityUsedInOtherRejections() != null && !forgeHeatRep.getHeatQuantityUsedInOtherRejections().isEmpty()) {
                totalOtherRejectionsHeatQuantity += Double.parseDouble(forgeHeatRep.getHeatQuantityUsedInOtherRejections());
            }
            
            // Add up rejected pieces from all forge heats
            if (forgeHeatRep.getRejectedPieces() != null && !forgeHeatRep.getRejectedPieces().isEmpty()) {
                totalRejectedPiecesFromHeats += Integer.parseInt(forgeHeatRep.getRejectedPieces());
            }
        }
        
        // Add rejections to the required total heat quantity
        requiredTotalHeatQuantity += totalRejectedPiecesHeatQuantity + totalOtherRejectionsHeatQuantity;
        
        // Validate that total rejected pieces heat quantity matches expected value
        double expectedRejectedPiecesHeatQuantity = rejectedPiecesCount * itemWeight;
        if (Math.abs(totalRejectedPiecesHeatQuantity - expectedRejectedPiecesHeatQuantity) > 0.0001) {
            log.error("Sum of heat quantity used in rejected pieces ({}) does not match expected value ({})",
                     totalRejectedPiecesHeatQuantity, expectedRejectedPiecesHeatQuantity);
            throw new IllegalArgumentException(
                String.format("Sum of heat quantity used in rejected pieces (%.2f) must equal expected value (%.2f)",
                            totalRejectedPiecesHeatQuantity, expectedRejectedPiecesHeatQuantity)
            );
        }
        
        // Validate that total other rejections heat quantity matches otherRejectionsKg
        if (Math.abs(totalOtherRejectionsHeatQuantity - otherRejectionsKg) > 0.0001) {
            log.error("Sum of heat quantity used in other rejections ({}) does not match provided value ({})",
                     totalOtherRejectionsHeatQuantity, otherRejectionsKg);
            throw new IllegalArgumentException(
                String.format("Sum of heat quantity used in other rejections (%.2f) must equal provided value (%.2f)",
                            totalOtherRejectionsHeatQuantity, otherRejectionsKg)
            );
        }
        
        // Validate that total rejected pieces from all forge heats matches rejectedPiecesCount
        if (totalRejectedPiecesFromHeats != rejectedPiecesCount) {
            log.error("Sum of rejected pieces from all forge heats ({}) does not match total rejected pieces count ({})",
                     totalRejectedPiecesFromHeats, rejectedPiecesCount);
            throw new IllegalArgumentException(
                String.format("Sum of rejected pieces from all forge heats (%d) must equal total rejected pieces count (%d)",
                            totalRejectedPiecesFromHeats, rejectedPiecesCount)
            );
        }
    }

    // Validate total heat quantities (existing + new) match required quantity
    double totalHeatQuantity = existingHeatQuantities.values().stream().mapToDouble(Double::doubleValue).sum() + newHeatQuantityTotal;
    if (Math.abs(totalHeatQuantity - requiredTotalHeatQuantity) > 0.0001) {
        log.error("Total heat quantities ({}) do not match required quantity for actual forged pieces and rejections ({})", 
                 totalHeatQuantity, requiredTotalHeatQuantity);
        throw new IllegalArgumentException(
            String.format("Total heat quantities (%.2f) must equal the required material for actual forged pieces and rejections (%.2f)",
                        totalHeatQuantity, requiredTotalHeatQuantity)
        );
    }

    // Process each existing forge heat
    for (ForgeHeat existingForgeHeat : existingForge.getForgeHeats()) {
        double newHeatQuantity = existingHeatQuantities.get(existingForgeHeat.getId());
        double originalHeatQuantity = existingForgeHeat.getHeatQuantityUsed();
        double quantityDifference = originalHeatQuantity - newHeatQuantity;

        // Update forge heat quantity
        existingForgeHeat.setHeatQuantityUsed(newHeatQuantity);
        
        // Set rejection-related quantities if applicable
        if (hasRejections) {
            // Find the corresponding forge heat representation
            Optional<ForgeHeatRepresentation> forgeHeatRepOpt = existingForgeHeats.stream()
                .filter(fhr -> fhr.getId().equals(existingForgeHeat.getId()))
                .findFirst();
            
            if (forgeHeatRepOpt.isPresent()) {
                ForgeHeatRepresentation forgeHeatRep = forgeHeatRepOpt.get();
                
                if (forgeHeatRep.getHeatQuantityUsedInRejectedPieces() != null && !forgeHeatRep.getHeatQuantityUsedInRejectedPieces().isEmpty()) {
                    double rejectedPiecesHeatQuantity = Double.parseDouble(forgeHeatRep.getHeatQuantityUsedInRejectedPieces());
                    existingForgeHeat.setHeatQuantityUsedInRejectedPieces(rejectedPiecesHeatQuantity);
                }
                
                if (forgeHeatRep.getHeatQuantityUsedInOtherRejections() != null && !forgeHeatRep.getHeatQuantityUsedInOtherRejections().isEmpty()) {
                    double otherRejectionsHeatQuantity = Double.parseDouble(forgeHeatRep.getHeatQuantityUsedInOtherRejections());
                    existingForgeHeat.setHeatQuantityUsedInOtherRejections(otherRejectionsHeatQuantity);
                }
                
                // Set rejected pieces count if provided
                if (forgeHeatRep.getRejectedPieces() != null && !forgeHeatRep.getRejectedPieces().isEmpty()) {
                    int rejectedPiecesFromHeat = Integer.parseInt(forgeHeatRep.getRejectedPieces());
                    existingForgeHeat.setRejectedPieces(rejectedPiecesFromHeat);
                }
            }
        }

        // Update heat's available quantity
        Heat heat = existingForgeHeat.getHeat();
        double newAvailableQuantity = heat.getAvailableHeatQuantity() + quantityDifference;

        // Validate new available quantity
        if (newAvailableQuantity < 0) {
            log.error("Insufficient heat quantity available for heat={}", heat.getId());
            throw new IllegalArgumentException("Insufficient heat quantity available for heat " + heat.getId());
        }

        heat.setAvailableHeatQuantity(newAvailableQuantity);
        rawMaterialHeatService.updateRawMaterialHeat(heat);

        log.info("Updated existing heat ID={}: original quantity={}, new quantity={}, difference={}",
                heat.getId(), originalHeatQuantity, newHeatQuantity, quantityDifference);
    }

    // Process new forge heats
    for (ForgeHeatRepresentation newForgeHeatRep : newForgeHeats) {
        // Get the heat entity to validate quantity before creating ForgeHeat
        Heat heat = rawMaterialHeatService.getRawMaterialHeatById(newForgeHeatRep.getHeatId());
        double heatQuantityToConsume = Double.parseDouble(newForgeHeatRep.getHeatQuantityUsed());
        
        // Validate heat has sufficient quantity
        if (heat.getAvailableHeatQuantity() < heatQuantityToConsume) {
            log.error("Insufficient heat quantity available for heat={}, required={}, available={}", 
                     heat.getId(), heatQuantityToConsume, heat.getAvailableHeatQuantity());
            throw new IllegalArgumentException("Insufficient heat quantity available for heat " + heat.getId());
        }
        
        // Create new ForgeHeat entity using assembler
        ForgeHeat newForgeHeat = forgeHeatAssembler.createAssembleFromHeatId(newForgeHeatRep);
        
        // Set the forge reference
        newForgeHeat.setForge(existingForge);
        
        // Update heat's available quantity
        double newAvailableQuantity = heat.getAvailableHeatQuantity() - heatQuantityToConsume;
        heat.setAvailableHeatQuantity(newAvailableQuantity);
        rawMaterialHeatService.updateRawMaterialHeat(heat);
        
        // Add to the forge's heat list
        existingForge.getForgeHeats().add(newForgeHeat);
        
        log.info("Added new forge heat for heat ID={}: quantity consumed={}, new available quantity={}",
                heat.getId(), heatQuantityToConsume, newAvailableQuantity);
    }

    existingForge.setForgingStatus(Forge.ForgeStatus.COMPLETED);
    ProcessedItem existingForgeProcessedItem = existingForge.getProcessedItem();
    existingForgeProcessedItem.setActualForgePiecesCount(actualForgedPieces);
    existingForgeProcessedItem.setAvailableForgePiecesCountForHeat(actualForgedPieces);
    
    // Set rejection data if applicable
    if (hasRejections) {
        existingForgeProcessedItem.setRejectedForgePiecesCount(rejectedPiecesCount);
        existingForgeProcessedItem.setOtherForgeRejectionsKg(otherRejectionsKg);
    }
    
    existingForge.setProcessedItem(existingForgeProcessedItem);
    existingForge.setEndAt(endAt);

    Forge completedForge = forgeRepository.save(existingForge);

    forgingLine.setForgingLineStatus(ForgingLine.ForgingLineStatus.FORGE_NOT_APPLIED);
    forgingLineService.saveForgingLine(forgingLine);

    ForgeRepresentation result = forgeAssembler.dissemble(completedForge);
    result.setItemWeightType(representation.getItemWeightType()); // Preserve the weight type used
    return result;
  }

  @Transactional
  public ForgeRepresentation endForge(long tenantId, long forgingLineId, long forgeId, String endAt) {
    // 1. Validate tenant exists
    tenantService.validateTenantExists(tenantId);
    
    // 2. Validate forging line exists for tenant
    ForgingLine forgingLine = getForgingLineUsingTenantIdAndForgingLineId(tenantId, forgingLineId);
    
    // 3. Validate forge exists and is applied on the forging line
    boolean isForgeAppliedOnForgingLine = isForgeAppliedOnForgingLine(forgingLine.getId());
    if (!isForgeAppliedOnForgingLine) {
      log.error("ForgingLine={} does not have a forge set. Cannot end forge on this forging line", forgingLineId);
      throw new ForgeNotFoundException("Forge does not exists for forgingLine!");
    }
    
    // 4. Get and validate the forge
    Forge existingForge = getForgeById(forgeId);
    
    if (existingForge.getEndAt() != null) {
      log.error("The forge={} having traceability={} has already been ended!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new ForgeNotInExpectedStatusException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + " has already been ended!");
    }

    if (!Forge.ForgeStatus.IN_PROGRESS.equals(existingForge.getForgingStatus())) {
      log.error("The forge={} having traceability={} is not in IN_PROGRESS status to end it!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new ForgeNotInExpectedStatusException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + " not in IN_PROGRESS status to end it!");
    }
    
    // 5. Validate end time
    LocalDateTime endDateTime = ConvertorUtils.convertStringToLocalDateTime(endAt);
    
    if (existingForge.getStartAt().compareTo(endDateTime) > 0) {
      log.error("The forge={} having traceability={} end time is before the forge start time!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new RuntimeException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + " end time is before the forge start time!");
    }
    
    // Validate end time is after the last forge shift end time
    if (existingForge.getForgeShifts() != null && !existingForge.getForgeShifts().isEmpty()) {
      ForgeShift latestShift = existingForge.getLatestForgeShift();
      if (latestShift != null && endDateTime.compareTo(latestShift.getEndDateTime()) < 0) {
        log.error("Forge end time {} is before the latest forge shift end time {}", endDateTime, latestShift.getEndDateTime());
        throw new IllegalArgumentException("Forge end time must be equal to or after the latest forge shift end time");
      }
    }
    
    // 6. Process heat quantity adjustments based on forge shifts usage
    processHeatQuantityAdjustmentsOnForgeCompletion(existingForge);
    
    // 7. Update processedItem fields based on forge shifts totals
    updateProcessedItemFromForgeShifts(existingForge);
    
    // 8. Mark forge as completed
    existingForge.setForgingStatus(Forge.ForgeStatus.COMPLETED);
    existingForge.setEndAt(endDateTime);
    
    // 9. Save the completed forge
    Forge completedForge = forgeRepository.save(existingForge);
    
    // 10. Update forging line status
    forgingLine.setForgingLineStatus(ForgingLine.ForgingLineStatus.FORGE_NOT_APPLIED);
    forgingLineService.saveForgingLine(forgingLine);
    
    log.info("Forge ID={} completed successfully at {}", forgeId, endDateTime);
    return forgeAssembler.dissemble(completedForge);
  }

  /**
   * Processes heat quantity adjustments when completing a forge
   * Returns unused heat quantities back to inventory if total forge shifts usage is less than original allocation
   */
  private void processHeatQuantityAdjustmentsOnForgeCompletion(Forge forge) {
    log.info("Processing heat quantity adjustments for forge completion, forge ID={}", forge.getId());
    
    // Get item weight for calculations
    ItemWeightType weightType = forge.getItemWeightType();
    Double itemWeight = determineItemWeight(forge.getProcessedItem().getItem(), weightType);
    
    // Create a map of original forge heat allocations (heatId -> quantity allocated during applyForge)
    Map<Long, Double> originalHeatAllocations = forge.getForgeHeats().stream()
        .collect(Collectors.toMap(
            fh -> fh.getHeat().getId(),
            ForgeHeat::getHeatQuantityUsed
        ));
    
    log.info("Original heat allocations: {}", originalHeatAllocations);
    
    // Calculate total usage of each heat across all forge shifts
    Map<Long, Double> totalUsageByHeat = new HashMap<>();
    
    if (forge.getForgeShifts() != null && !forge.getForgeShifts().isEmpty()) {
      for (ForgeShift forgeShift : forge.getForgeShifts()) {
        if (!forgeShift.isDeleted()) {
          for (ForgeShiftHeat shiftHeat : forgeShift.getForgeShiftHeats()) {
            if (!shiftHeat.isDeleted()) {
              Long heatId = shiftHeat.getHeat().getId();
              double usage = shiftHeat.getHeatQuantityUsed();
              totalUsageByHeat.merge(heatId, usage, Double::sum);
            }
          }
        }
      }
    }
    
    log.info("Total usage by heat across all forge shifts: {}", totalUsageByHeat);
    
    // Process each original heat allocation
    for (Map.Entry<Long, Double> entry : originalHeatAllocations.entrySet()) {
      Long heatId = entry.getKey();
      double originalAllocation = entry.getValue();
      double totalUsage = totalUsageByHeat.getOrDefault(heatId, 0.0);
      
      // Find the corresponding ForgeHeat entity to update
      ForgeHeat forgeHeat = forge.getForgeHeats().stream()
          .filter(fh -> fh.getHeat().getId().equals(heatId))
          .findFirst()
          .orElse(null);
      
      if (totalUsage < originalAllocation) {
        // Return unused quantity back to heat inventory
        double unusedQuantity = originalAllocation - totalUsage;
        
        Heat heat = rawMaterialHeatService.getRawMaterialHeatById(heatId);
        double newAvailableQuantity = heat.getAvailableHeatQuantity() + unusedQuantity;
        heat.setAvailableHeatQuantity(newAvailableQuantity);
        rawMaterialHeatService.updateRawMaterialHeat(heat);
        
        // Record the returned quantity in ForgeHeat for audit trail
        if (forgeHeat != null) {
          forgeHeat.setHeatQuantityReturned(unusedQuantity);
        }
        
        log.info("Returned unused heat quantity for heat ID={}: original allocation={}, total usage={}, returned={}",
                heatId, originalAllocation, totalUsage, unusedQuantity);
      } else if (totalUsage > originalAllocation) {
        // This should not happen as forge shifts should have already deducted extra usage
        log.warn("Total usage ({}) exceeds original allocation ({}) for heat ID={}. This should have been handled during forge shifts.",
                totalUsage, originalAllocation, heatId);
      } else {
        log.info("Heat ID={}: Total usage matches original allocation ({}), no adjustment needed", heatId, originalAllocation);
        
        // Set returned quantity to 0 for clarity in audit trail
        if (forgeHeat != null) {
          forgeHeat.setHeatQuantityReturned(0.0);
        }
      }
    }
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
        throw new IllegalStateException("This forging cannot be deleted as it is not in the COMPLETED status.");
    }

    // 4. Check if forge's processed item is used in any active heat treatment batches
    ProcessedItem processedItem = forge.getProcessedItem();
    boolean hasActiveHeatTreatmentBatches = processedItem.getProcessedItemHeatTreatmentBatches().stream()
        .anyMatch(batch -> !batch.isDeleted() && !batch.getHeatTreatmentBatch().isDeleted());

    if (hasActiveHeatTreatmentBatches) {
        log.error("Cannot delete forge id={} as it has active heat treatment batches", forgeId);
        throw new IllegalStateException("This forging cannot be deleted as a heat treatment batch entry exists for it.");
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

  /**
   * Search for a forge and all its related entities by forge traceability number
   * @param forgeTraceabilityNumber The forge traceability number to search for
   * @return A DTO containing the forge and related entities information
   */
  @Transactional(readOnly = true)
  public ForgeTraceabilitySearchResultDTO searchByForgeTraceabilityNumber(String forgeTraceabilityNumber) {
    Forge forge = getForgeByForgeTraceabilityNumber(forgeTraceabilityNumber);
    ForgeRepresentation forgeRepresentation = forgeAssembler.dissemble(forge);
    
    // Find related heat treatment batches using repository
    List<HeatTreatmentBatchRepresentation> heatTreatmentBatchRepresentations = heatTreatmentBatchRepository
        .findByForgeTraceabilityNumber(forgeTraceabilityNumber)
        .stream()
        .map(heatTreatmentBatchAssembler::dissemble)
        .collect(Collectors.toList());
    
    // Find related machining batches using repository
    List<MachiningBatchRepresentation> machiningBatchRepresentations = machiningBatchRepository
        .findByForgeTraceabilityNumber(forgeTraceabilityNumber)
        .stream()
        .map(machiningBatchAssembler::dissemble)
        .collect(Collectors.toList());
    
    // Find related inspection batches using repository
    List<InspectionBatchRepresentation> inspectionBatchRepresentations = inspectionBatchRepository
        .findByForgeTraceabilityNumber(forgeTraceabilityNumber)
        .stream()
        .map(inspectionBatchAssembler::dissemble)
        .collect(Collectors.toList());
    
    // Find related dispatch batches using repository
    List<DispatchBatchRepresentation> dispatchBatchRepresentations = dispatchBatchRepository
        .findByForgeTraceabilityNumber(forgeTraceabilityNumber)
        .stream()
        .map(dispatchBatchAssembler::dissemble)
        .collect(Collectors.toList());
    
    return ForgeTraceabilitySearchResultDTO.builder()
        .forge(forgeRepresentation)
        .heatTreatmentBatches(heatTreatmentBatchRepresentations)
        .machiningBatches(machiningBatchRepresentations)
        .inspectionBatches(inspectionBatchRepresentations)
        .dispatchBatches(dispatchBatchRepresentations)
        .build();
  }

  /**
   * Determines which weight value to use based on the specified weight type
   * @param item The item from which to extract the weight
   * @param weightType The type of weight to use from the ItemWeightType enum
   * @return The appropriate weight value based on the weight type
   * @throws IllegalArgumentException if the selected weight type is null for the item
   */
  private Double determineItemWeight(Item item, ItemWeightType weightType) {
    Double itemWeight;
    
    if (weightType == null) {
        weightType = ItemWeightType.getDefault();
    }
    
    switch (weightType) {
        case ITEM_SLUG_WEIGHT:
            itemWeight = item.getItemSlugWeight();
            log.info("Using item slug weight: {}", itemWeight);
            break;
        case ITEM_FORGED_WEIGHT:
            itemWeight = item.getItemForgedWeight();
            log.info("Using item forged weight: {}", itemWeight);
            break;
        case ITEM_FINISHED_WEIGHT:
            itemWeight = item.getItemFinishedWeight();
            log.info("Using item finished weight: {}", itemWeight);
            break;
        case ITEM_WEIGHT:
        default:
            itemWeight = item.getItemWeight();
            log.info("Using item weight: {}", itemWeight);
            break;
    }
    
    if (itemWeight == null) {
        log.error("Selected weight type {} is null for item {}", weightType, item.getId());
        throw new IllegalArgumentException("Selected weight type " + weightType + " is null for item " + item.getId());
    }
    
    return itemWeight;
  }

  /**
   * Search for forges by item name, forge traceability number, or forging line name with pagination
   * @param tenantId The tenant ID
   * @param searchType The type of search (ITEM_NAME, FORGE_TRACEABILITY_NUMBER, or FORGING_LINE_NAME)
   * @param searchTerm The search term (substring matching for all search types)
   * @param page The page number (0-based)
   * @param size The page size
   * @return Page of ForgeRepresentation containing the search results
   */
  @Transactional(readOnly = true)
  public Page<ForgeRepresentation> searchForges(Long tenantId, String searchType, String searchTerm, int page, int size) {
    if (searchTerm == null || searchTerm.trim().isEmpty() || searchType == null || searchType.trim().isEmpty()) {
      Pageable pageable = PageRequest.of(page, size);
      return Page.empty(pageable);
    }

    Pageable pageable = PageRequest.of(page, size);
    Page<Forge> forgePage;

    switch (searchType.toUpperCase()) {
      case "ITEM_NAME":
        forgePage = forgeRepository.findForgesByItemNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "FORGE_TRACEABILITY_NUMBER":
        forgePage = forgeRepository.findForgesByForgeTraceabilityNumberContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "FORGING_LINE_NAME":
        forgePage = forgeRepository.findForgesByForgingLineNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      default:
        log.error("Invalid search type: {}", searchType);
        throw new IllegalArgumentException("Invalid search type: " + searchType + ". Valid types are: ITEM_NAME, FORGE_TRACEABILITY_NUMBER, FORGING_LINE_NAME");
    }

    return forgePage.map(forgeAssembler::dissemble);
  }

  /**
   * Creates a new forge shift for an existing forge in IN_PROGRESS status
   * @param tenantId The tenant ID
   * @param forgingLineId The forging line ID
   * @param forgeId The forge ID
   * @param forgeShiftRepresentation The forge shift data
   * @return The created forge shift representation
   */
  @Transactional
  public ForgeShiftRepresentation createForgeShift(long tenantId, long forgingLineId, long forgeId, 
                                                   ForgeShiftRepresentation forgeShiftRepresentation) {
    // 1. Validate tenant exists
    tenantService.validateTenantExists(tenantId);
    
    // 2. Validate forging line exists for tenant
    ForgingLine forgingLine = getForgingLineUsingTenantIdAndForgingLineId(tenantId, forgingLineId);
    
    // 3. Validate forge exists and belongs to the forging line
    Forge forge = getForgeByIdAndForgingLineId(forgeId, forgingLine.getId());
    
    // 4. Validate forge is in IN_PROGRESS status
    if (Forge.ForgeStatus.IN_PROGRESS != forge.getForgingStatus()) {
      log.error("Forge={} is not in IN_PROGRESS status to add forge shift. Current status: {}", 
               forgeId, forge.getForgingStatus());
      throw new ForgeNotInExpectedStatusException("Forge must be in IN_PROGRESS status to add forge shift");
    }
    
    // 5. Parse and validate start and end times
    LocalDateTime startDateTime = ConvertorUtils.convertStringToLocalDateTime(forgeShiftRepresentation.getStartDateTime());
    LocalDateTime endDateTime = ConvertorUtils.convertStringToLocalDateTime(forgeShiftRepresentation.getEndDateTime());
    
    if (startDateTime.compareTo(endDateTime) >= 0) {
      log.error("Forge shift start time={} must be before end time={}", startDateTime, endDateTime);
      throw new IllegalArgumentException("Forge shift start time must be before end time");
    }
    
    // 6. Validate sequential shift timing
    validateForgeShiftSequencing(forge, startDateTime);
    
    // 7. Parse actual forged pieces count
    int actualForgedPiecesCount = Integer.parseInt(forgeShiftRepresentation.getActualForgedPiecesCount());
    
    // 8. Parse rejection data if applicable
    int rejectedPiecesCount = 0;
    double otherRejectionsKg = 0.0;
    boolean hasRejections = forgeShiftRepresentation.getRejection() != null && forgeShiftRepresentation.getRejection();
    
    if (hasRejections) {
      rejectedPiecesCount = Integer.parseInt(forgeShiftRepresentation.getRejectedForgePiecesCount());
      if (forgeShiftRepresentation.getOtherForgeRejectionsKg() != null && 
          !forgeShiftRepresentation.getOtherForgeRejectionsKg().isEmpty()) {
        otherRejectionsKg = Double.parseDouble(forgeShiftRepresentation.getOtherForgeRejectionsKg());
      }
    }
    
    // 9. Get item weight for calculations
    ItemWeightType weightType = forge.getItemWeightType();
    Double itemWeight = determineItemWeight(forge.getProcessedItem().getItem(), weightType);
    
    // 10. Validate forge shift heat data and totals
    validateForgeShiftHeatsAndTotals(forgeShiftRepresentation, actualForgedPiecesCount, 
                                     rejectedPiecesCount, otherRejectionsKg, itemWeight, hasRejections);
    
    // 11. Process heat allocations and create forge shift
    ForgeShift forgeShift = processForgeShiftWithProperHeatAllocation(forge, forgeShiftRepresentation, 
                                                                     startDateTime, endDateTime, 
                                                                     actualForgedPiecesCount, rejectedPiecesCount, 
                                                                     otherRejectionsKg, hasRejections, itemWeight);
    
    // 12. Save and return
    ForgeShift savedForgeShift = forgeShiftRepository.save(forgeShift);
    return forgeShiftAssembler.dissemble(savedForgeShift);
  }

  private void validateForgeShiftSequencing(Forge forge, LocalDateTime startDateTime) {
    // First forge shift must start after forge start time
    if (forge.getForgeShifts() == null || forge.getForgeShifts().isEmpty()) {
      if (forge.getStartAt() == null) {
        log.error("Cannot create forge shift before forge has been started");
        throw new IllegalStateException("Cannot create forge shift before forge has been started");
      }
      if (startDateTime.compareTo(forge.getStartAt()) <= 0) {
        log.error("First forge shift start time={} must be after forge start time={}", 
                 startDateTime, forge.getStartAt());
        throw new IllegalArgumentException("First forge shift must start after forge start time");
      }
    } else {
      // Subsequent shifts must start after the previous shift end time
      ForgeShift latestShift = forge.getLatestForgeShift();
      if (latestShift != null && startDateTime.compareTo(latestShift.getEndDateTime()) <= 0) {
        log.error("Forge shift start time={} must be after previous shift end time={}", 
                 startDateTime, latestShift.getEndDateTime());
        throw new IllegalArgumentException("Forge shift must start after previous shift end time");
      }
    }
  }

  /**
   * Validates forge shift heats and totals according to the new requirements
   */
  private void validateForgeShiftHeatsAndTotals(ForgeShiftRepresentation representation,
                                                int actualForgedPiecesCount, int rejectedPiecesCount, 
                                                double otherRejectionsKg, Double itemWeight, 
                                                boolean hasRejections) {
    if (representation.getForgeShiftHeats() == null || representation.getForgeShiftHeats().isEmpty()) {
      log.error("Forge shift must have at least one heat material");
      throw new IllegalArgumentException("Forge shift must have at least one heat material");
    }
    
    // Extract heatPieces from heatQuantityUsed for validation
    int totalHeatPieces = 0;
    int totalRejectedPieces = 0;
    double totalOtherRejections = 0.0;
    
    for (ForgeShiftHeatRepresentation heatRep : representation.getForgeShiftHeats()) {
      // Validate heat selection
      if (heatRep.getHeatId() == null && (heatRep.getHeat() == null || heatRep.getHeat().getId() == null)) {
        log.error("Heat selection is required for each forge shift heat");
        throw new IllegalArgumentException("Heat selection is required for each forge shift heat");
      }
      
      // Validate heat quantity used is provided
      if (heatRep.getHeatQuantityUsed() == null || heatRep.getHeatQuantityUsed().isEmpty()) {
        log.error("Heat quantity used is required for each forge shift heat");
        throw new IllegalArgumentException("Heat quantity used is required for each forge shift heat");
      }
      
      // Calculate pieces from the heat quantity and item weight
      double heatQuantityUsed = Double.parseDouble(heatRep.getHeatQuantityUsed());
      double rejectedPiecesQuantity = 0.0;
      double otherRejectionsQuantity = 0.0;
      int rejectedPiecesFromHeat = 0;
      
      if (hasRejections) {
        // Parse rejection data for this heat
        if (heatRep.getHeatQuantityUsedInRejectedPieces() != null && 
            !heatRep.getHeatQuantityUsedInRejectedPieces().isEmpty()) {
          rejectedPiecesQuantity = Double.parseDouble(heatRep.getHeatQuantityUsedInRejectedPieces());
        }
        
        if (heatRep.getHeatQuantityUsedInOtherRejections() != null && 
            !heatRep.getHeatQuantityUsedInOtherRejections().isEmpty()) {
          otherRejectionsQuantity = Double.parseDouble(heatRep.getHeatQuantityUsedInOtherRejections());
        }
        
        if (heatRep.getRejectedPieces() != null && !heatRep.getRejectedPieces().isEmpty()) {
          rejectedPiecesFromHeat = Integer.parseInt(heatRep.getRejectedPieces());
        }
        
        // Validate rejected pieces quantity matches pieces count * item weight
        double expectedRejectedPiecesQuantity = rejectedPiecesFromHeat * itemWeight;
        if (Math.abs(rejectedPiecesQuantity - expectedRejectedPiecesQuantity) > 0.0001) {
          log.error("Heat rejected pieces quantity ({}) does not match pieces count ({}) * item weight ({})",
                   rejectedPiecesQuantity, rejectedPiecesFromHeat, itemWeight);
          throw new IllegalArgumentException(
              String.format("Heat rejected pieces quantity (%.2f) must equal pieces count (%d) * item weight (%.2f)",
                          rejectedPiecesQuantity, rejectedPiecesFromHeat, itemWeight)
          );
        }
        
        totalRejectedPieces += rejectedPiecesFromHeat;
        totalOtherRejections += otherRejectionsQuantity;
      }
      
      // Calculate heat pieces from total heat quantity
      double expectedHeatQuantity = rejectedPiecesQuantity + otherRejectionsQuantity;
      double heatPiecesQuantity = heatQuantityUsed - expectedHeatQuantity;
      
      if (heatPiecesQuantity < 0 || Math.abs(heatPiecesQuantity % itemWeight) > 0.0001) {
        log.error("Invalid heat quantity calculation for heat. Total quantity: {}, rejections: {}, other: {}",
                 heatQuantityUsed, rejectedPiecesQuantity, otherRejectionsQuantity);
        throw new IllegalArgumentException("Heat quantity calculation is invalid - check pieces and rejection quantities");
      }
      
      int heatPieces = (int) Math.round(heatPiecesQuantity / itemWeight);
      totalHeatPieces += heatPieces;
      
        // Set the calculated heatPieces in the representation for later use
      heatRep.setHeatPieces(String.valueOf(heatPieces));
      
      // Validate the total heat quantity matches the formula
      double calculatedTotalQuantity = (heatPieces + rejectedPiecesFromHeat) * itemWeight + otherRejectionsQuantity;
      if (Math.abs(calculatedTotalQuantity - heatQuantityUsed) > 0.0001) {
        log.error("Heat quantity used ({}) does not match calculated quantity ({}) for heat pieces: {}, rejected: {}, other: {}",
                 heatQuantityUsed, calculatedTotalQuantity, heatPieces, rejectedPiecesFromHeat, otherRejectionsQuantity);
        throw new IllegalArgumentException(
            String.format("Heat quantity used (%.2f) must match calculated quantity (%.2f) based on pieces",
                        heatQuantityUsed, calculatedTotalQuantity)
        );
      }
    }
    
    // Validate totals match expected values
    if (totalHeatPieces != actualForgedPiecesCount) {
      log.error("Sum of heat pieces ({}) does not match actual forged pieces count ({})",
               totalHeatPieces, actualForgedPiecesCount);
      throw new IllegalArgumentException(
          String.format("Sum of heat pieces (%d) must equal actual forged pieces count (%d)",
                      totalHeatPieces, actualForgedPiecesCount)
      );
    }
    
    if (hasRejections) {
      if (totalRejectedPieces != rejectedPiecesCount) {
        log.error("Sum of heat rejected pieces ({}) does not match total rejected pieces count ({})",
                 totalRejectedPieces, rejectedPiecesCount);
        throw new IllegalArgumentException(
            String.format("Sum of heat rejected pieces (%d) must equal total rejected pieces count (%d)",
                        totalRejectedPieces, rejectedPiecesCount)
        );
      }
      
      if (Math.abs(totalOtherRejections - otherRejectionsKg) > 0.0001) {
        log.error("Sum of heat other rejections ({}) does not match total other rejections ({})",
                 totalOtherRejections, otherRejectionsKg);
        throw new IllegalArgumentException(
            String.format("Sum of heat other rejections (%.2f) must equal total other rejections (%.2f)",
                        totalOtherRejections, otherRejectionsKg)
        );
      }
    }
  }

  /**
   * Processes forge shift with proper heat allocation logic based on original applyForge quantities
   */
  private ForgeShift processForgeShiftWithProperHeatAllocation(Forge forge, ForgeShiftRepresentation representation,
                                                               LocalDateTime startDateTime, LocalDateTime endDateTime,
                                                               int actualForgedPiecesCount, int rejectedPiecesCount,
                                                               double otherRejectionsKg, boolean hasRejections,
                                                               Double itemWeight) {
    // Create a map of original forge heat allocations (heatId -> quantity allocated during applyForge)
    Map<Long, Double> originalHeatAllocations = forge.getForgeHeats().stream()
        .collect(Collectors.toMap(
            fh -> fh.getHeat().getId(),
            ForgeHeat::getHeatQuantityUsed
        ));
    
    log.info("Original heat allocations from applyForge: {}", originalHeatAllocations);
    
    // Calculate total usage of each heat across all existing forge shifts
    Map<Long, Double> totalExistingUsageByHeat = new HashMap<>();
    
    if (forge.getForgeShifts() != null && !forge.getForgeShifts().isEmpty()) {
      for (ForgeShift existingShift : forge.getForgeShifts()) {
        if (!existingShift.isDeleted()) {
          for (ForgeShiftHeat existingShiftHeat : existingShift.getForgeShiftHeats()) {
            if (!existingShiftHeat.isDeleted()) {
              Long heatId = existingShiftHeat.getHeat().getId();
              double heatUsage = existingShiftHeat.getHeatQuantityUsed();
              totalExistingUsageByHeat.merge(heatId, heatUsage, Double::sum);
            }
          }
        }
      }
    }
    
    log.info("Total existing usage by heat across all previous forge shifts: {}", totalExistingUsageByHeat);
    
    // Process each forge shift heat
    List<ForgeShiftHeat> forgeShiftHeats = new ArrayList<>();
    
    for (ForgeShiftHeatRepresentation heatRep : representation.getForgeShiftHeats()) {
      Long heatId = heatRep.getHeatId() != null ? heatRep.getHeatId() : heatRep.getHeat().getId();
      double currentShiftUsage = Double.parseDouble(heatRep.getHeatQuantityUsed());
      
      // Get the heat entity
      Heat heat = rawMaterialHeatService.getRawMaterialHeatById(heatId);
      
      // Calculate total usage including current shift
      double existingUsage = totalExistingUsageByHeat.getOrDefault(heatId, 0.0);
      double totalUsageWithCurrentShift = existingUsage + currentShiftUsage;
      
      // Check if this heat was part of the original applyForge allocation
      Double originalAllocation = originalHeatAllocations.get(heatId);
      
      if (originalAllocation != null) {
        // This heat was part of original allocation
        log.info("Heat ID={}: Current shift usage={}, Existing usage={}, Total usage={}, Original allocation={}", 
                heatId, currentShiftUsage, existingUsage, totalUsageWithCurrentShift, originalAllocation);
        
        if (totalUsageWithCurrentShift > originalAllocation) {
          // Total usage exceeds original allocation, need to deduct extra from inventory
          double totalExcess = totalUsageWithCurrentShift - originalAllocation;
          
          // However, we only need to deduct what hasn't been deducted yet in previous shifts
          double previousExcess = Math.max(0, existingUsage - originalAllocation);
          double additionalDeduction = totalExcess - previousExcess;
          
          if (additionalDeduction > 0) {
            // Validate heat has sufficient quantity for additional deduction
            if (heat.getAvailableHeatQuantity() < additionalDeduction) {
              log.error("Insufficient heat quantity for additional deduction. Heat ID={}, required additional={}, available={}",
                       heatId, additionalDeduction, heat.getAvailableHeatQuantity());
              throw new IllegalArgumentException(
                  String.format("Insufficient heat quantity for additional deduction. Heat %d requires %.2f kg additional but only %.2f kg available",
                              heatId, additionalDeduction, heat.getAvailableHeatQuantity())
              );
            }
            
            // Deduct additional usage from heat inventory
            double newAvailableQuantity = heat.getAvailableHeatQuantity() - additionalDeduction;
            heat.setAvailableHeatQuantity(newAvailableQuantity);
            rawMaterialHeatService.updateRawMaterialHeat(heat);
            
            log.info("Deducted additional usage from heat ID={}: total excess={}, previous excess={}, additional deduction={}, new available={}",
                    heatId, totalExcess, previousExcess, additionalDeduction, newAvailableQuantity);
          } else {
            log.info("Heat ID={}: No additional deduction needed, excess was already deducted in previous shifts", heatId);
          }
        } else {
          // Total usage is within original allocation, no deduction needed
          log.info("Heat ID={}: Total usage within original allocation, no additional deduction needed", heatId);
        }
      } else {
        // This is a new heat not part of original allocation, deduct full current shift usage
        log.info("Heat ID={}: New heat not in original allocation, deducting full current shift usage={}", heatId, currentShiftUsage);
        
        // Validate heat has sufficient quantity
        if (heat.getAvailableHeatQuantity() < currentShiftUsage) {
          log.error("Insufficient heat quantity for new heat. Heat ID={}, required={}, available={}",
                   heatId, currentShiftUsage, heat.getAvailableHeatQuantity());
          throw new IllegalArgumentException(
              String.format("Insufficient heat quantity for new heat. Heat %d requires %.2f kg but only %.2f kg available",
                          heatId, currentShiftUsage, heat.getAvailableHeatQuantity())
          );
        }
        
        // Deduct full current shift usage from heat inventory
        double newAvailableQuantity = heat.getAvailableHeatQuantity() - currentShiftUsage;
        heat.setAvailableHeatQuantity(newAvailableQuantity);
        rawMaterialHeatService.updateRawMaterialHeat(heat);
        
        log.info("Deducted full current shift usage from new heat ID={}: usage={}, new available={}",
                heatId, currentShiftUsage, newAvailableQuantity);
      }
      
      // Create forge shift heat entity
      ForgeShiftHeat forgeShiftHeat = forgeShiftHeatAssembler.createAssemble(heatRep);
      forgeShiftHeats.add(forgeShiftHeat);
    }
    
    // Create forge shift
    ForgeShift forgeShift = ForgeShift.builder()
        .forge(forge)
        .startDateTime(startDateTime)
        .endDateTime(endDateTime)
        .forgeShiftHeats(forgeShiftHeats)
        .actualForgedPiecesCount(actualForgedPiecesCount)
        .rejectedForgePiecesCount(rejectedPiecesCount)
        .otherForgeRejectionsKg(otherRejectionsKg)
        .rejection(hasRejections)
        .createdAt(LocalDateTime.now())
        .build();
    
    // Set forge shift reference in forge shift heats
    forgeShiftHeats.forEach(heat -> heat.setForgeShift(forgeShift));
    
    // Add forge shift to forge
    forge.addForgeShift(forgeShift);
    
    // Update processedItem fields based on total actual forged pieces across all forge shifts
    updateProcessedItemFromForgeShifts(forge);
    
    return forgeShift;
  }

  /**
   * Updates the processedItem fields based on the sum of all forge shifts' actual forged pieces count
   */
  private void updateProcessedItemFromForgeShifts(Forge forge) {
    // Calculate total actual forged pieces across all forge shifts
    int totalActualForgedPieces = forge.getForgeShifts().stream()
        .filter(shift -> !shift.isDeleted())
        .mapToInt(ForgeShift::getActualForgedPiecesCount)
        .sum();
    
    log.info("Updating processedItem for forge ID={}: total actual forged pieces across all shifts = {}", 
             forge.getId(), totalActualForgedPieces);
    
    // Update processedItem fields
    ProcessedItem processedItem = forge.getProcessedItem();
    processedItem.setActualForgePiecesCount(totalActualForgedPieces);
    processedItem.setAvailableForgePiecesCountForHeat(totalActualForgedPieces);
    
    // The processedItem will be saved when the forge is saved due to cascade relationship
  }
}

