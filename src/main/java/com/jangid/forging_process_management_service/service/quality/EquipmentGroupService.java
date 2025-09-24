package com.jangid.forging_process_management_service.service.quality;

import com.jangid.forging_process_management_service.assemblers.quality.EquipmentGroupAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.quality.EquipmentGroup;
import com.jangid.forging_process_management_service.entities.quality.EquipmentGroupGauge;
import com.jangid.forging_process_management_service.entities.quality.Gauge;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.EquipmentGroupRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.EquipmentGroupListRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.repositories.TenantRepository;
import com.jangid.forging_process_management_service.repositories.quality.EquipmentGroupRepository;
import com.jangid.forging_process_management_service.repositories.quality.EquipmentGroupGaugeRepository;
import com.jangid.forging_process_management_service.repositories.quality.GaugeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@RequiredArgsConstructor
public class EquipmentGroupService {

  private final EquipmentGroupRepository equipmentGroupRepository;
  private final EquipmentGroupGaugeRepository equipmentGroupGaugeRepository;
  private final GaugeRepository gaugeRepository;
  private final TenantRepository tenantRepository;
  private final EquipmentGroupAssembler equipmentGroupAssembler;

  @Transactional
  public EquipmentGroupRepresentation createEquipmentGroup(Long tenantId, EquipmentGroupRepresentation equipmentGroupRepresentation) {
    log.info("Creating equipment group for tenant: {}", tenantId);

    Tenant tenant = tenantRepository.findByIdAndDeletedFalse(tenantId)
      .orElseThrow(() -> new TenantNotFoundException(tenantId.toString()));

    // Check if equipment group with same name already exists for this tenant
    if (equipmentGroupRepository.existsByGroupNameAndTenantIdAndDeletedFalse(
        equipmentGroupRepresentation.getGroupName(), tenantId)) {
      throw new RuntimeException("Equipment group with name '" + equipmentGroupRepresentation.getGroupName() + "' already exists for this tenant");
    }

    EquipmentGroup equipmentGroup = EquipmentGroup.builder()
      .groupName(equipmentGroupRepresentation.getGroupName())
      .groupDescription(equipmentGroupRepresentation.getGroupDescription())
      .tenant(tenant)
      .deleted(false)
      .build();

    EquipmentGroup savedEquipmentGroup = equipmentGroupRepository.save(equipmentGroup);

    // Add gauges to the group if provided
    if (equipmentGroupRepresentation.getGauges() != null && !equipmentGroupRepresentation.getGauges().isEmpty()) {
      addGaugesToGroup(savedEquipmentGroup.getId(), tenantId, 
        equipmentGroupRepresentation.getGauges().stream()
          .map(gr -> gr.getId())
          .collect(Collectors.toList()));
    }

    log.info("Equipment group created successfully with ID: {}", savedEquipmentGroup.getId());
    return equipmentGroupAssembler.toRepresentation(savedEquipmentGroup);
  }

  @Transactional(readOnly = true)
  public Page<EquipmentGroupRepresentation> getAllEquipmentGroupsOfTenant(Long tenantId, int page, int size) {
    log.info("Fetching equipment groups for tenant: {} with pagination page: {}, size: {}", tenantId, page, size);

    if (!tenantRepository.findByIdAndDeletedFalse(tenantId).isPresent()) {
      throw new TenantNotFoundException(tenantId.toString());
    }

    Pageable pageable = PageRequest.of(page, size);
    Page<EquipmentGroup> equipmentGroupsPage = equipmentGroupRepository.findAllByTenantIdAndDeletedFalse(tenantId, pageable);

    return equipmentGroupsPage.map(equipmentGroupAssembler::toRepresentation);
  }

  @Transactional(readOnly = true)
  public EquipmentGroupListRepresentation getAllEquipmentGroupsOfTenantWithoutPagination(Long tenantId) {
    log.info("Fetching all equipment groups for tenant: {} without pagination", tenantId);

    if (!tenantRepository.findByIdAndDeletedFalse(tenantId).isPresent()) {
      throw new TenantNotFoundException(tenantId.toString());
    }

    List<EquipmentGroup> equipmentGroups = equipmentGroupRepository.findAllByTenantIdAndDeletedFalse(tenantId);
    return equipmentGroupAssembler.toListRepresentation(equipmentGroups);
  }

  @Transactional(readOnly = true)
  public EquipmentGroupRepresentation getEquipmentGroupById(Long equipmentGroupId, Long tenantId) {
    log.info("Fetching equipment group with ID: {} for tenant: {}", equipmentGroupId, tenantId);

    EquipmentGroup equipmentGroup = equipmentGroupRepository.findByIdAndTenantIdAndDeletedFalse(equipmentGroupId, tenantId)
      .orElseThrow(() -> new RuntimeException("Equipment group not found with ID: " + equipmentGroupId));

    return equipmentGroupAssembler.toRepresentation(equipmentGroup);
  }

  @Transactional
  public EquipmentGroupRepresentation updateEquipmentGroup(Long equipmentGroupId, Long tenantId, EquipmentGroupRepresentation equipmentGroupRepresentation) {
    log.info("Updating equipment group with ID: {} for tenant: {}", equipmentGroupId, tenantId);

    EquipmentGroup existingEquipmentGroup = equipmentGroupRepository.findByIdAndTenantIdAndDeletedFalse(equipmentGroupId, tenantId)
      .orElseThrow(() -> new RuntimeException("Equipment group not found with ID: " + equipmentGroupId));

    // Check if another equipment group with same name already exists for this tenant
    if (equipmentGroupRepository.existsByGroupNameAndTenantIdAndIdNotAndDeletedFalse(
        equipmentGroupRepresentation.getGroupName(), tenantId, equipmentGroupId)) {
      throw new RuntimeException("Equipment group with name '" + equipmentGroupRepresentation.getGroupName() + "' already exists for this tenant");
    }

    existingEquipmentGroup.setGroupName(equipmentGroupRepresentation.getGroupName());
    existingEquipmentGroup.setGroupDescription(equipmentGroupRepresentation.getGroupDescription());

    EquipmentGroup updatedEquipmentGroup = equipmentGroupRepository.save(existingEquipmentGroup);

    log.info("Equipment group updated successfully with ID: {}", updatedEquipmentGroup.getId());
    return equipmentGroupAssembler.toRepresentation(updatedEquipmentGroup);
  }

