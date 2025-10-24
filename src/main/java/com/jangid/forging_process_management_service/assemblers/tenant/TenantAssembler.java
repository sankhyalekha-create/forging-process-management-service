package com.jangid.forging_process_management_service.assemblers.tenant;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entitiesRepresentation.tenant.TenantRepresentation;
import org.springframework.stereotype.Component;

@Component
public class TenantAssembler {

  public Tenant assemble(TenantRepresentation representation) {
    if (representation == null) {
      return null;
    }

    return Tenant.builder()
      .id(representation.getId())
      .tenantName(representation.getTenantName())
      .tenantOrgId(representation.getTenantOrgId())
      .phoneNumber(representation.getPhoneNumber())
      .gstin(representation.getGstin())
      .email(representation.getEmail())
      .address(representation.getAddress())
      .stateCode(representation.getStateCode())
      .pincode(representation.getPincode())
      .otherDetails(representation.getOtherDetails())
      .isInternal(representation.getIsInternal() != null ? representation.getIsInternal() : false)
      .isActive(representation.getIsActive() != null ? representation.getIsActive() : true)
      .tenantConfigurations(representation.getTenantConfigurations())
      .build();
  }

  public TenantRepresentation dissemble(Tenant tenant) {
    if (tenant == null) {
      return null;
    }

    return TenantRepresentation.builder()
      .id(tenant.getId())
      .tenantName(tenant.getTenantName())
      .tenantOrgId(tenant.getTenantOrgId())
      .phoneNumber(tenant.getPhoneNumber())
      .gstin(tenant.getGstin())
      .email(tenant.getEmail())
      .address(tenant.getAddress())
      .stateCode(tenant.getStateCode())
      .pincode(tenant.getPincode())
      .otherDetails(tenant.getOtherDetails())
      .isInternal(tenant.isInternal())
      .isActive(tenant.isActive())
      .createdAt(tenant.getCreatedAt())
      .updatedAt(tenant.getUpdatedAt())
      .tenantConfigurations(tenant.getTenantConfigurations())
      .build();
  }

  public Tenant updateEntity(Tenant tenant, TenantRepresentation representation) {
    if (representation == null) {
      return tenant;
    }

    if (representation.getTenantName() != null) {
      tenant.setTenantName(representation.getTenantName());
    }
    if (representation.getTenantOrgId() != null) {
      tenant.setTenantOrgId(representation.getTenantOrgId());
    }
    if (representation.getPhoneNumber() != null) {
      tenant.setPhoneNumber(representation.getPhoneNumber());
    }
    if (representation.getGstin() != null) {
      tenant.setGstin(representation.getGstin());
    }
    if (representation.getEmail() != null) {
      tenant.setEmail(representation.getEmail());
    }
    if (representation.getAddress() != null) {
      tenant.setAddress(representation.getAddress());
    }
    if (representation.getStateCode() != null) {
      tenant.setStateCode(representation.getStateCode());
    }
    if (representation.getPincode() != null) {
      tenant.setPincode(representation.getPincode());
    }
    if (representation.getOtherDetails() != null) {
      tenant.setOtherDetails(representation.getOtherDetails());
    }
    if (representation.getIsInternal() != null) {
      tenant.setInternal(representation.getIsInternal());
    }
    if (representation.getIsActive() != null) {
      tenant.setActive(representation.getIsActive());
    }
    if (representation.getTenantConfigurations() != null) {
      tenant.setTenantConfigurations(representation.getTenantConfigurations());
    }

    return tenant;
  }
}

