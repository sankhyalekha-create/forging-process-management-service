package com.jangid.forging_process_management_service.service.inventory;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialAssembler;
import com.jangid.forging_process_management_service.entities.inventory.BarDiameter;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterial;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.inventory.RawMaterialHeatRepository;
import com.jangid.forging_process_management_service.repositories.inventory.RawMaterialRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.utils.ConstantUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RawMaterialService {

  @Autowired
  private RawMaterialRepository rawMaterialRepository;

  @Autowired
  private RawMaterialHeatRepository rawMaterialHeatRepository;
  @Autowired
  private TenantService tenantService;

  @Transactional
  public RawMaterialRepresentation addRawMaterial(Long tenantId, RawMaterialRepresentation rawMaterialRepresentation) {
    Tenant tenant = tenantService.getTenantById(tenantId);
    RawMaterial rawMaterial = RawMaterial.builder()
        .rawMaterialInvoiceNumber(rawMaterialRepresentation.getRawMaterialInvoiceNumber())
        .rawMaterialReceivingDate(LocalDateTime.parse(rawMaterialRepresentation.getRawMaterialReceivingDate(), ConstantUtils.DATE_TIME_FORMATTER))
        .rawMaterialInputCode(rawMaterialRepresentation.getRawMaterialInputCode())
        .rawMaterialTotalQuantity(Float.valueOf(rawMaterialRepresentation.getRawMaterialTotalQuantity()))
        .rawMaterialHsnCode(rawMaterialRepresentation.getRawMaterialHsnCode())
        .rawMaterialGoodsDescription(rawMaterialRepresentation.getRawMaterialGoodsDescription())
        .createdAt(LocalDateTime.now())
        .tenant(tenant).build();

    List<Heat> heats = getHeats(rawMaterial, rawMaterialRepresentation);
    heats.forEach(h -> h.setCreatedAt(LocalDateTime.now()));
    rawMaterial.setHeats(heats);
    RawMaterial savedRawMaterial = rawMaterialRepository.save(rawMaterial);
    return RawMaterialAssembler.dissemble(savedRawMaterial);
  }

  @Transactional
  public RawMaterialRepresentation updateRawMaterial(Long tenantId, Long rawMaterialId, RawMaterialRepresentation rawMaterialRepresentation) {
    Tenant tenant = tenantService.getTenantById(tenantId);
    RawMaterial existingRawMaterial = getRawMaterialByIdAndTenantId(rawMaterialId, tenantId);

    existingRawMaterial.setRawMaterialInvoiceNumber(rawMaterialRepresentation.getRawMaterialInvoiceNumber());
    existingRawMaterial.setRawMaterialReceivingDate(rawMaterialRepresentation.getRawMaterialReceivingDate() != null ? LocalDateTime.parse(rawMaterialRepresentation.getRawMaterialReceivingDate(), ConstantUtils.DATE_TIME_FORMATTER) : null);
    existingRawMaterial.setRawMaterialInputCode(rawMaterialRepresentation.getRawMaterialInputCode());
    existingRawMaterial.setRawMaterialTotalQuantity(Float.valueOf(rawMaterialRepresentation.getRawMaterialTotalQuantity()));
    existingRawMaterial.setRawMaterialHsnCode(rawMaterialRepresentation.getRawMaterialHsnCode());
    existingRawMaterial.setRawMaterialGoodsDescription(rawMaterialRepresentation.getRawMaterialGoodsDescription());
    existingRawMaterial.setTenant(tenant);

    existingRawMaterial.getHeats().clear();
    List<Heat> heats = getHeats(existingRawMaterial, rawMaterialRepresentation);

    existingRawMaterial.getHeats().addAll(heats);
    RawMaterial savedRawMaterial = rawMaterialRepository.save(existingRawMaterial);

    return RawMaterialAssembler.dissemble(savedRawMaterial);
  }

  public Page<RawMaterialRepresentation> getAllRawMaterialsOfTenant(long tenantId, int page, int size){
    Pageable pageable = PageRequest.of(page, size);
    Page<RawMaterial> rawMaterialsPage = rawMaterialRepository.findByTenantIdAndDeletedIsFalseOrderByRawMaterialReceivingDateDesc(tenantId, pageable);
    return rawMaterialsPage.map(RawMaterialAssembler::dissemble);
  }

  public RawMaterial getRawMaterialById(long materialId){
    Optional<RawMaterial> optionalRawMaterial = rawMaterialRepository.findById(materialId);
    if (optionalRawMaterial.isEmpty()){
      log.error("RawMaterial with id="+materialId+" not found!");
      throw new RuntimeException("RawMaterial with id="+materialId+" not found!");
    }
    return optionalRawMaterial.get();
  }

  @Transactional
  public void deleteRawMaterialByIdAndTenantId(Long rawMaterialId, Long tenantId) {
    Optional<RawMaterial> rawMaterialOptional = rawMaterialRepository.findByIdAndTenantIdAndDeletedFalse(rawMaterialId, tenantId);
    if (rawMaterialOptional.isEmpty()) {
      log.error("rawMaterialId with id="+rawMaterialId+" having "+tenantId+" not found!");
      throw new ResourceNotFoundException("rawMaterialId with id=" + rawMaterialId + " having " + tenantId + " not found!");
    }
    RawMaterial rawMaterial = rawMaterialOptional.get();
    rawMaterial.setDeleted(true);
    rawMaterial.setDeletedAt(LocalDateTime.now());
    rawMaterialRepository.save(rawMaterial);
  }

  private RawMaterial getRawMaterialByIdAndTenantId(long materialId, long tenantId){
    Optional<RawMaterial> optionalRawMaterial = rawMaterialRepository.findByIdAndTenantIdAndDeletedFalse(materialId, tenantId);
    if (optionalRawMaterial.isEmpty()){
      log.error("RawMaterial with id="+materialId+" having "+tenantId+" not found!");
      throw new RuntimeException("RawMaterial with id="+materialId+" having "+tenantId+" not found!");
    }
    return optionalRawMaterial.get();
  }

  public RawMaterial getRawMaterialByInvoiceNumber(long tenantId, String invoiceNumber){
    Optional<RawMaterial> optionalRawMaterial = rawMaterialRepository.findByTenantIdAndRawMaterialInvoiceNumberAndDeletedIsFalse(tenantId, invoiceNumber);
    if (optionalRawMaterial.isEmpty()){
      log.error("RawMaterial with invoiceNumber="+invoiceNumber+" for tenant="+tenantId+" not found!");
      return null;
    }
    return optionalRawMaterial.get();
  }

  public List<RawMaterial> getRawMaterialByHeatNumber(long tenantId, String heatNumber){
    List<Heat> heats = rawMaterialHeatRepository.findByHeatNumberAndDeletedIsFalse(heatNumber);
    if (heats == null){
      log.error("rawMaterialHeat with heatNumber= "+heatNumber+" for tenant= "+tenantId+" not found!");
      return Collections.emptyList();
    }
    List<RawMaterial> rawMaterials = new ArrayList<>();
    heats.stream().filter(h -> Objects.equals(tenantId, h.getRawMaterial().getTenant().getId())).forEach(h -> rawMaterials.add(h.getRawMaterial()));
    return rawMaterials.stream().sorted((a, b) -> b.getRawMaterialReceivingDate().compareTo(a.getRawMaterialReceivingDate())).collect(Collectors.toList());
  }

  private List<Heat> getHeats(RawMaterial rawMaterial, RawMaterialRepresentation rawMaterialRepresentation){
    List<Heat> heats = new ArrayList<>();
    for(RawMaterialHeatRepresentation heat : rawMaterialRepresentation.getHeats()){
      heats.add(Heat.builder()
                    .heatNumber(heat.getHeatNumber())
                    .heatQuantity(Float.valueOf(heat.getHeatQuantity()))
                    .availableHeatQuantity(Float.valueOf(heat.getHeatQuantity()))
                    .rawMaterialTestCertificateNumber(heat.getRawMaterialTestCertificateNumber())
                    .barDiameter(heat.getBarDiameter()!=null ? BarDiameter.valueOf(heat.getBarDiameter()): null)
                    .rawMaterialReceivingInspectionReportNumber(heat.getRawMaterialReceivingInspectionReportNumber())
                    .rawMaterialInspectionSource(heat.getRawMaterialInspectionSource())
                    .rawMaterialLocation(heat.getRawMaterialLocation())
                    .rawMaterial(rawMaterial).build());
    }
    return heats;
  }

  public RawMaterialListRepresentation getRawMaterialListRepresentation(List<RawMaterial> rawMaterials) {
    if (rawMaterials == null) {
      log.error("RawMaterial list is null!");
      return RawMaterialListRepresentation.builder().build();
    }
    List<RawMaterialRepresentation> rawMaterialRepresentations = new ArrayList<>();
    rawMaterials.forEach(rm -> rawMaterialRepresentations.add(RawMaterialAssembler.dissemble(rm)));
    return RawMaterialListRepresentation.builder()
        .rawMaterials(rawMaterialRepresentations).build();
  }

  public List<RawMaterial> getRawMaterialByStartAndEndDate(String startDate, String endDate, long tenantId){
    LocalDateTime sDate = LocalDate.parse(startDate, ConstantUtils.DAY_FORMATTER).atStartOfDay();
    endDate = endDate+ConstantUtils.LAST_MINUTE_OF_DAY;
    LocalDateTime eDate = LocalDateTime.parse(endDate, ConstantUtils.DATE_TIME_FORMATTER);

    List<RawMaterial> rawMaterials = rawMaterialRepository.findByTenantIdAndRawMaterialReceivingDateGreaterThanAndRawMaterialReceivingDateLessThanAndDeletedIsFalseOrderByRawMaterialReceivingDateDesc(tenantId, sDate, eDate);
    if (rawMaterials == null){
      log.error("RawMaterials with startDate= "+startDate+" and endDate= "+endDate+" for tenant= "+tenantId+" not found!");
      return Collections.emptyList();
    }
    return rawMaterials;
  }

  public List<Heat> getAvailableRawMaterialByTenantId(long tenantId){
    List<RawMaterial> rawMaterials = rawMaterialRepository.findByTenantIdAndDeletedIsFalseOrderByRawMaterialReceivingDateDesc(tenantId);

    List<Heat> heats = rawMaterials.stream()
        .flatMap(rm -> rm.getHeats().stream())
        .filter(rmh -> rmh.getAvailableHeatQuantity() > 0)
        .collect(Collectors.toList());
    if (heats.isEmpty()) {
      log.info("No records exist for tenant={} with heats having available quantity greater than 0", tenantId);
    }

    return heats;
  }
}