  @Transactional
  public void deleteEquipmentGroup(Long equipmentGroupId, Long tenantId) {
    log.info("Deleting equipment group with ID: {} for tenant: {}", equipmentGroupId, tenantId);

    EquipmentGroup equipmentGroup = equipmentGroupRepository.findByIdAndTenantIdAndDeletedFalse(equipmentGroupId, tenantId)
      .orElseThrow(() -> new RuntimeException("Equipment group not found with ID: " + equipmentGroupId));

    // Soft delete all equipment group gauges first
    equipmentGroupGaugeRepository.softDeleteByEquipmentGroupIdAndTenantId(equipmentGroupId, tenantId);

    // Soft delete the equipment group
    equipmentGroup.setDeleted(true);
    equipmentGroup.setDeletedAt(LocalDateTime.now());
    equipmentGroupRepository.save(equipmentGroup);

    log.info("Equipment group deleted successfully with ID: {}", equipmentGroupId);
  }

  @Transactional
  public EquipmentGroupRepresentation addGaugesToGroup(Long equipmentGroupId, Long tenantId, List<Long> gaugeIds) {
    log.info("Adding gauges to equipment group with ID: {} for tenant: {}", equipmentGroupId, tenantId);

    EquipmentGroup equipmentGroup = equipmentGroupRepository.findByIdAndTenantIdAndDeletedFalse(equipmentGroupId, tenantId)
      .orElseThrow(() -> new RuntimeException("Equipment group not found with ID: " + equipmentGroupId));

    List<Gauge> gauges = gaugeRepository.findAllByIdInAndTenantIdAndDeletedFalse(gaugeIds, tenantId);
    
    if (gauges.size() != gaugeIds.size()) {
      throw new RuntimeException("Some gauges not found or do not belong to the tenant");
    }

    for (Gauge gauge : gauges) {
      // Check if gauge is already in the group (active relationship)
      if (!equipmentGroupGaugeRepository.existsByEquipmentGroupIdAndGaugeIdAndTenantIdAndDeletedFalse(equipmentGroupId, gauge.getId(), tenantId)) {
        // Check if there's a deleted relationship that can be reactivated
        Optional<EquipmentGroupGauge> existingDeletedRelationship = equipmentGroupGaugeRepository
          .findByEquipmentGroupIdAndGaugeIdAndTenantIdAndDeletedTrue(equipmentGroupId, gauge.getId(), tenantId);
        
        if (existingDeletedRelationship.isPresent()) {
          // Reactivate the existing relationship
          EquipmentGroupGauge equipmentGroupGauge = existingDeletedRelationship.get();
          equipmentGroupGauge.setDeleted(false);
          equipmentGroupGauge.setDeletedAt(null);
          equipmentGroupGaugeRepository.save(equipmentGroupGauge);
        } else {
          // Create new relationship
          EquipmentGroupGauge equipmentGroupGauge = EquipmentGroupGauge.builder()
            .equipmentGroup(equipmentGroup)
            .gauge(gauge)
            .tenant(equipmentGroup.getTenant())
            .deleted(false)
            .build();
          equipmentGroupGaugeRepository.save(equipmentGroupGauge);
        }
      }
    }

    log.info("Gauges added successfully to equipment group with ID: {}", equipmentGroupId);
    
    // Refresh the equipment group to get updated associations
    EquipmentGroup refreshedEquipmentGroup = equipmentGroupRepository.findByIdAndTenantIdAndDeletedFalse(equipmentGroupId, tenantId)
      .orElseThrow(() -> new RuntimeException("Equipment group not found with ID: " + equipmentGroupId));
    
    return equipmentGroupAssembler.toRepresentation(refreshedEquipmentGroup);
  }

  @Transactional
  public EquipmentGroupRepresentation removeGaugeFromGroup(Long equipmentGroupId, Long tenantId, Long gaugeId) {
    log.info("Removing gauge {} from equipment group with ID: {} for tenant: {}", gaugeId, equipmentGroupId, tenantId);

    // Validate equipment group exists
    equipmentGroupRepository.findByIdAndTenantIdAndDeletedFalse(equipmentGroupId, tenantId)
      .orElseThrow(() -> new RuntimeException("Equipment group not found with ID: " + equipmentGroupId));

    equipmentGroupGaugeRepository.softDeleteByEquipmentGroupIdAndGaugeIdAndTenantId(equipmentGroupId, gaugeId, tenantId);

    log.info("Gauge removed successfully from equipment group with ID: {}", equipmentGroupId);
    
    // Refresh the equipment group to get updated associations
    EquipmentGroup refreshedEquipmentGroup = equipmentGroupRepository.findByIdAndTenantIdAndDeletedFalse(equipmentGroupId, tenantId)
      .orElseThrow(() -> new RuntimeException("Equipment group not found with ID: " + equipmentGroupId));
    
    return equipmentGroupAssembler.toRepresentation(refreshedEquipmentGroup);
  }
}
