package com.jangid.forging_process_management_service.assemblers.vendor;

import com.jangid.forging_process_management_service.entities.vendor.Vendor;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorEntityRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class VendorAssembler {

  private final VendorEntityAssembler vendorEntityAssembler;

  @Autowired
  public VendorAssembler(VendorEntityAssembler vendorEntityAssembler) {
    this.vendorEntityAssembler = vendorEntityAssembler;
  }

  public Vendor createAssemble(VendorRepresentation representation) {
    Vendor vendor = assemble(representation);
    vendor.setCreatedAt(LocalDateTime.now());
    return vendor;
  }

  public Vendor assemble(VendorRepresentation representation) {

    return Vendor.builder()
        .id(representation.getId())
        .vendorName(representation.getVendorName())
        .address(representation.getAddress())
        .gstinUin(representation.getGstinUin())
        .phoneNumber(representation.getPhoneNumber())
        .panNumber(representation.getPanNumber())
        .entities(representation.getEntities() != null ? representation.getEntities().stream()
            .map(vendorEntityAssembler::assemble)
            .collect(Collectors.toList()) : new ArrayList<>())
        .build();
  }

  public VendorRepresentation dissemble(Vendor vendor) {
    List<VendorEntityRepresentation> entityRepresentations = vendor.getEntities().stream()
        .map(vendorEntityAssembler::dissemble)
        .collect(Collectors.toList());

    return VendorRepresentation.builder()
        .id(vendor.getId())
        .vendorName(vendor.getVendorName())
        .address(vendor.getAddress())
        .gstinUin(vendor.getGstinUin())
        .phoneNumber(vendor.getPhoneNumber())
        .panNumber(vendor.getPanNumber())
        .entities(entityRepresentations)
        .build();
  }
} 