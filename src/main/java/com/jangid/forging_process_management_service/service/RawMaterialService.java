package com.jangid.forging_process_management_service.service;

import com.jangid.forging_process_management_service.assemblers.RawMaterialAssembler;
import com.jangid.forging_process_management_service.entities.BarDiameter;
import com.jangid.forging_process_management_service.entities.RawMaterial;
import com.jangid.forging_process_management_service.entities.RawMaterialHeat;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entitiesRepresentation.RawMaterialHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.RawMaterialRepresentation;
import com.jangid.forging_process_management_service.exception.RawMaterialNotFoundException;
import com.jangid.forging_process_management_service.repositories.RawMaterialRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RawMaterialService {

  public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  @Autowired
  private RawMaterialRepository rawMaterialRepository;

  @Autowired
  private TenantService tenantService;

  @Transactional
  public RawMaterialRepresentation addRawMaterial(Long tenantId, RawMaterialRepresentation rawMaterialRepresentation) {
    Tenant tenant = tenantService.getTenantById(tenantId);
    RawMaterial rawMaterial = RawMaterial.builder()
        .rawMaterialInvoiceNumber(rawMaterialRepresentation.getRawMaterialInvoiceNumber())
        .rawMaterialReceivingDate(LocalDateTime.parse(rawMaterialRepresentation.getRawMaterialReceivingDate(), formatter))
        .rawMaterialInputCode(rawMaterialRepresentation.getRawMaterialInputCode())
        .rawMaterialTotalQuantity(Float.valueOf(rawMaterialRepresentation.getRawMaterialTotalQuantity()))
        .rawMaterialHsnCode(rawMaterialRepresentation.getRawMaterialHsnCode())
        .rawMaterialGoodsDescription(rawMaterialRepresentation.getRawMaterialGoodsDescription())
        .createdAt(LocalDateTime.now())
        .tenant(tenant).build();

    List<RawMaterialHeat> heats = new ArrayList<>();
    for(RawMaterialHeatRepresentation heat : rawMaterialRepresentation.getHeats()){
      heats.add(RawMaterialHeat.builder()
                    .heatNumber(heat.getHeatNumber())
                    .heatQuantity(Float.valueOf(heat.getHeatQuantity()))
                    .rawMaterialTestCertificateNumber(heat.getRawMaterialTestCertificateNumber())
                    .barDiameter(heat.getBarDiameter()!=null ? BarDiameter.valueOf(heat.getBarDiameter()): null)
                    .rawMaterialReceivingInspectionReportNumber(heat.getRawMaterialReceivingInspectionReportNumber())
                    .rawMaterialInspectionSource(heat.getRawMaterialInspectionSource())
                    .rawMaterialLocation(heat.getRawMaterialLocation()).build());
    }
    rawMaterial.setHeats(heats);
    RawMaterial savedRawMaterial = rawMaterialRepository.save(rawMaterial);
    return RawMaterialAssembler.dissemble(savedRawMaterial);
  }

  public List<RawMaterialRepresentation> getAllRawMaterialsOfTenant(long tenantId){
    List<RawMaterial> rawMaterials =  rawMaterialRepository.findByTenantId(tenantId);
    List<RawMaterialRepresentation> rawMaterialRepresentations = new ArrayList<>();
    rawMaterials.forEach(rm -> rawMaterialRepresentations.add(RawMaterialAssembler.dissemble(rm)));
    return rawMaterialRepresentations;
  }

  public RawMaterial getRawMaterialById(long materialId){
    Optional<RawMaterial> optionalRawMaterial = rawMaterialRepository.findById(materialId);
    if (optionalRawMaterial.isEmpty()){
      log.error("RawMaterial with id="+materialId+" not found!");
      throw new RuntimeException("RawMaterial with id="+materialId+" not found!");
    }
    return optionalRawMaterial.get();
  }

  public RawMaterial getRawMaterialByInvoiceNumber(long tenantId, String invoiceNumber){
    Optional<RawMaterial> optionalRawMaterial = rawMaterialRepository.findByTenantIdAndRawMaterialInvoiceNumberAndDeletedIsFalse(tenantId, invoiceNumber);
    if (optionalRawMaterial.isEmpty()){
      log.error("RawMaterial with invoiceNumber="+invoiceNumber+" for tenant="+tenantId+" not found!");
      throw new RawMaterialNotFoundException("RawMaterial with invoiceNumber=" + invoiceNumber + " for tenant=" + tenantId + " not found!");
    }
    return optionalRawMaterial.get();
  }
}
