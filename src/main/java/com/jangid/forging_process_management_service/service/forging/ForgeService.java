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
}

