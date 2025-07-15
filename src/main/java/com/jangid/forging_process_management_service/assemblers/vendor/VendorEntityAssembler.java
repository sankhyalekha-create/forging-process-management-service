package com.jangid.forging_process_management_service.assemblers.vendor;

import com.jangid.forging_process_management_service.entities.vendor.VendorEntity;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorEntityRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class VendorEntityAssembler {

  public VendorEntity createAssemble(VendorEntityRepresentation representation) {
    VendorEntity vendorEntity = assemble(representation);
    vendorEntity.setCreatedAt(LocalDateTime.now());
    return vendorEntity;
  }

  public VendorEntity assemble(VendorEntityRepresentation representation) {
    return VendorEntity.builder()
        .id(representation.getId())
        .vendorEntityName(representation.getVendorEntityName())
        .address(representation.getAddress())
        .gstinUin(representation.getGstinUin())
        .phoneNumber(representation.getPhoneNumber())
        .panNumber(representation.getPanNumber())
        .isBillingEntity(representation.isBillingEntity())
        .isShippingEntity(representation.isShippingEntity())
        .build();
  }

  public VendorEntityRepresentation dissemble(VendorEntity vendorEntity) {
    return VendorEntityRepresentation.builder()
        .id(vendorEntity.getId())
        .vendorEntityName(vendorEntity.getVendorEntityName())
        .address(vendorEntity.getAddress())
        .gstinUin(vendorEntity.getGstinUin())
        .phoneNumber(vendorEntity.getPhoneNumber())
        .panNumber(vendorEntity.getPanNumber())
        .isBillingEntity(vendorEntity.isBillingEntity())
        .isShippingEntity(vendorEntity.isShippingEntity())
        .vendorId(vendorEntity.getVendor().getId())
        .build();
  }
} 